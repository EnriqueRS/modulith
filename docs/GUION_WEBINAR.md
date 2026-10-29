# 🎤 Guion — Webinar "Introducción a Spring Modulith"

**Duración objetivo: 30-35 min** (+ Q&A) · **Proyecto**: `event-tickets-modulith` · **Deck**: 13 diapositivas

> Guion de apoyo para la presentación. Cada bloque indica objetivo, puntos a comentar y transición. La sección final recoge los puntos que no se pueden olvidar y las preguntas probables.

---

## 0️⃣ Preparación (antes de empezar) — *checklist*

- [ ] `./mvnw spring-boot:run` arrancado y probado (app en `:8080`)
- [ ] Swagger UI abierta: `http://localhost:8080/swagger-ui.html`
- [ ] Consola H2 abierta: `http://localhost:8080/h2-console` (JDBC `jdbc:h2:mem:ticketsdb`, usuario `sa`) con la query lista:

  ```sql
  SELECT event_type, completion_date FROM event_publication;
  ```

- [ ] Terminal visible con los logs de la app (etiquetas `[booking]`, `[seats]`, `[notification]` a la vista)
- [ ] Pestañas preparadas: `/actuator/modulith` y `/actuator/metrics/modulith.events.processed`
- [ ] JSONs de ejemplo copiados al portapapeles (no escribir en directo):
  - **Evento**: `{"name":"Concierto Demo","venue":"Sala Apolo","eventDate":"2026-12-01T20:00:00Z","price":25.00,"totalCapacity":50}`
  - **Reserva**: `{"eventId":1,"customerEmail":"demo@example.com","quantity":2}`
  - **Cancelar**: `POST /api/bookings/1/cancel?reason=Cliente no puede asistir`
- [ ] Suite verde por la mañana: `./mvnw test` → 22/22
- [ ] **Demo de arquitectura preparada**: en `BookingService` las 2 líneas de la violación (`SeatService`) deben estar **comentadas** y verificadas con `./mvnw test` en verde — en directo solo hay que **descomentarlas** (ver paso 💥 de la demo)
- [ ] Extras preparados: Swagger UI con los **grupos por módulo** visibles (`01-events`, `02-booking`, `03-seats`) y `target/spring-modulith-docs/` regenerado con un visor PlantUML a mano (plugin del IDE o plantuml.com)
- [ ] (Copia de seguridad del deck y del repo por si hay que volver atrás)

---

## Bloque 1 · Apertura — *2 min*

### 🎞️ Diapositiva 1 — Portada

**Objetivo**: poner el tema y el hilo conductor.

- Hoy quiero hablar sobre la propuesta que nos hace Spring sobre la arquitectura de los monolitos. Y como con Spring Modulith nos podemos beneficiar de las bondades de los monolitos tradicionales, pero sin sufrir las carencias que historicamente hemos tenido en este tipo de arquitectura.

### 🎞️ Diapositiva 2 — Índice (3 bloques)

**Objetivo**: contrato de la sesión.

- **<1> ¿Por qué modularidad?** — los dolores que vivimos a diario.
- **<2> Arquitectura del ejemplo** — los 4 módulos y cómo se comunican.
- **<3> Demo en vivo** — todo el flujo de una reserva, en directo.

Esto es lo que vamos a ver a grandes rasgos, primero el comentar los pros y contras que tiene el monolito y lo que aporta Spring Modulith.
Luego voy a explicar un pequeño proyecto a modo de demo, realizado con Spring Modulith.
Y por último, lo vamos a ver en vivo, señalando las partes claves que antes habremos visto

---

### 🎞️ Diapositiva 4 — El problema del monolito clásico

**Objetivo**: que la audiencia reconozca SUS dolores.

Los 4 dolores (uno por tarjeta), en este orden y con su frase-gancho:

1. **ACOPLAMIENTO** — Y sobretodo esto se acentúa cuando se trabaja en el mismo monolito varias personas y no digamos, varios equipos...

