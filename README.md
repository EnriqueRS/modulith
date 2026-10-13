# event-tickets-modulith

Sistema de **reserva de entradas para eventos** construido como **Spring Modulith** — un monolito con módulos verificados, comunicación por eventos fiable y documentación de arquitectura generada desde el código.

> 📘 **Informe detallado**: [docs/SPRING_MODULITH.md](docs/SPRING_MODULITH.md) — qué aporta Spring Modulith y dónde está implementado, con referencias concretas a código.

## Stack

| Capa | Tecnología |
|---|---|
| Base | Java 21, Spring Boot 3.5.16, Maven |
| Modularidad | Spring Modulith 1.4.13 |
| Persistencia | JPA (Hibernate) + H2 en memoria |
| API | REST + springdoc OpenAPI (Swagger UI) |
| Observabilidad | Actuator: `/actuator/modulith` + métricas Micrometer |

## Módulos

```
com.example.tickets
├── events        Catálogo de eventos (conciertos): API pública EventService
├── booking       Reservas: publica BookingConfirmedEvent / BookingCancelledEvent
├── seats         Aforo: escucha eventos y reserva/libera plazas
└── notification  Emails (simulados por log): escucha los mismos eventos
```

- Dependencias síncronas: `booking → events`, `seats → events` (solo la API pública).
- Dependencias asíncronas: `booking → {seats, notification}` por eventos, con registro persistente en la tabla `event_publication`, reintentos al arrancar y observación vía actuator.

```
        (síncrono: valida evento y precio)      (síncrono: aforo inicial)
Booking ─────────────────────────────▶ Event ◀─────────────────────── Seats
  │
  ├─ BookingConfirmedEvent / BookingCancelledEvent  (AFTER_COMMIT, asíncrono,
  │        │                                         registrado en event_publication)
  │        ├────────▶ Seats         reserva / libera aforo
  │        └────────▶ Notification  email de confirmación / cancelación
```

## Requisitos

- **JDK 21**. En esta máquina el wrapper ya lo resuelve solo: si `JAVA_HOME` no está definido, `./mvnw` usa `/usr/lib/jvm/java-21-openjdk-amd64` automáticamente (parche local en el script `mvnw` — el `openjdk-25` del sistema es solo JRE y no incluye `javac`).
- En **IntelliJ**: *Project Structure → SDK = 21*. Si delegas build/run en Maven, ajusta también el JRE del runner de Maven a JDK 21.

## Ejecutar

```bash
./mvnw spring-boot:run
```

- App: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html> (agrupada por módulo: `01-events`, `02-booking`, `03-seats` — la API refleja la estructura de carpetas)
- Arquitectura de módulos: <http://localhost:8080/actuator/modulith>
- Métricas de eventos: <http://localhost:8080/actuator/metrics/modulith.events.processed>
- Consola H2: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:ticketsdb`, usuario `sa`; la tabla `event_publication` guarda el registro de eventos)

### Prueba rápida

```bash
# 1. Crear evento
curl -X POST http://localhost:8080/api/events -H "Content-Type: application/json" \
  -d '{"name":"Concierto Demo","venue":"Sala Apolo","eventDate":"2026-12-01T20:00:00Z","price":25.00,"totalCapacity":50}'

# 2. Reservar 2 entradas (dispara BookingConfirmedEvent)
curl -X POST http://localhost:8080/api/bookings -H "Content-Type: application/json" \
  -d '{"eventId":1,"customerEmail":"demo@example.com","quantity":2}'

# 3. Ver aforo reservado, métricas y el registro de publicaciones
curl http://localhost:8080/api/seats/1
curl http://localhost:8080/actuator/metrics/modulith.events.processed
# Tabla event_publication (histórico con completion-mode=UPDATE):
# http://localhost:8080/h2-console -> SELECT event_type, completion_date FROM event_publication;

# 4. Cancelar la reserva (dispara BookingCancelledEvent y libera aforo)
curl -X POST "http://localhost:8080/api/bookings/1/cancel?reason=Cliente%20no%20puede%20asistir"
curl http://localhost:8080/api/seats/1   # reservedSeats vuelve a 0
```

## Tests

```bash
./mvnw test
```

- `ModularityTests` — ⭐ verifica la arquitectura (sin dependencias internas entre módulos, sin ciclos) y genera la documentación en `target/spring-modulith-docs/` (PlantUML + C4).
- `*ModuleTest` — tests de módulo aislado (`@ApplicationModuleTest`): cada módulo arranca solo.
- `*ServiceTest` — tests unitarios (Mockito).
- `BookingFlowIntegrationTest` — flujo end-to-end: reserva → evento asíncrono → aforo reservado, sin publicaciones incompletas.

### Demo de arquitectura

Rompe una regla y la build te lo impedirá: la guía completa, con los pasos exactos y la salida real de `verify()`, está en [docs/SPRING_MODULITH.md, sección 6](docs/SPRING_MODULITH.md#6-prueba-de-fuego-viola-una-frontera-y-mira-cómo-la-build-te-lo-impide). En resumen: inyecta `SeatService` en `BookingService` (un solo fichero) y ejecuta `./mvnw test` — `ModularityTests` fallará con `Cycle detected: booking → seats → booking`, señalando la clase, el método y la línea exacta.
