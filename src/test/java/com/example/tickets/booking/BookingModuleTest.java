package com.example.tickets.booking;

import com.example.tickets.events.Event;
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
 * resuelve con un mock (@MockitoBean): el test demuestra que booking solo
 * necesita la API pública del módulo externo.
 *
 * <p>{@code @RecordApplicationEvents} es utilidad de Spring Framework
 * (integrada con los tests de Modulith): captura los eventos publicados para
 * verificar el contrato asíncrono sin levantar los módulos consumidores.
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
                .thenReturn(Event.builder()
                        .id(1L)
                        .name("Concierto Mock")
                        .price(new BigDecimal("30.00"))
                        .totalCapacity(100)
                        .eventDate(Instant.parse("2026-11-15T21:00:00Z"))
                        .build());
    }

    @Test
    void shouldCreateBookingAndPublishConfirmedEvent() {
        Booking booking = bookingService.createBooking(new CreateBookingRequest(1L, "module@example.com", 2));

        assertThat(booking.getId()).isNotNull();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo(new BigDecimal("60.00"));

        // El contrato asíncrono del módulo se cumple: BookingConfirmedEvent publicado
        assertThat(events.stream(BookingEvents.BookingConfirmedEvent.class).count()).isEqualTo(1);
        assertThat(events.stream(BookingEvents.BookingConfirmedEvent.class)
                .findFirst().orElseThrow().bookingId()).isEqualTo(booking.getId());
    }

    @Test
    void shouldCancelBookingAndPublishCancelledEvent() {
        Booking booking = bookingService.createBooking(new CreateBookingRequest(1L, "module@example.com", 1));

        Booking cancelled = bookingService.cancelBooking(booking.getId(), "Test módulo");

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(events.stream(BookingEvents.BookingCancelledEvent.class).count()).isEqualTo(1);
    }
}
