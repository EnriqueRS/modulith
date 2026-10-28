package com.example.tickets.seats.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado del aforo de un evento.
 *
 * <p>Se inicializa perezosamente (con el aforo del evento) la primera vez que
 * se reserva una plaza.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long eventId;

    private int totalSeats;

    private int reservedSeats;

    public int getAvailableSeats() {
        return totalSeats - reservedSeats;
    }
}
