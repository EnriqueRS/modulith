# Informe: qué aporta Spring Modulith en `event-tickets-modulith`

> Análisis del uso de Spring Modulith **1.4.13** sobre Spring Boot **3.5.16** (Java 21).
> Proyecto: sistema de reserva de entradas para eventos.

---

## 1. ¿Qué es Spring Modulith?

Spring Modulith es una extensión del ecosistema Spring Boot para construir **aplicaciones monolíticas modularmente estructuradas** (moduliths): la simplicidad de despliegue de un monolito con las fronteras y la disciplina de dependencias de los microservicios.

No es un framework de ejecución pesado: es una capa de **convenciones, verificación y herramientas** sobre Spring Boot. De hecho, sus reglas de verificación se ejecutan con **ArchUnit** por debajo.

## 2. Mapa rápido: bondad → dónde está implementada

| Bondad | Implementación concreta |
|---|---|
| Modularidad por paquetes, sin configuración | Estructura de `src/main/java/com/example/tickets` (4 módulos) |
| Verificación arquitectónica en la build | `src/test/java/com/example/tickets/ModularityTests.java` → `modules.verify()` |
| Contratos de eventos entre módulos | `src/main/java/com/example/tickets/booking/BookingEvents.java` |
| Escucha desacoplada tras el commit | `seats/BookingEventHandler.java` y `notification/BookingNotificationHandler.java` → `@ApplicationModuleListener` |
| Entrega fiable de eventos con reintentos | Dependencias `spring-modulith-events-jpa` + `spring-modulith-events-jackson` y `application.properties` |
| Observabilidad de módulos y eventos | `/actuator/modulith`, métricas `modulith.events.processed` |
| Tests que arrancan un solo módulo | `*ModuleTest.java` con `@ApplicationModuleTest` |
| Verificación de eventos publicados en test | `booking/BookingModuleTest.java` → `@RecordApplicationEvents` |
| Documentación viva (PlantUML + C4) | `ModularityTests` → `Documenter` → `target/spring-modulith-docs/` |
| Camino a microservicios | Fronteras ya verificadas: cada módulo es extraíble |

## 3. Las bondades, una a una

### 3.1 Modularidad por paquetes, sin una línea de configuración

Con Spring Modulith, cada paquete directo bajo la aplicación es un **módulo**, y todo lo que cuelga de él son sus **internos**:

```
src/main/java/com/example/tickets/
├── TicketsApplication.java   # @SpringBootApplication (punto de entrada)
├── events/                   # Módulo: catálogo de eventos (conciertos, obras...)
├── booking/                  # Módulo: reservas. Publica BookingConfirmed/CancelledEvent
├── seats/                    # Módulo: aforo. Escucha los eventos de booking
└── notification/             # Módulo: emails. Escucha los eventos de booking
```

No hay XML, ni anotaciones de arquitectura en el código de negocio, ni config: la estructura del paquete **es** la arquitectura. Los módulos se ven en `ModularityTests` (basta `ApplicationModules.of(TicketsApplication.class)` para que Modulith los deduzca).

En este proyecto hay dos tipos de dependencia entre módulos, y las dos son deliberadas:

- **Síncrona (lectura):** `booking → events` y `seats → events`, siempre a través de `EventService` (la API pública del módulo). Nunca se inyecta un `EventRepository` ajeno.
- **Asíncrona (negocio):** `booking → seats` y `booking → notification`, exclusivamente por eventos. `booking` no sabe quién reacciona a sus eventos.

### 3.2 La arquitectura se verifica en cada build

`src/test/java/com/example/tickets/ModularityTests.java`:

```java
static final ApplicationModules modules = ApplicationModules.of(TicketsApplication.class);

@Test
void verifiesModularStructure() {
    System.out.println(modules.toString()); // estructura detectada
    modules.verify();                       // falla si hay violaciones
}
```

`verify()` (basado en ArchUnit) **rompe la build** si:

- un módulo usa una clase *interna* de otro (por ejemplo, `SeatService` inyectando `EventRepository`),
- aparecen dependencias circulares entre módulos,
- se violan las reglas de exposición de la API de cada módulo.

