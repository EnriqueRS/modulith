package com.example.tickets.seats;

import com.example.tickets.events.Event;
import com.example.tickets.events.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private final Event event = Event.builder()
            .id(7L)
            .name("Teatro")
            .totalCapacity(100)
            .build();

    @Test
    void shouldInitializeInventoryLazilyAndReserve() {
        when(seatInventoryRepository.findByEventId(7L)).thenReturn(Optional.empty());
        when(eventService.getEventById(7L)).thenReturn(event);
        when(seatInventoryRepository.save(any(SeatInventory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SeatInventory inventory = seatService.reserveSeats(7L, 10);

        assertThat(inventory.getTotalSeats()).isEqualTo(100);
        assertThat(inventory.getReservedSeats()).isEqualTo(10);
        assertThat(inventory.getAvailableSeats()).isEqualTo(90);
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

        SeatInventory inventory = seatService.releaseSeats(7L, 5);

        assertThat(inventory.getReservedSeats()).isEqualTo(15);
    }
}
