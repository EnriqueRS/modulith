package com.example.tickets.booking;

import com.example.tickets.events.EventInfo;
import com.example.tickets.events.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de MÓDULO para booking con Spring Modulith.
 *
 * <p>STANDALONE: arranca SOLO booking. La dependencia síncrona con events se
 * resuelve con un mock (@MockitoBean) de su API pública (EventService/EventInfo).
 *
 * <p>{@code @RecordApplicationEvents} captura los eventos publicados para
 * verificar el contrato asíncrono sin levantar los consumidores.
 */
@ApplicationModuleTest
@RecordApplicationEvents
class BookingModuleTest {

    @MockitoBean
    private EventService eventService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(eventService.getEventById(1L))
                .thenReturn(new EventInfo(1L, "Concierto Mock", "Sala Mock",
                        Instant.parse("2026-11-15T21:00:00Z"), new BigDecimal("30.00"), 100));
    }

    @Test
    void shouldCreateBookingAndPublishConfirmedEvent() {
        BookingInfo booking = bookingService.createBooking(new CreateBookingRequest(1L, "module@example.com", 2));

        assertThat(booking.id()).isNotNull();
        assertThat(booking.status()).isEqualTo("CONFIRMED");
        assertThat(booking.totalAmount()).isEqualByComparingTo(new BigDecimal("60.00"));

        // El contrato asíncrono del módulo se cumple: BookingConfirmedEvent publicado
        assertThat(events.stream(BookingEvents.BookingConfirmedEvent.class).count()).isEqualTo(1);
        assertThat(events.stream(BookingEvents.BookingConfirmedEvent.class)
                .findFirst().orElseThrow().bookingId()).isEqualTo(booking.id());
    }

    @Test
    void shouldCancelBookingAndPublishCancelledEvent() {
        BookingInfo booking = bookingService.createBooking(new CreateBookingRequest(1L, "module@example.com", 1));

        BookingInfo cancelled = bookingService.cancelBooking(booking.id(), "Test módulo");

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(events.stream(BookingEvents.BookingCancelledEvent.class).count()).isEqualTo(1);
    }
}
