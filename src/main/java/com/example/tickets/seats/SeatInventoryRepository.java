package com.example.tickets.seats;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeatInventoryRepository extends JpaRepository<SeatInventory, Long> {

    Optional<SeatInventory> findByEventId(Long eventId);
}
