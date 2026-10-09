package com.example.tickets.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio del módulo {@code events}: catálogo de eventos.
 *
 * <p>Es el único punto de entrada que otros módulos usan de forma síncrona
 * ({@code booking} y {@code seats}). Al estar en el paquete raíz del módulo,
 * forma parte de su API pública según las reglas de Spring Modulith.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;

    @Transactional
    public Event createEvent(Event event) {
        Event saved = eventRepository.save(event);
        log.info("Evento creado: {} ({}) con aforo {}", saved.getName(), saved.getId(), saved.getTotalCapacity());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado con id: " + id));
    }
}
