package com.example.tickets.booking;

import com.example.tickets.booking.internal.Booking;
import com.example.tickets.booking.internal.BookingRepository;
import com.example.tickets.booking.internal.BookingStatus;
import com.example.tickets.events.EventInfo;
import com.example.tickets.events.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test unitario del módulo booking: comprueba que al crear/cancelar
 * se publica el evento de dominio correcto (el contrato de BookingEvents)
 * y que la API devuelve DTOs, no entidades internas.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventService eventService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    private EventInfo concert;

    @BeforeEach
    void setUp() {
        concert = new EventInfo(5L, "Concierto Rock", "Sala Apolo",
                Instant.parse("2026-11-15T21:00:00Z"), new BigDecimal("30.00"), 500);
    }

    @Test
    void shouldCreateBookingAndPublishConfirmedEvent() {
        when(eventService.getEventById(5L)).thenReturn(concert);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(inv -> {
                    Booking b = inv.getArgument(0);
                    b.setId(42L);
                    return b;
                });

        BookingInfo booking = bookingService.createBooking(new CreateBookingRequest(5L, "unit@example.com", 2));

        assertThat(booking.status()).isEqualTo("CONFIRMED");
        assertThat(booking.totalAmount()).isEqualByComparingTo(new BigDecimal("60.00"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());

        BookingEvents.BookingConfirmedEvent event = (BookingEvents.BookingConfirmedEvent) captor.getValue();
        assertThat(event.bookingId()).isEqualTo(42L);
        assertThat(event.eventId()).isEqualTo(5L);
        assertThat(event.quantity()).isEqualTo(2);
    }

    @Test
    void shouldCancelBookingAndPublishCancelledEvent() {
        Booking existing = Booking.builder()
                .id(42L)
                .customerEmail("unit@example.com")
                .eventId(5L)
                .quantity(2)
                .totalAmount(new BigDecimal("60.00"))
                .status(BookingStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();
        when(bookingRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingInfo cancelled = bookingService.cancelBooking(42L, "Test unitario");

        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.cancellationReason()).isEqualTo("Test unitario");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BookingEvents.BookingCancelledEvent event = (BookingEvents.BookingCancelledEvent) captor.getValue();
        assertThat(event.bookingId()).isEqualTo(42L);
        assertThat(event.reason()).isEqualTo("Test unitario");
    }
}
