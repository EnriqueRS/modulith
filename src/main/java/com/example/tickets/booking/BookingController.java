package com.example.tickets.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public Booking create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{id}")
    public Booking getById(@PathVariable Long id) {
        return bookingService.getBookingById(id);
    }

    @GetMapping
    public List<Booking> byCustomer(@RequestParam(required = false) String customerEmail) {
        return (customerEmail == null || customerEmail.isBlank())
                ? bookingService.getAllBookings()
                : bookingService.getBookingsByCustomer(customerEmail);
    }

    @PostMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id, @RequestParam(defaultValue = "Sin motivo") String reason) {
        return bookingService.cancelBooking(id, reason);
    }
}
