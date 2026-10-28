package com.example.tickets.seats;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{eventId}")
    public ResponseEntity<SeatAvailability> availability(@PathVariable Long eventId) {
        return seatService.getAvailability(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
