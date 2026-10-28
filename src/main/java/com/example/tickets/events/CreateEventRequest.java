package com.example.tickets.events;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato del endpoint de alta de eventos.
 */
public record CreateEventRequest(
        @NotBlank String name,
        @NotBlank String venue,
        @NotNull Instant eventDate,
        @NotNull BigDecimal price,
        @Min(1) int totalCapacity
) {
}
