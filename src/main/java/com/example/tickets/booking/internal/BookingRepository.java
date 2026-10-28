package com.example.tickets.booking.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByCustomerEmail(String customerEmail);
}