2. **TESTS LENTOS** — Cuando todo está tan acoplado, te suele obligar a arrancar todo el contexto de la aplicación para probar un módulo en concreto o los test se hacen más complejos al tener que mockear partes del monolito

3. **MIEDO A REFACTORIZAR** — Esto es un clásico, hasta subir de versión de java es un dolor que muchas veces es definitivo para que el proyecto muera.

4. **ONBOARDING DIFÍCIL** — Cuando alguien nuevo se incorpora al equipo, es muy complejo poder ver algo claro entre tantas dependencias cruzadas.

### 🎞️ Diapositiva 5 — Tres formas de estructurar un sistema

**Objetivo**: situar el punto medio.

- **<1> Monolito clásico**: rápido de empezar, cualquier clase llama a cualquier clase.
- **<2> Monolito modular** *(la destacada, fondo verde)*: "la simplicidad del monolito con la disciplina de los microservicios".
- **<3> Microservicios**: máxima autonomía, pero pagas latencia, consistencia eventual y operativa.
- Cierre con el banner: **"Spring Modulith vive en el punto medio: la evolución natural cuando el monolito se queda pequeño pero los microservicios son demasiado."**
- 💡 Si hay tiempo: "y cada módulo bien hecho es un **candidato natural a extraerse** como microservicio — pagas el peaje solo cuando toca".

➡️ *Transición*: "¿Y qué es exactamente Spring Modulith?"

### 🎞️ Diapositiva 6 — ¿Qué es Spring Modulith?

Entrando ya un poco más en concreto qué es Spring Modulith.

**Objetivo**: los 4 pilares (izquierda: definición; derecha: capacidades).

Definición en 2 frases (columna izquierda):

- "Un proyecto del ecosistema Spring que impone **estructura modular a Spring Boot mediante convenciones y validación automática**."
- "**Cada paquete directo** bajo el paquete de la aplicación es un módulo; lo que cuelga de subpaquetes es **interno**. Verifica en tests que solo se usen las APIs públicas."
- "Comunicación: llamada directa (API pública) o **eventos de dominio desacoplados**."

Los 4 pilares (derecha), con una frase cada uno:

1. **LÍMITES VALIDADOS** — ArchUnit es el equivalente a JUnit pero para la arquitectura.
2. **EVENTOS ENTRE MÓDULOS** — `@ApplicationModuleListener`: asíncrono, transaccional, **persistido con reintentos** si un consumidor falla.
3. **TESTING AISLADO** — `@ApplicationModuleTest` arranca **solo el módulo** bajo prueba.
4. **DOCUMENTACIÓN VIVA** — Produce de forma automática una documentación viva, `Documenter` (es una clase que luego veremos en el ejemplo) genera PlantUML y C4 **desde el código real**: *"el diagrama nunca queda obsoleto porque se dibuja en cada build"*. Son diagramas básicos, pero tienen el valor de que siempre están actualizados, están ligados al código real.

⚠️ **No olvidar**: anticipar la demo — "todo esto lo vamos a **ver**, no solo contar".

➡️ *Transición*: "Vamos a la arquitectura del ejemplo."

### 🎞️ Diapositiva 7 — Spring Modulith, de cerca

- Es un proyecto joven, de hecho la release inicial es del 2.023

Los 4 datos (derecha), con su frase:

- **<1> AGOSTO 2023 · 1.0** — "Primera versión estable, sobre Spring Boot 3. El repositorio público llevaba vivo desde julio de 2022."
- **<2> CADENCIA SEMESTRAL** — "Sale una versión cada seis meses, en sintonía con Spring Boot: con Boot 3.5 te corresponde Modulith 1.4; con Boot 4, Modulith 2." ⚠️ **Conectar**: "este proyecto usa Boot 3.5 + Modulith 1.4 — el par correcto, no una versión vieja."
- **<3> OPEN SOURCE** — "Apache 2.0 en spring-projects/spring-modulith, liderado por Oliver Drotbohm (equipo de Spring). Más de 1.200 estrellas en GitHub."

