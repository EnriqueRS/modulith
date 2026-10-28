package com.example.tickets.integration;

import com.example.tickets.booking.BookingInfo;
import com.example.tickets.booking.BookingService;
import com.example.tickets.booking.CreateBookingRequest;
import com.example.tickets.events.CreateEventRequest;
import com.example.tickets.events.EventInfo;
import com.example.tickets.events.EventService;
import com.example.tickets.seats.SeatAvailability;
import com.example.tickets.seats.SeatService;
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
 * Test de integración end-to-end: comprueba el flujo completo del modulith
 * usando SOLO las APIs públicas de los módulos (los internos no se tocan).
 *
 * <p>Reserva confirmada -> BookingConfirmedEvent registrado en
 * {@code event_publication} -> handler de seats reserva el aforo (asíncrono,
 * tras el commit) -> handler de notification envía el email.
 *
 * <p>Al final, NO debe quedar ninguna publicación incompleta.
 */
@SpringBootTest
class BookingFlowIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private EventInfo createEvent(int capacity) {
        return eventService.createEvent(new CreateEventRequest(
                "Evento Integración " + System.nanoTime(),
                "Sala Test",
                Instant.parse("2026-12-10T20:00:00Z"),
                new BigDecimal("15.00"),
                capacity));
    }

    private int pendingPublications() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL", Integer.class);
        return count == null ? 0 : count;
    }

    @Test
    void confirmedBookingShouldReserveSeatsAsynchronously() {
        EventInfo event = createEvent(100);

        BookingInfo booking = bookingService.createBooking(new CreateBookingRequest(event.id(), "flow@example.com", 3));

        // El listener es asíncrono (AFTER_COMMIT): esperamos a que se procese
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    SeatAvailability availability = seatService.getAvailability(event.id()).orElseThrow();
                    assertThat(availability.reservedSeats()).isEqualTo(3);
                });

        assertThat(booking.status()).isEqualTo("CONFIRMED");

        // El registro de publicaciones no debe dejar eventos pendientes
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(pendingPublications()).isZero());
    }

    @Test
    void cancelledBookingShouldReleaseSeatsAsynchronously() {
        EventInfo event = createEvent(100);
        BookingInfo booking = bookingService.createBooking(new CreateBookingRequest(event.id(), "flow2@example.com", 4));

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(seatService.getAvailability(event.id()).orElseThrow()
                        .reservedSeats()).isEqualTo(4));

        bookingService.cancelBooking(booking.id(), "Test integración");

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(seatService.getAvailability(event.id()).orElseThrow()
                        .reservedSeats()).isZero());

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(pendingPublications()).isZero());
    }

    @Test
    void completedPublicationsAreKeptForAudit() {
        EventInfo event = createEvent(50);
        bookingService.createBooking(new CreateBookingRequest(event.id(), "audit@example.com", 1));

        // Con completion-mode=UPDATE, el historial queda en la tabla
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
