package com.example.tickets.events;

import com.example.tickets.events.internal.Event;
import com.example.tickets.events.internal.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test unitario del módulo events: sin contexto de Spring, solo Mockito.
 * Usa los internos (entidad y repositorio) porque el test vive en el módulo.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event festival() {
        return Event.builder()
                .id(1L)
                .name("Festival de Verano")
                .venue("Plaza Mayor")
                .eventDate(Instant.parse("2026-12-01T20:00:00Z"))
                .price(new BigDecimal("45.00"))
                .totalCapacity(200)
                .build();
    }

    private CreateEventRequest festivalRequest() {
        return new CreateEventRequest(
                "Festival de Verano", "Plaza Mayor",
                Instant.parse("2026-12-01T20:00:00Z"), new BigDecimal("45.00"), 200);
    }

    @Test
    void shouldCreateEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(festival());

        EventInfo created = eventService.createEvent(festivalRequest());

        assertThat(created.name()).isEqualTo("Festival de Verano");
        assertThat(created.totalCapacity()).isEqualTo(200);
    }

    @Test
    void shouldGetAllEvents() {
        when(eventRepository.findAll()).thenReturn(List.of(festival()));

        List<EventInfo> events = eventService.getAllEvents();

        assertThat(events).hasSize(1);
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }
}
