package com.example.tickets.events;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de MÓDULO para events con Spring Modulith.
 *
 * <p>{@code @ApplicationModuleTest} (STANDALONE por defecto) arranca SOLO el
 * módulo events: ni booking ni seats ni notification se instancian. El test es
 * más rápido y demuestra que events no necesita a nadie.
 */
@ApplicationModuleTest
class EventsModuleTest {

    @Autowired
    private EventService eventService;

    @Test
    void shouldCreateAndFetchEvent() {
        Event created = eventService.createEvent(Event.builder()
                .name("Jazz Night")
                .venue("Sala Clamores")
                .eventDate(Instant.parse("2026-11-20T21:30:00Z"))
                .price(new BigDecimal("25.50"))
                .totalCapacity(80)
                .build());

        Event fetched = eventService.getEventById(created.getId());

        assertThat(fetched.getName()).isEqualTo("Jazz Night");
        assertThat(fetched.getTotalCapacity()).isEqualTo(80);
    }
}