⚠️ **No olvidar**: es un proyecto joven pero no un juguete — avalado y mantenido por el equipo de Spring.

➡️ *Transición*: "Vamos a la arquitectura del ejemplo."

---

## Bloque 3 · Arquitectura del ejemplo — *7 min*

### 🎞️ Diapositiva 8 — Divider <2>

"Arquitectura del ejemplo: un sistema de reserva de entradas para eventos. Pequeño a propósito: comunicación real entre módulos, sin ruido."

### 🎞️ Diapositiva 9 — Los 4 módulos del proyecto

**Objetivo**: presentar el mapa mental que se usará el resto del webinar.

Presentar cada tarjeta con su frase:

- **EVENTS** — "La fuente de verdad del dominio de evento: altas, precios, aforo. API REST clásica. **No sabe nada** de reservas."
- **BOOKING** — "Orquesta: crea y cancela reservas. Llama a Events **síncronamente** cuando necesita el precio, y **publica eventos** al confirmar o cancelar."
- **SEATS** — "Gestiona el aforo. No llama a nadie: **escucha**. BookingConfirmed reserva plazas, BookingCancelled las libera."
- **NOTIFICATION** — "Emails simulados en logs. **Solo consume eventos**: podríamos borrar este módulo entero y ningún otro cambiaría ni una línea. Ese es el nivel de desacoplamiento que buscamos." ⚠️ **Frase estrella — no olvidarla.**


### 🎞️ Diapositiva 10 — ¿Dónde acaba un módulo?

señalar que **no hay config** — "la estructura de paquetes ES la arquitectura; Modulith la detecta sola".

**Árbol real del proyecto** (la slide lo muestra simplificado; tenlo a mano por si preguntan o quieres enseñarlo ampliado):

```
com.example.tickets/
├── TicketsApplication.java
├── OpenApiGroupsConfig.java        # config (en la raíz: no forma módulo)
├── events/                         # módulo
│   ├── EventService · EventInfo · EventController · CreateEventRequest   # API
│   └── internal/                   # Event, EventRepository
├── booking/                        # módulo
│   ├── BookingService · BookingInfo · BookingController · CreateBookingRequest
│   ├── BookingEvents.java          # contrato de eventos (público)
│   └── internal/                   # Booking, BookingRepository, BookingStatus
├── seats/                          # módulo
│   ├── SeatService · SeatAvailability · SeatController                   # API
│   └── internal/                   # SeatInventory, SeatInventoryRepository, BookingEventHandler
└── notification/                   # módulo sin API pública
    └── internal/                   # NotificationService, BookingNotificationHandler
```

- "**Cada carpeta directa** bajo `com.example.tickets` es un módulo. No hay configuración: Modulith los detecta al arrancar los tests."
- "La frontera tiene dos lados: lo que está en la **raíz** de la carpeta del módulo es su **API pública**; lo que metes en **subcarpetas** es **interno** y ningún otro módulo puede tocarlo."
- **En el código**: "cada módulo ya tiene su carpeta `internal/`: entidades, repositorios y handlers. La raíz solo expone servicios, DTOs y controladores "
- **Si quieres declararlo explícito**: `@ApplicationModule(allowedDependencies)` limita de qué módulos puedes depender; `@NamedInterface` expone un subpaquete con nombre. Por defecto el módulo es **cerrado** (`closed` en `/actuator/modulith`).


---

### 🎞️ Diapositiva 11 — Flujo de una reserva

**Objetivo**: explicar las 2 fases y la regla síncrono/asíncrono ANTES de la demo.

