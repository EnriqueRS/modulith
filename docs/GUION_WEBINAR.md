# 🎤 Guion — Webinar "Introducción a Spring Modulith"

**Duración objetivo: 30 min** (+ Q&A) · **Proyecto**: `event-tickets-modulith` · **Deck**: 13 diapositivas

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
- [ ] (Si piensas romper la modularidad en directo) tener el diff de la demo preparado para pegar — ver `SPRING_MODULITH.md` §6

---

## Bloque 1 · Apertura — *2 min*

### 🎞️ Diapositiva 1 — Portada

**Objetivo**: poner el tema y el hilo conductor.

- Saludo + presentación personal (30 s).
- "Hoy hablamos de **modularidad dentro del monolito**: cómo Spring Modulith devuelve las fronteras a aplicaciones Spring Boot."
- Hilo conductor: **"lo vamos a ver sobre un sistema de reserva de entradas que está funcionando ahora mismo en esta máquina"**.
- ⚠️ No entrar en detalle del proyecto todavía — solo dejar sembrado que habrá demo en vivo.

➡️ *Transición*: "Vamos con la agenda."

### 🎞️ Diapositiva 2 — Índice (3 bloques)

**Objetivo**: contrato de la sesión.

- **<1> ¿Por qué modularidad?** — los dolores que vivimos a diario.
- **<2> Arquitectura del ejemplo** — los 4 módulos y cómo se comunican.
- **<3> Demo en vivo** — todo el flujo de una reserva, en directo.
- Recordar: "al final, preguntas".
- ⚠️ OJO: el índice tiene **3 bloques** (no mencionar testing como sección — el testing se menciona dentro del bloque 2).

➡️ *Transición*: "Empecemos por el dolor."

---

## Bloque 2 · ¿Por qué modularidad? — *8 min*

### 🎞️ Diapositiva 3 — Divider <1>

Solo respirar: "¿Por qué modularidad? Lo que vivimos a diario."

### 🎞️ Diapositiva 4 — El problema del monolito clásico

**Objetivo**: que la audiencia reconozca SUS dolores.

Los 4 dolores (uno por tarjeta), en este orden y con su frase-gancho:

1. **ACOPLAMIENTO** — "Cambiar una clase rompe tres módulos: sin fronteras, cada cambio se propaga de forma impredecible."
2. **TESTS LENTOS** — "Arrancar todo el contexto para cada test es un cuello de botella. Guarda esta idea: la modularidad también la arregla — cada test arranca **solo lo que toca**." *(siembra la demo de tests de módulo)*
3. **MIEDO A REFACTORIZAR** — "Sin validación de dependencias, 'limpiar' código es una apuesta."
4. **ONBOARDING DIFÍCIL** — "Un desarrollador nuevo necesita semanas para entender un grafo de dependencias oculto."

⚠️ **No olvidar**: la frase de cierre de la slide — *"Spring Modulith trae de vuelta las fronteras **dentro** del monolito"* — es la tesis de todo el webinar.

➡️ *Transición*: "¿Y qué opciones hay? Tres, en realidad."

### 🎞️ Diapositiva 5 — Tres formas de estructurar un sistema

**Objetivo**: situar el punto medio.

- **<1> Monolito clásico**: rápido de empezar, cualquier clase llama a cualquier clase.
- **<2> Monolito modular** *(la destacada, fondo verde)*: "la simplicidad del monolito con la disciplina de los microservicios".
- **<3> Microservicios**: máxima autonomía, pero pagas latencia, consistencia eventual y operativa.
- Cierre con el banner: **"Spring Modulith vive en el punto medio: la evolución natural cuando el monolito se queda pequeño pero los microservicios son demasiado."**
- 💡 Si hay tiempo: "y cada módulo bien hecho es un **candidato natural a extraerse** como microservicio — pagas el peaje solo cuando toca".

➡️ *Transición*: "¿Y qué es exactamente Spring Modulith?"

