package com.example.tickets.events;

import com.example.tickets.events.internal.Event;
import com.example.tickets.events.internal.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio del módulo {@code events}: la API pública que consumen
 * {@code booking} y {@code seats}.
 *
 * <p>Nunca devuelve la entidad {@link Event} (interna del módulo): expone
 * {@link EventInfo}. Así otros módulos no pueden acoplarse a la persistencia.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;

    @Transactional
    public EventInfo createEvent(CreateEventRequest request) {
        Event saved = eventRepository.save(Event.builder()
                .name(request.name())
                .venue(request.venue())
                .eventDate(request.eventDate())
                .price(request.price())
                .totalCapacity(request.totalCapacity())
                .build());

        log.info("Evento creado: {} ({}) con aforo {}", saved.getName(), saved.getId(), saved.getTotalCapacity());
        return toInfo(saved);
    }

    @Transactional(readOnly = true)
    public List<EventInfo> getAllEvents() {
        return eventRepository.findAll().stream().map(this::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public EventInfo getEventById(Long id) {
        return eventRepository.findById(id)
                .map(this::toInfo)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado con id: " + id));
    }

    private EventInfo toInfo(Event event) {
        return new EventInfo(
                event.getId(),
                event.getName(),
                event.getVenue(),
                event.getEventDate(),
                event.getPrice(),
                event.getTotalCapacity());
    }
}
