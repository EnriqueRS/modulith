package com.example.tickets.events;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public EventInfo create(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping
    public List<EventInfo> getAll() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public EventInfo getById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }
}
