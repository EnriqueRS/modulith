package com.example.tickets.seats;

import com.example.tickets.events.Event;
import com.example.tickets.events.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de MÓDULO para seats con Spring Modulith.
 *
 * <p>STANDALONE: el módulo seats arranca solo. seats depende síncronamente de
 * events (para conocer el aforo inicial), así que EventService se mockea.
 * La comunicación con booking es por eventos, así que booking NO hace falta:
 * el módulo solo conoce el contrato BookingEvents.
 *
 * <p>Cada test usa un eventId distinto: el contexto (y su H2) se comparte
 * entre métodos, y el inventario se inicializa perezosamente.
 */
@ApplicationModuleTest
class SeatsModuleTest {

    @MockitoBean
    private EventService eventService;

    @Autowired
    private SeatService seatService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(eventService.getEventById(ArgumentMatchers.anyLong()))
                .thenReturn(Event.builder()
                        .id(1L)
                        .name("Evento Mock")
                        .totalCapacity(100)
                        .eventDate(Instant.parse("2026-11-15T21:00:00Z"))
                        .build());
    }

    @Test
    void shouldReserveSeatsWithLazyInventoryInitialization() {
        SeatInventory inventory = seatService.reserveSeats(101L, 5);

        assertThat(inventory.getTotalSeats()).isEqualTo(100);
        assertThat(inventory.getReservedSeats()).isEqualTo(5);
        assertThat(inventory.getAvailableSeats()).isEqualTo(95);
    }

    @Test
    void shouldReleaseSeats() {
        seatService.reserveSeats(102L, 5);
        SeatInventory inventory = seatService.releaseSeats(102L, 2);

        assertThat(inventory.getReservedSeats()).isEqualTo(3);
    }

    @Test
    void shouldThrowOnInsufficientSeats() {
        seatService.reserveSeats(103L, 100);

        assertThatThrownBy(() -> seatService.reserveSeats(103L, 1))
                .isInstanceOf(InsufficientSeatsException.class);
    }
}
