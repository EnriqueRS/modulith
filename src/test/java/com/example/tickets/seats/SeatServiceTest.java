package com.example.tickets.seats;

import com.example.tickets.events.EventInfo;
import com.example.tickets.events.EventService;
import com.example.tickets.seats.internal.InsufficientSeatsException;
import com.example.tickets.seats.internal.SeatInventory;
import com.example.tickets.seats.internal.SeatInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test unitario del módulo seats: reserva, liberación y aforo insuficiente.
 */
@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    private SeatInventoryRepository seatInventoryRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private SeatService seatService;

    private final EventInfo event = new EventInfo(7L, "Teatro", "Sala",
            Instant.parse("2026-12-01T20:00:00Z"), new BigDecimal("10.00"), 100);

    @Test
    void shouldInitializeInventoryLazilyAndReserve() {
        when(seatInventoryRepository.findByEventId(7L)).thenReturn(Optional.empty());
        when(eventService.getEventById(7L)).thenReturn(event);
        when(seatInventoryRepository.save(any(SeatInventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SeatAvailability inventory = seatService.reserveSeats(7L, 10);

        assertThat(inventory.totalSeats()).isEqualTo(100);
        assertThat(inventory.reservedSeats()).isEqualTo(10);
        assertThat(inventory.availableSeats()).isEqualTo(90);
    }

    @Test
    void shouldThrowInsufficientSeats() {
        when(seatInventoryRepository.findByEventId(7L)).thenReturn(Optional.of(
                SeatInventory.builder().eventId(7L).totalSeats(100).reservedSeats(95).build()));

        assertThatThrownBy(() -> seatService.reserveSeats(7L, 10))
                .isInstanceOf(InsufficientSeatsException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    void shouldReleaseSeats() {
        when(seatInventoryRepository.findByEventId(7L)).thenReturn(Optional.of(
                SeatInventory.builder().eventId(7L).totalSeats(100).reservedSeats(20).build()));
        when(seatInventoryRepository.save(any(SeatInventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SeatAvailability inventory = seatService.releaseSeats(7L, 5).orElseThrow();

        assertThat(inventory.reservedSeats()).isEqualTo(15);
    }
}
