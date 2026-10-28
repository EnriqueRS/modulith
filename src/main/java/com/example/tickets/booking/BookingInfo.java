package com.example.tickets.booking;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista pública de una reserva. La entidad {@code Booking} y su estado
 * interno no salen del módulo.
 */
public record BookingInfo(
        Long id,
        String customerEmail,
        Long eventId,
        int quantity,
        BigDecimal totalAmount,
        String status,
        String cancellationReason,
        Instant createdAt
) {
}
