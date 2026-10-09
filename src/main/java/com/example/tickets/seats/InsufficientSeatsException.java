package com.example.tickets.seats;

public class InsufficientSeatsException extends RuntimeException {

    public InsufficientSeatsException(Long eventId, int requested, int available) {
        super("Aforo insuficiente para el evento %d: solicitadas %d, disponibles %d"
                .formatted(eventId, requested, available));
    }
}