- **Fase 1 (síncrona)**: "El usuario hace POST a `/api/bookings`. Booking consulta a Events para el precio — llamada síncrona, y **es correcta que lo sea**: necesita el precio antes de persistir. Persiste la reserva y publica `BookingConfirmedEvent`."
- **La frase clave**: "La petición HTTP termina **en milisegundos**; el resto ocurre después, de forma desacoplada."
- **<1> SEATS RESERVA** — "aumenta las plazas reservadas. Si falla, la transacción del evento lo registra **para reintento**."
- **<2> NOTIFICATION ENVÍA** — "compone el email con el detalle de la reserva."
- **<3> SÍNCRONO VS ASÍNCRONO** — la regla: *"síncrono cuando necesitas el resultado para continuar; asíncrono cuando el negocio puede seguir sin esperar."*
- **<4> COMPENSACIÓN** — "si el cliente cancela, `BookingCancelledEvent` dispara la liberación del aforo y el email: **el patrón saga en acción**."

⚠️ **No olvidar**: "dos módulos reaccionan al mismo evento **sin conocerse entre sí**".

➡️ *Transición*: "Basta de slides. Vamos a verlo con la app arrancada."

---

## Bloque 4 · Demo en vivo — *10 min*

### 🎞️ Diapositiva 12 — Divider <3> Demo en vivo

"Momento de la verdad." (cambia a terminal/Swagger)

### Documentación automática
Arrancada la app, la API se consulta desde Swagger UI: **`http://localhost:8080/swagger-ui.html`**.

El desplegable superior agrupa los endpoints **por módulo** (configurado en `OpenApiGroupsConfig` con un `GroupedOpenApi` por paquete, con `pathsToMatch`): la documentación REST es un espejo de la estructura de carpetas.

```bash
./mvnw spring-boot:run 
```

### Diagramas generados desde el código

`ModularityTests.writesArchitectureDocumentation()` ejecuta:

```java
new Documenter(modules)
        .writeModulesAsPlantUml()           // diagrama global
        .writeIndividualModulesAsPlantUml() // un diagrama por módulo
        .writeAggregatingDocument();        // documento C4 agregado
```

Salida en `target/spring-modulith-docs/`: `components.puml`, `module-booking.puml`, `module-events.puml`, `module-seats.puml`, `module-notification.puml` y `all-docs.adoc` (modelado C4).

Como se genera desde el código **en cada build de tests**, la documentación no puede mentir: si un módulo empieza a depender de otro, el diagrama cambia (y si la dependencia es ilegal, ni siquiera se genera — la build falla antes).

### El recorrido completo (verificado):

```bash
# 1. Evento con aforo 50
curl -X POST http://localhost:8080/api/events -H "Content-Type: application/json" \
  -d '{"name":"Demo Evento","venue":"Sala X","eventDate":"2026-12-01T20:00:00Z","price":10.00,"totalCapacity":50}'

# 2. Reserva A: las 50 entradas -> aforo lleno (reservedSeats: 50, available: 0)
curl -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" \
  -d '{"eventId":1,"customerEmail":"a@example.com","quantity":50}'

# 3. Reserva B: 10 entradas -> devuelve CONFIRMED al cliente, pero el listener
#    de Seats falla al procesarla (aforo agotado):
#    InsufficientSeatsException: Aforo insuficiente para el evento 1:
#    solicitadas 10, disponibles 0
#    -> su publicación queda INCOMPLETA en event_publication (completion_date NULL).
#    La de Notification SÍ se completa: el fallo de un consumidor no arrastra a los demás.
curl -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" \
  -d '{"eventId":1,"customerEmail":"b@example.com","quantity":10}'

# 4. Liberar aforo: cancelar la reserva A (Seats libera las 50 plazas)
curl -X POST "http://localhost:8080/api/bookings/1/cancel?reason=Liberar%20aforo"

```

### Provocar fallo en arquitectura, violar límites. Inyectar el interno en `BookingService`

En `src/main/java/com/example/tickets/booking/BookingService.java`, añade el import y el campo (con Lombok `@RequiredArgsConstructor`, basta declararlo: entra en el constructor):

```java
import com.example.tickets.events.internal.EventRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventService eventService;
    
    private final EventRepository eventRepository;
    // ⚠️ VIOLACIÓN: interno de events

    private final ApplicationEventPublisher eventPublisher;
```

```bash
./mvnw test -Dtest=ModularityTests
```