### 🎞️ Diapositiva 6 — ¿Qué es Spring Modulith?

**Objetivo**: los 4 pilares (izquierda: definición; derecha: capacidades).

Definición en 2 frases (columna izquierda):

- "Un proyecto del ecosistema Spring que impone **estructura modular a Spring Boot mediante convenciones y validación automática**."
- "**Cada paquete directo** bajo el paquete de la aplicación es un módulo; lo que cuelga de subpaquetes es **interno**. Verifica en tests que solo se usen las APIs públicas."
- "Comunicación: llamada directa (API pública) o **eventos de dominio desacoplados**."

Los 4 pilares (derecha), con una frase cada uno:

1. **LÍMITES VALIDADOS** — ArchUnit bajo el capó; la build falla si un módulo toca interioridades de otro. *"La arquitectura se ejecuta en cada `mvn test`."*
2. **EVENTOS ENTRE MÓDULOS** — `@ApplicationModuleListener`: asíncrono, transaccional, **persistido con reintentos** si un consumidor falla.
3. **TESTING AISLADO** — `@ApplicationModuleTest` arranca **solo el módulo** bajo prueba.
4. **DOCUMENTACIÓN VIVA** — `Documenter` genera PlantUML y C4 **desde el código real**: *"el diagrama nunca queda obsoleto porque se dibuja en cada build"*.

⚠️ **No olvidar**: anticipar la demo — "todo esto lo vamos a **ver**, no solo contar".

➡️ *Transición*: "Vamos a la arquitectura del ejemplo."

---

## Bloque 3 · Arquitectura del ejemplo — *5 min*

### 🎞️ Diapositiva 7 — Divider <2>

"Arquitectura del ejemplo: un sistema de reserva de entradas para eventos. Pequeño a propósito: comunicación real entre módulos, sin ruido."

### 🎞️ Diapositiva 8 — Los 4 módulos del proyecto

**Objetivo**: presentar el mapa mental que se usará el resto del webinar.

Presentar cada tarjeta con su frase:

- **EVENTS** — "La fuente de verdad del dominio de evento: altas, precios, aforo. API REST clásica. **No sabe nada** de reservas."
- **BOOKING** — "Orquesta: crea y cancela reservas. Llama a Events **síncronamente** cuando necesita el precio, y **publica eventos** al confirmar o cancelar."
- **SEATS** — "Gestiona el aforo. No llama a nadie: **escucha**. BookingConfirmed reserva plazas, BookingCancelled las libera."
- **NOTIFICATION** — "Emails simulados en logs. **Solo consume eventos**: podríamos borrar este módulo entero y ningún otro cambiaría ni una línea. Ese es el nivel de desacoplamiento que buscamos." ⚠️ **Frase estrella — no olvidarla.**

⚠️ **No olvidar**: señalar que **no hay config** — "la estructura de paquetes ES la arquitectura; Modulith la detecta sola".

➡️ *Transición*: "Veamos el flujo completo de una reserva — aquí está todo el juego."

### 🎞️ Diapositiva 9 — Flujo de una reserva

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

### 🎞️ Diapositiva 10 — Divider <3> Demo en vivo

"Momento de la verdad." (cambia a terminal/Swagger)

### 🎞️ Diapositiva 11 — Pasos de la demo *(segunda pantalla, para no perderse)*

Los 4 pasos, con su narración:

**<1> CREAR EVENTO** — `POST /api/events` (Swagger o curl):

- "Damos de alta un concierto con su aforo (50). Fijaos: **Events no sabe nada** de reservas ni de notificaciones — solo guarda su dominio."
- ✅ Verificar respuesta: `id=1`.

**<2> CREAR RESERVA** — `POST /api/bookings`:

- "Booking valida el evento, calcula el total (2 × 25 = **50.00**) y publica `BookingConfirmedEvent` **dentro de su transacción**."
- ⚠️ **No olvidar** mirar el reloj: "la respuesta HTTP volvió en milisegundos — **antes** de que pasara todo lo demás".

