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
 * módulo events: ni booking ni seats ni notification se instancian.
 */
@ApplicationModuleTest
class EventsModuleTest {

    @Autowired
    private EventService eventService;

    @Test
    void shouldCreateAndFetchEvent() {
        EventInfo created = eventService.createEvent(new CreateEventRequest(
                "Jazz Night", "Sala Clamores",
                Instant.parse("2026-11-20T21:30:00Z"), new BigDecimal("25.50"), 80));

        EventInfo fetched = eventService.getEventById(created.id());

        assertThat(fetched.name()).isEqualTo("Jazz Night");
        assertThat(fetched.totalCapacity()).isEqualTo(80);
    }
}