`modules.verify()` **falla** con este mensaje (salida real):

```text
org.springframework.modulith.core.Violations:
- Module 'booking' depends on non-exposed type
  com.example.tickets.events.internal.EventRepository within module 'events'!
  BookingService declares constructor
  BookingService(BookingRepository, EventService, EventRepository, ApplicationEventPublisher)
  in (BookingService.java:0)
- Module 'booking' depends on non-exposed type
  com.example.tickets.events.internal.EventRepository within module 'events'!
  Field <com.example.tickets.booking.BookingService.eventRepository> has type
  <com.example.tickets.events.internal.EventRepository> in (BookingService.java:0)
```

### 🎞️ Diapositiva 14 — Logs en vivo *(recapitulación de la terminal)*

Usar la slide como **mapa de lo que acaba de pasar** en la terminal:

- "Primero la petición: Booking crea la **reserva #1**, 4 entradas, total 160, `CONFIRMED`."
- "Se publica el evento, y fijaos en las **etiquetas entre corchetes**: cada módulo firma sus logs — podemos seguir la traza de una reserva leyendo solo un color."
- "Seats reserva 4 plazas: el aforo pasa de 80 a **76 disponibles**. Notification envía el email."
- "Y por último el `HTTP 201 Created` en 91 ms. **El cliente no esperó ni al aforo ni al email. Esa asimetría temporal es exactamente el desacoplamiento.**"

⚠️ **No olvidar**: cerrar el círculo con el Event Publication Registry — "si cualquier listener falla, no se pierde nada: queda registrado y se reintenta".

### 💥 Bonus de la demo — Romper la arquitectura en directo *(2 min, el golpe de efecto)*

**Objetivo**: que la audiencia VEA que la arquitectura se ejecuta — no es un diagrama, es la build.

**Narración**: "Hemos dicho que `booking` se comunica con `seats` por eventos. ¿Qué pasaría si alguien, con la mejor intención, decidiera llamar directamente? *Comento dos líneas y listo*…"

**Acción** (en `src/main/java/com/example/tickets/booking/BookingService.java`):

1. **Descomentar** las 2 líneas preparadas:

```java
private final SeatService seatService; // ⚠️ VIOLACIÓN: ciclo booking ↔ seats
...
seatService.getAvailability(event.getId()); // ⚠️ trabajo de Seats hecho desde Booking
```

   *(el import de `SeatService` debe quedar también descomentado)*

2. Ejecutar:

```bash
./mvnw test -Dtest=ModularityTests
```

3. **Señalar en pantalla** el fallo (aparece en ~2 segundos):

```text
org.springframework.modulith.core.Violations:
- Cycle detected: Slice booking -> Slice seats -> Slice booking
  Method <...BookingService.createBooking(...)> calls method
  <...seats.SeatService.getAvailability(java.lang.Long)> in (BookingService.java:45)
```

**Mensajes a decir mientras se lee el error**:

- "No es un warning: **la build falla**. Este descuido no llega nunca a producción."
- "Fijaos en la precisión: te dice el **ciclo completo** — booking → seats → booking — y la **clase, el método y la línea exacta** de la violación."
- "Y no es solo este test: cualquier `@ApplicationModuleTest` ejecuta la misma verificación al arrancar."

**Reversión** (cerrar el círculo):

4. **Volver a comentar** las 2 líneas y relanzar `./mvnw test` → **22/22 en verde**.
5. Frase de cierre: *"La red de seguridad está de vuelta. La arquitectura se mantiene sola en cada build."*

⚠️ **No olvidar**: dejar las líneas **comentadas** al acabar (estado commitado). Si la sala aprieta, este bonus se puede saltar sin afectar al resto — por eso va al final del bloque.

### 🗑️ Bonus — El módulo borrable *(2 min, la prueba del desacoplamiento)*

**Objetivo**: demostrar en directo la frase estrella de la slide 8: *"podríamos borrar el módulo notification y ningún otro cambiaría ni una línea"*.