Esto convierte la arquitectura en algo **ejecutable**: un descuido de dos segundos en el IDE no llega a `main`, lo detecta `mvn test`. En la [sección 6](#6-prueba-de-fuego-viola-una-frontera-y-mira-cómo-la-build-te-lo-impide) hay una prueba reproducible: inyectar `SeatService` en `BookingService` y ver cómo la build detecta el ciclo.

### 3.3 Contratos de eventos: la API pública asíncrona

`booking/BookingEvents.java` define lo único que los consumidores conocen de `booking`:

```java
public record BookingConfirmedEvent(
        Long bookingId, Long eventId, String customerEmail,
        int quantity, BigDecimal totalAmount, Instant occurredAt
) {}
```

Son records inmutables, versionables y autocontenidos. `seats` y `notification` importan `BookingEvents`, jamás `Booking` ni `BookingService`: desacoplamiento real, no teórico.

### 3.4 `@ApplicationModuleListener`: el listener que lo lleva todo hecho

`seats/BookingEventHandler.java`:

```java
@ApplicationModuleListener
public void onBookingConfirmed(BookingConfirmedEvent event) {
    seatService.reserveSeats(event.eventId(), event.quantity());
}
```

Una anotación equivale a la combinación clásica `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` + transacción propia (`REQUIRES_NEW`):

1. **Se ejecuta solo si la transacción de la reserva hizo commit** (nunca se reserva aforo por una reserva que no se persistió).
2. **Es asíncrona**: el API del `BookingController` no espera a seats ni notification.
3. **Corre en su propia transacción**, aislada de la del publicador.

Detalle importante de diseño: los handlers **no capturan excepciones**. Si `SeatService.reserveSeats` lanza `InsufficientSeatsException`, la excepción propagada es precisamente lo que marca la publicación como **incompleta** y activa los reintentos (siguiente bondad).

### 3.5 Entrega fiable de eventos: el Event Publication Registry

Los `ApplicationEvent` de toda la vida se pierden si la aplicación se cae o si el listener falla. Con Modulith, cada publicación se **registra en base de datos**:

- `pom.xml`: `spring-modulith-events-jpa` (registro en la tabla `event_publication`) **+ `spring-modulith-events-jackson`** (serializador JSON de los eventos; sin este artefacto el contexto no arranca: falta el bean `EventSerializer`).
- `application.properties`:

```properties
# Al arrancar, re-publica los eventos incompletos (p. ej. si Seats falló
# porque no había aforo en el intento anterior).
# En Modulith >= 1.4 la propiedad es "-on-restart" (antes "-on-startup")
spring.modulith.events.republish-outstanding-events-on-restart=true

# Conserva el historial de publicaciones completadas (auditables en H2)
spring.modulith.events.completion-mode=UPDATE
```

El ciclo resultante: la publicación se escribe **en la misma transacción** que la reserva; tras el commit los handlers la procesan; si terminan bien, queda *completada* (`completion-mode=UPDATE` la conserva con su `completion_date`); si fallan, queda *incompleta* en la tabla y **se reintenta al arrancar la app**. El flujo `booking → seats → notification` pasa a tener garantía *at-least-once*.

Además, con `spring-modulith-events-api` (ya en el classpath vía `events-jpa`) la aplicación dispone de beans de gestión: `IncompleteEventPublications` para reenviar las pendientes (por antigüedad o predicado) y `CompletedEventPublications` para purgar el histórico.

### 3.6 Observabilidad: la arquitectura visible en runtime

Con `spring-modulith-actuator` + `spring-modulith-starter-insight`:

- **`GET /actuator/modulith`**: el modelo de módulos con sus dependencias y su tipo (`ENTITY`, `USES_COMPONENT`, `EVENT_LISTENER`) — la arquitectura que `verify()` protege, consultable en runtime.
- **Métricas Micrometer**: `modulith.events.processed` (eventos procesados, con tags `event_type` y `module_name`) y un contador por tipo de evento (`BookingConfirmedEvent`, ...), consultables en `/actuator/metrics`. También `module.requests` instrumenta las invocaciones entre módulos.
- **Trazas**: el starter insight instrumenta las invocaciones entre módulos para Micrometer Tracing (Zipkin/OTLP añadiendo el bridge correspondiente).

> Nota de versiones: en Modulith 1.3 existía el endpoint `/actuator/eventpublications`; en 1.4+ fue eliminado y la gestión del registro pasó a las APIs `IncompleteEventPublications`/`CompletedEventPublications` y a la propia tabla `event_publication` (visible en la consola H2). Los nombres de métricas también cambiaron entre versiones.

En una demo se puede provocar un fallo (agotar el aforo), ver la publicación incompleta en la tabla `event_publication` desde la consola H2, liberar aforo y reiniciar para ver el reintento: la entrega fiable de eventos **se ve**, no se cuenta.

### 3.7 Tests de módulo aislados

`@ApplicationModuleTest` (modo `STANDALONE` por defecto) arranca **solo** el módulo bajo prueba:

| Test | Qué demuestra |
|---|---|
| `EventsModuleTest` | `events` no necesita a nadie. |
| `BookingModuleTest` | `booking` solo necesita su dependencia declarada (`events`, mockeada con `@MockitoBean`). |
| `SeatsModuleTest` | `seats` arranca sin `booking`: solo conoce `BookingEvents`. |
| `NotificationModuleTest` | `notification` es un consumidor puro: cero mocks. |

`BookingModuleTest` añade además `@RecordApplicationEvents` (utilidad de test de Spring, integrada con Modulith):

```java
@ApplicationModuleTest
@RecordApplicationEvents
class BookingModuleTest {

    @Test
    void shouldCreateBookingAndPublishConfirmedEvent() {
        ...
        assertThat(events.stream(BookingEvents.BookingConfirmedEvent.class).count()).isEqualTo(1);
    }
}
```

El contrato asíncrono del módulo se verifica **sin levantar los módulos consumidores**: tests rápidos, y además documentación de dependencias vivas (si `booking` empezara a necesitar `seats`, el test STANDALONE lo delataría).

El test de integración `integration/BookingFlowIntegrationTest.java` completa el cuadro con el flujo entero: crea evento → reserva → espera (Awaitility) a que el handler asíncrono haya reservado el aforo → comprueba que **no queda ninguna publicación incompleta** en `event_publication`.

### 3.8 Documentación viva: diagramas generados desde el código

`ModularityTests.writesArchitectureDocumentation()` ejecuta:

```java
new Documenter(modules)
        .writeModulesAsPlantUml()           // diagrama global
        .writeIndividualModulesAsPlantUml() // un diagrama por módulo
        .writeAggregatingDocument();        // documento C4 agregado
```

Salida en `target/spring-modulith-docs/`: `components.puml`, `module-booking.puml`, `module-events.puml`, `module-seats.puml`, `module-notification.puml` y `all-docs.adoc` (modelado C4).

Como se genera desde el código **en cada build de tests**, la documentación no puede mentir: si un módulo empieza a depender de otro, el diagrama cambia (y si la dependencia es ilegal, ni siquiera se genera — la build falla antes).

### 3.9 Camino a microservicios, sin pagar el peaje por adelantado

Cada módulo ya tiene lo que necesitaría para vivir solo: su propio dominio, su propia persistencia, su API pública (síncrona en `EventService`, asíncrona en `BookingEvents`) y tests que prueban su aislamiento. Extraer `notification` como servicio sería: sustituir el `ApplicationModuleListener` por un broker (Kafka/RabbitMQ — Modulith soporta externalización de eventos) y nada más. El monolito modular es la mejor *trampolín* a microservicios: empiezas con despliegue simple, y decides la partición distribuida cuando tienes datos, no especulación.

## 4. El flujo completo

```
        (síncrono: valida evento y precio)      (síncrono: aforo inicial)
Booking ─────────────────────────────▶ Event ◀─────────────────────── Seats
  │
  ├─ BookingConfirmedEvent / BookingCancelledEvent
  │    (publicado DENTRO de la transacción; entregado AFTER_COMMIT, asíncrono,
  │     registrado en event_publication con reintentos)
  │
  │       ├────────▶ Seats         reserva / libera aforo
  │       └────────▶ Notification  email de confirmación / cancelación
```

### 4.1 Anatomía de `POST /api/bookings`, milisegundo a milisegundo

El flujo tiene **dos fases** separadas por el commit, y entender esa frontera es entender todo el diseño:

**Fase síncrona — dentro de la petición HTTP** (`BookingController.create` → `BookingService.createBooking`, una única transacción):

1. `BookingService` consulta `EventService.getEventById(...)` → llamada síncrona `booking → events`: necesita el precio **antes** de poder calcular el total y persistir.
2. `BookingRepository.save(booking)` — la reserva se persiste con estado `CONFIRMED`.
3. `eventPublisher.publishEvent(BookingConfirmedEvent)` — aquí ocurre la magia del Event Publication Registry: en esa **misma transacción** se insertan **dos filas en `event_publication`** (una por cada listener registrado: `seats` y `notification`), con el evento serializado a JSON.
4. **COMMIT** — Spring devuelve `HTTP 201` al cliente. En la demo real: **91 ms**. El cliente no sabe que Seats y Notification existen.

**Fase asíncrona — después del commit** (hilos independientes, cada una con su transacción `REQUIRES_NEW`):

5. `seats.BookingEventHandler.onBookingConfirmed` → `SeatService.reserveSeats(eventId, quantity)`: inicializa el aforo si es la primera vez (leyendo la capacidad del evento vía `events`) y aumenta las plazas reservadas.
6. `notification.BookingNotificationHandler.onBookingConfirmed` → compone el email con el detalle y lo emite por consola.
7. Cada handler, al terminar **sin excepción**, marca **su** publicación como completada (`completion_date`, con `completion-mode=UPDATE` queda en la tabla para auditoría).

El orden 5-6 no está garantizado (corren en paralelo): eso es exactamente lo que muestra el test de integración, que usa Awaitility en lugar de suponer orden.

### 4.2 Por qué cada flecha es como es

| Flecha | Tipo | Razón de diseño |
|---|---|---|
| `booking → events` | Síncrona | Booking necesita el precio **ahora** para persistir el total correcto. Sin precio no hay reserva. |
| `seats → events` | Síncrona (solo lectura) | Seats necesita la capacidad total para inicializar su inventario perezosamente. |
| `booking → seats` | Asíncrona (evento) | Reservar aforo es una consecuencia del negocio, no un prerrequisito. Si el aforo se agota, la publicación queda pendiente — el pedido del cliente **no falla** por ello. |
| `booking → notification` | Asíncrona (evento) | El email no puede romper la reserva ni retrasar la respuesta. |

La regla de oro: **síncrono cuando necesitas el resultado para continuar; asíncrono cuando el negocio puede seguir sin esperar**. La verificación de arquitectura (sección 6) castiga cruzar esa línea: un ciclo tipo `booking ↔ seats` rompe la build.

### 4.3 El camino triste (que es el interesante)

- **Falla Seats** (p. ej. `InsufficientSeatsException` porque el evento está agotado): su publicación queda **incompleta** en `event_publication` (`completion_date IS NULL`). Notification, que escucha el mismo evento, sí procesa el suyo: el fallo de un consumidor **no bloquea a los demás**. Al reiniciar la app, `republish-outstanding-events-on-restart=true` re-entrega el evento; si mientras tanto se liberó aforo, la reserva se completa.
- **Cae la app entre el commit y los listeners**: las filas de `event_publication` ya están commitadas junto a la reserva — el evento **no se pierde**, se procesa al arrancar. Con `ApplicationEventPublisher` a secas, la reserva existiría sin aforo ni email, silenciosamente.
- **El cliente cancela**: `cancelBooking` persiste la cancelación y publica `BookingCancelledEvent` → Seats libera plazas, Notification envía el email de cancelación. Compensación por eventos — el patrón saga en miniatura.

En los tres casos, la tabla `event_publication` es la fuente de verdad: cualquier cosa que esté a medias se ve ahí (`completion_date IS NULL`), y cualquier cosa terminada queda registrada.

## 5. Compruébalo tú mismo

```bash
# Suite completa (22 tests): verificación arquitectónica incluida
./mvnw test

# Solo la verificación + documentación de arquitectura
./mvnw test -Dtest=ModularityTests
ls target/spring-modulith-docs/

# Arrancar la app
./mvnw spring-boot:run
```

Con la app arrancada:

```bash
# Arquitectura en runtime
curl http://localhost:8080/actuator/modulith

# Métricas de eventos procesados (tags: event_type, module_name)
curl http://localhost:8080/actuator/metrics/modulith.events.processed

# Crear evento y reservar
curl -X POST http://localhost:8080/api/events -H "Content-Type: application/json" \
  -d '{"name":"Concierto Demo","venue":"Sala Apolo","eventDate":"2026-12-01T20:00:00Z","price":25.00,"totalCapacity":50}'
curl -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" \
  -d '{"eventId":1,"customerEmail":"demo@example.com","quantity":2}'

# Ver el registro de publicaciones (tabla event_publication, vía consola H2):
# http://localhost:8080/h2-console (JDBC URL jdbc:h2:mem:ticketsdb, usuario sa)
#   SELECT event_type, completion_date FROM event_publication;

# Agotar aforo reservando de más y volver a mirar la tabla:
# verás la publicación con completion_date NULL, pendiente de reintento
```

## 6. Prueba de fuego: viola una frontera y mira cómo la build te lo impide

La demostración cabe en **un solo fichero**. El escenario: alguien de `booking` quiere comprobar el aforo directamente y llama a `SeatService`. Parece razonable... pero crea una dependencia **síncrona** `booking → seats` cuando `seats` ya consume los eventos de `booking`: un **ciclo** entre módulos.

### Paso 1 — Inyectar `SeatService` en `BookingService`

En `src/main/java/com/example/tickets/booking/BookingService.java`, añade el import, el campo traidor y una llamada (con Lombok `@RequiredArgsConstructor`, basta declarar el campo):

```java
import com.example.tickets.seats.SeatService;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
    private final SeatService seatService; // ⚠️ VIOLACIÓN: ciclo booking ↔ seats
    private final ApplicationEventPublisher eventPublisher;

    public Booking createBooking(CreateBookingRequest request) {
        Event event = eventService.getEventById(request.eventId());
        seatService.getAvailability(event.getId()); // ⚠️ trabajo de Seats hecho desde Booking
        // ...
```

### Paso 2 — La build lo impide

```bash
./mvnw test -Dtest=ModularityTests
```

`modules.verify()` **falla** con este mensaje (salida real):

```text
org.springframework.modulith.core.Violations:
- Cycle detected: Slice booking ->
                Slice seats ->
                Slice booking
  1. Dependencies of Slice booking
    - Constructor <...BookingService.<init>(..., SeatService, ...)> has parameter of type
      <...seats.SeatService> in (BookingService.java:0)
    - Field <...BookingService.seatService> has type <...seats.SeatService> in (BookingService.java:0)
    - Method <...BookingService.createBooking(...)> calls method
      <...seats.SeatService.getAvailability(java.lang.Long)> in (BookingService.java:45)
  2. Dependencies of Slice seats
    - Method <...BookingEventHandler.onBookingConfirmed(...)> has parameter of type
      <...booking.BookingEvents$BookingConfirmedEvent> in (BookingEventHandler.java:0)
    ...
```

El diagnóstico señala el ciclo completo con **ambas direcciones**: la dependencia nueva que acabas de añadir (`booking → seats`, con clase, método y línea exactos) y la que ya existía al revés (`seats → booking`, por los eventos que consume). `BookingModuleTest` tampoco arranca: `@ApplicationModuleTest` ejecuta la misma verificación al inicializar el slice.

### Paso 3 — Revertir

Elimina de `BookingService` el import de `SeatService`, el campo y la llamada a `getAvailability`:

```bash
# quita estas tres líneas de BookingService.java:
#   import com.example.tickets.seats.SeatService;
#   private final SeatService seatService; ...
#   seatService.getAvailability(event.getId()); ...
./mvnw test
```

Suite completa de nuevo en verde (22/22).

### Lo que acaba de demostrar

| Mecanismo | Qué comprueba |
|---|---|
| `modules.verify()` (ArchUnit) | En cada `mvnw test`: sin ciclos entre módulos, con diagnóstico quirúrgico (clase, método, línea). |
| `@ApplicationModuleTest` | La misma verificación en el arranque de cada test de módulo. |
| La regla de oro | `booking` se comunica con `seats` **por eventos**; hacerlo síncrono no es "un detalle", es romper el diseño — y la build lo sabe. |

> Variante más estricta: si además mueves una clase a un subpaquete `internal/` del módulo (p. ej. `events/internal/`) y la inyectas desde otro módulo, `verify()` falla con `non-exposed type ... within module ...` — la regla de API pública vs. internos. La convención: paquete raíz del módulo = API pública; subpaquetes = internos.

## 7. Conclusión

El proyecto demuestra el caso de uso canónico de Spring Modulith: **un monolito con fronteras genuinas y verificadas**. La arquitectura vive en tres sitios sincronizados por la herramienta: en los paquetes (estructura), en la build (`verify()` la hace cumplir) y en la documentación (`Documenter` la dibuja). La comunicación por eventos tiene entrega fiable con reintentos y observabilidad de serie, y los tests de módulo confirman el desacoplamiento en cada ejecución. Todo con despliegue de un solo artefacto JAR.
