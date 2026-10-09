package com.example.tickets.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Petición de creación de reserva.
 */
public record CreateBookingRequest(
        @NotNull Long eventId,
        @NotBlank String customerEmail,
        @Min(1) int quantity
) {
}
