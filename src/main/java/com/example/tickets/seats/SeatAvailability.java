package com.example.tickets.seats;

/**
 * Vista pública del aforo de un evento. La entidad {@code SeatInventory}
 * vive en {@code seats.internal}.
 */
public record SeatAvailability(
        Long eventId,
        int totalSeats,
        int reservedSeats,
        int availableSeats
) {
}
