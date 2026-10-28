package com.example.tickets.events;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista pública de un evento: lo único que otros módulos ven de
 * {@code events}. La entidad {@code Event} vive en {@code events.internal}
 * y no se expone.
 */
public record EventInfo(
        Long id,
        String name,
        String venue,
        Instant eventDate,
        BigDecimal price,
        int totalCapacity
) {
}