**<3> OBSERVAR EL EFECTO** — aquí se luce el proyecto (ir enseñando cada cosa):

- 🖥️ **Logs**: aparecen `🎟️ [Seats] BookingConfirmed… reservando 2 plazas` y `📬 [Notification]` — "**después** de que el cliente recibió su respuesta. Ese orden es la prueba del desacoplamiento."
- 🗄️ **Consola H2**: `SELECT event_type, completion_date FROM event_publication;` → "cada evento quedó **registrado y completado** — entrega fiable, no fire-and-forget".
- 📊 **`/actuator/metrics/modulith.events.processed`** → contador con tags `event_type` y `module_name`.
- 🏗️ **`/actuator/modulith`** → "la arquitectura en runtime: véis los 4 módulos y el tipo de cada dependencia — `USES_COMPONENT` (booking→events) y `EVENT_LISTENER` (seats→booking)."

**<4> CANCELAR Y VER REINTENTOS** — `POST /api/bookings/1/cancel`:

- "`BookingCancelledEvent` libera el aforo y avisa al cliente. **Saga**: cada evento tiene su contrapartida."
- 💡 Opcional si va con tiempo (el golpe de efecto): **agotar el aforo a propósito** — reservar más de 50 y ver en H2 la publicación con `completion_date NULL` → "queda **pendiente de reintento**; si libero aforo y reinicio, se procesa. Con ApplicationEvent a secas, este evento se habría perdido."
- 💡 Opcional (demo de arquitectura, `SPRING_MODULITH.md` §6): inyectar `SeatService` en `BookingService` y que la build escupa el `Cycle detected` con la línea exacta. ⚠️ Solo si dominas los tiempos.

⚠️ **No olvidar**: los JSON van **copiados** — escribir JSON en directo es tiempo muerto.

### 🎞️ Diapositiva 12 — Logs en vivo *(recapitulación de la terminal)*

Usar la slide como **mapa de lo que acaba de pasar** en la terminal:

- "Primero la petición: Booking crea la **reserva #1**, 4 entradas, total 160, `CONFIRMED`."
- "Se publica el evento, y fijaos en las **etiquetas entre corchetes**: cada módulo firma sus logs — podemos seguir la traza de una reserva leyendo solo un color."
- "Seats reserva 4 plazas: el aforo pasa de 80 a **76 disponibles**. Notification envía el email."
- "Y por último el `HTTP 201 Created` en 91 ms. **El cliente no esperó ni al aforo ni al email. Esa asimetría temporal es exactamente el desacoplamiento.**"

⚠️ **No olvidar**: cerrar el círculo con el Event Publication Registry — "si cualquier listener falla, no se pierde nada: queda registrado y se reintenta".

---

## Bloque 5 · Cierre — *5 min*

### 🎞️ Diapositiva 13 — Cierre (innusual)

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
3. ⭐ "La petición HTTP termina en milisegundos; el resto ocurre después" (slide 9).
4. ⭐ Las filas de `event_publication` se insertan **en la misma transacción** que la reserva (demo, paso 2 → H2).
5. ⭐ Los listeners corren **AFTER_COMMIT, asíncronos y en transacción propia** (`REQUIRES_NEW`) — una anotación: `@ApplicationModuleListener`.
6. ⭐ Fallo de un consumidor → publicación **incompleta** → **reintento** (no pérdida).
7. ⭐ La regla síncrono/asíncrono: "resultado necesario ahora" vs. "el negocio sigue sin esperar" (slide 9, bloque <3>).
8. ⭐ La saga en miniatura: confirmar ↔ cancelar como evento y contrapartida (slide 9, bloque <4>).
9. ⭐ La arquitectura se **verifica en cada build** (y en cada test de módulo) — demo del `Cycle detected` si hay tiempo.
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

**Distribución de tiempos**: Apertura 2' · Por qué 8' · Arquitectura 5' · Demo 10' · Cierre+Q&A 5'.
