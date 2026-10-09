package com.example.tickets.booking;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato de eventos del módulo {@code booking}.
 *
 * <p>Estos records son la API pública asíncrona del módulo: son lo único que
 * {@code seats} y {@code notification} conocen de {@code booking}. Nunca
 * dependen de {@code Booking}, {@code BookingService} ni {@code BookingRepository},
 * que son internos del módulo (lo verifica {@code ModularityTests}).
 */
public final class BookingEvents {

    private BookingEvents() {
        // Clase de utilidad, no instanciar
    }

    /**
     * Evento emitido al confirmarse una reserva (dentro de la transacción;
     * los listeners se ejecutan tras el commit).
     * {@code seats} reserva aforo y {@code notification} envía el email.
     */
    public record BookingConfirmedEvent(
            Long bookingId,
            Long eventId,
            String customerEmail,
            int quantity,
            BigDecimal totalAmount,
            Instant occurredAt
    ) {}

    /**
     * Evento emitido al cancelarse una reserva.
     * {@code seats} libera el aforo y {@code notification} avisa al cliente.
     */
    public record BookingCancelledEvent(
            Long bookingId,
            Long eventId,
            String customerEmail,
            int quantity,
            String reason,
            Instant occurredAt
    ) {}
}
