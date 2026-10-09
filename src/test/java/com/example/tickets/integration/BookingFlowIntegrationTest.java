package com.example.tickets.integration;

import com.example.tickets.booking.Booking;
import com.example.tickets.booking.BookingService;
import com.example.tickets.booking.CreateBookingRequest;
import com.example.tickets.events.Event;
import com.example.tickets.events.EventService;
import com.example.tickets.seats.SeatInventory;
import com.example.tickets.seats.SeatInventoryRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración end-to-end: comprueba el flujo completo del modulith.
 *
 * <p>Reserva confirmada → BookingConfirmedEvent publicado y registrado en la
 * tabla {@code event_publication} → handler de seats reserva el aforo
 * (asíncrono, tras el commit) → handler de notification envía el email.
 *
 * <p>Al final, NO debe quedar ninguna publicación incompleta: si un handler
 * hubiera fallado, el evento quedaría pendiente de reintento.
 */
@SpringBootTest
class BookingFlowIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatInventoryRepository seatInventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Event createEvent(int capacity) {
        return eventService.createEvent(Event.builder()
                .name("Evento Integración " + System.nanoTime())
                .venue("Sala Test")
                .eventDate(Instant.parse("2026-12-10T20:00:00Z"))
                .price(new BigDecimal("15.00"))
                .totalCapacity(capacity)
                .build());
    }

    private int pendingPublications() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL", Integer.class);
        return count == null ? 0 : count;
    }

    @Test
    void confirmedBookingShouldReserveSeatsAsynchronously() {
        Event event = createEvent(100);

        Booking booking = bookingService.createBooking(new CreateBookingRequest(event.getId(), "flow@example.com", 3));

        // El listener es asíncrono (AFTER_COMMIT): esperamos a que se procese
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    SeatInventory inventory = seatInventoryRepository.findByEventId(event.getId()).orElseThrow();
                    assertThat(inventory.getReservedSeats()).isEqualTo(3);
                });

        // El registro de publicaciones no debe dejar eventos pendientes
        // (también asíncrono: esperamos a que todos los handlers completen)
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(pendingPublications()).isZero());
    }

    @Test
    void cancelledBookingShouldReleaseSeatsAsynchronously() {
        Event event = createEvent(100);
        Booking booking = bookingService.createBooking(new CreateBookingRequest(event.getId(), "flow2@example.com", 4));

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(seatInventoryRepository.findByEventId(event.getId()).orElseThrow()
                                .getReservedSeats()).isEqualTo(4));

        bookingService.cancelBooking(booking.getId(), "Test integración");

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertThat(seatInventoryRepository.findByEventId(event.getId()).orElseThrow()
                                .getReservedSeats()).isZero());

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(pendingPublications()).isZero());
    }

    @Test
    void completedPublicationsAreKeptForAudit() {
        Event event = createEvent(50);
        bookingService.createBooking(new CreateBookingRequest(event.getId(), "audit@example.com", 1));

        // Con spring.modulith.events.disable-deletion=true, el historial queda en la tabla
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    Integer completed = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL", Integer.class);
                    assertThat(completed).isNotNull();
                    assertThat(completed).isGreaterThanOrEqualTo(1);
                });

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(pendingPublications()).isZero());
    }
}