**Narración**: "¿Recordáis que dije que Notification podría desaparecer? No lo vamos a borrar, solo lo desenchufamos: **un comentario de una línea**."

**Acción** (en `src/main/java/com/example/tickets/notification/BookingNotificationHandler.java`):

```java
// @Component   // ⚠️ DEMO: módulo notification desactivado
```

(sin tocar nada más — ni imports, ni config)

**Reiniciar la app** y repetir una reserva:

1. Crear una reserva en Swagger o con curl.
2. **Señalar en los logs**:

```text
🎟️ [Seats] BookingConfirmed para reserva 1: reservando 2 plazas del evento 1
✅ [Seats] Aforo reservado para la reserva 1
```

...y **ni una mención de [Notification]**: cero errores, cero stacktraces, el aforo reservado (`reservedSeats: 2`), el cliente con su respuesta.

**Mensajes a decir**:

- "El módulo de emails ha desaparecido y **el negocio sigue**. Ni Booking ni Seats saben que falta: solo conocen un evento que **nadie** escucha ahora... y eso tampoco rompe nada."
- "Fijaos en el registro: para el mismo evento ya solo se crea la publicación de Seats. Los listeners se descubren en runtime, no están cableados a mano."
- Remate: "Esto es lo que compra la comunicación por eventos: **sumar o quitar consumidores es una decisión de despliegue, no un proyecto de refactorización**."

**Reversión**: quitar el comentario de `@Component`, reiniciar → los emails vuelven. Suite completa en verde (22/22).

⚠️ **No olvidar**: restaurar el `@Component` al acabar (estado commitado). Este bonus y el 💥 se complementan: el 💥 muestra que la build **prohíbe** el acoplamiento indebido; este 🗑️ muestra la **recompensa** del desacoplamiento correcto.

### 🗺️ Extra 1 — Los endpoints siguen la estructura de carpetas *(1-2 min)*

**Objetivo**: demostrar que la superficie REST hereda la modularidad — no hay un "mega controller".

**Acción** (dos pantallas, 30 s cada una):

1. **Árbol del proyecto en el IDE** — señalar el espejo carpeta↔endpoint:

```
com/example/tickets/
├── events/EventController.java        →  /api/events
├── booking/BookingController.java     →  /api/bookings
└── seats/SeatController.java          →  /api/seats/{eventId}
```

2. **Swagger UI** — abrir el desplegable de grupos: **`01-events`**, **`02-booking`**, **`03-seats`** (configurados por paquete en `OpenApiGroupsConfig`).

**Mensajes a decir**:

- "Cada módulo posee **sus** endpoints: la URL refleja el paquete. El API no es un vertedero: es un espejo de la estructura de carpetas."
- Abrir el grupo `02-booking`: "solo sus 4 endpoints — nada de Events ni de Seats se cuela aquí."
- Remate: "¿y Notification? **No tiene endpoints**: es consumidor puro de eventos. Su 'API' son los listeners, y no aparece en Swagger porque no expone REST."

### 📄 Extra 2 — Documentación viva: PlantUML y C4 desde el código *(2 min)*

**Objetivo**: demostrar que la documentación de arquitectura **se genera** y no se mantiene a mano.

**Acción**:

1. Ejecutar (si no se acaba de hacer en el bonus 💥):

```bash
./mvnw test -Dtest=ModularityTests
```

2. Abrir **`target/spring-modulith-docs/`** y enseñar:
   - **`components.puml`** — el diagrama global de componentes: los 4 módulos y sus dependencias, **idéntico a la slide 8**.
   - **`module-booking.puml`**, `module-events.puml`, `module-seats.puml`, `module-notification.puml` — un diagrama por módulo, con sus API expuestas y los eventos que publica/escucha.
   - **`all-docs.adoc`** — el documento C4 agregado (modelado de componentes y relaciones).

**Mensajes a decir**:

- "Esto **no está dibujado a mano**: se genera desde el código en cada ejecución de tests. El diagrama de arquitectura no puede quedar obsoleto porque no lo edita nadie."
- Conectar con el bonus 💥: "y si violas las reglas, ni siquiera llega a generarse — la build falla **antes**."
- Visualizarlo: plugin de PlantUML de IntelliJ/VS Code, o pegando el `.puml` en el servidor de plantuml.com.

---

## Bloque 5 · Cierre — *5 min*

### 🎞️ Diapositiva 15 — Cierre (innusual)

Mensaje final en 3 frases (la síntesis):

1. **"Un monolito con fronteras genuinas"**: módulos que la build verifica, no que un diagrama promete.
2. **"Comunicación por eventos con entrega fiable"**: nada se pierde entre commit y consumidor — y se ve en runtime.
3. **"El mejor trampolín a microservicios"**: cada módulo ya está aislado, testeado y documentado; extraerlo es una decisión de negocio, no una reescritura.

- Agradecer + abrir Q&A.
- Enlaces si procede: repo con el código, `SPRING_MODULITH.md` (el informe con todo esto por escrito).

---

## 📌 Los 10 puntos que NO se te pueden escapar

1. ⭐ La tesis: *"las fronteras vuelven **dentro** del monolito"* (slide 4).
2. ⭐ El módulo borrable: *"Notification desaparece y nadie cambia ni una línea"* (slide 8).
3. ⭐ "La petición HTTP termina en milisegundos; el resto ocurre después" (slide 11).
4. ⭐ Las filas de `event_publication` se insertan **en la misma transacción** que la reserva (demo, paso 2 → H2).
5. ⭐ Los listeners corren **AFTER_COMMIT, asíncronos y en transacción propia** (`REQUIRES_NEW`) — una anotación: `@ApplicationModuleListener`.
6. ⭐ Fallo de un consumidor → publicación **incompleta** → **reintento** (no pérdida).
7. ⭐ La regla síncrono/asíncrono: "resultado necesario ahora" vs. "el negocio sigue sin esperar" (slide 11, bloque <3>).
8. ⭐ La saga en miniatura: confirmar ↔ cancelar como evento y contrapartida (slide 11, bloque <4>).
9. ⭐ La arquitectura se **verifica en cada build** (y en cada test de módulo) — demo 💥 del `Cycle detected` en directo. Y la regla de fronteras (slide 10): raíz del paquete = API pública, subpaquetes = internos.
10. ⭐ Documentación viva: PlantUML/C4 generados desde el código — "el diagrama no puede mentir".

---

## 🛡️ Preguntas probables (y respuesta corta)

| Pregunta | Respuesta corta |
|---|---|
| ¿Y si necesito la respuesta de Seats antes de responder al cliente? | Entonces esa lógica no es asíncrona: o la haces síncrona (API pública, como booking→events) o replanteas el contrato. La regla: síncrono si el resultado es prerrequisito. |
| ¿Los eventos son "al menos una vez"? ¿Y los duplicados? | Sí, at-least-once: los consumidores deben ser **idempotentes** (reservar plazas por bookingId, no por acumulación). |
| ¿Esto escala a varios nodos? | El registry JPA funciona, pero para distribución real: externalización de eventos a Kafka/RabbitMQ (Modulith lo soporta con `@Externalized`). |
| ¿Por qué no microservicios directamente? | Complejidad de distribución (latencia, consistencia, operativa) desde el día 1. El modulith la difiere hasta que tienes datos, no especulación. |
| ¿Cómo sabe Modulith qué es público y qué interno? | Paquete raíz del módulo = API pública; subpaquetes = internos. Verificado por ArchUnit en cada build. |
| ¿Qué pasa si dos módulos necesitan lo mismo? | Sube la lógica al módulo que la posee y expón API, o un tercer módulo del que ambos dependan. Lo que **no** puede pasar es un ciclo — la build lo impide. |

---

**Distribución de tiempos**: Apertura 2' · Por qué 9' · Arquitectura 7' · Demo (con bonus 💥 y extras 🗺️📄 si sobra tiempo) 12' · Cierre 3'.
