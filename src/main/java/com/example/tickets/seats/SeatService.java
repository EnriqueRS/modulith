package com.example.tickets.seats;

import com.example.tickets.events.Event;
import com.example.tickets.events.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio del módulo {@code seats}: gestión del aforo.
 *
 * <p>Dependencia síncrona: {@code seats -> events} (solo la API pública
 * {@link EventService}, para conocer el aforo al inicializar el inventario).
 * La comunicación con {@code booking} es exclusivamente por eventos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatInventoryRepository seatInventoryRepository;
    private final EventService eventService;

    /**
     * Reserva {@code quantity} plazas para el evento.
     *
     * @throws InsufficientSeatsException si no hay aforo suficiente. La excepción
     *         NO se captura en los handlers: propagarla es lo que deja la
     *         publicación del evento incompleta y activa el reintento de Modulith.
     */
    @Transactional
    public SeatInventory reserveSeats(Long eventId, int quantity) {
        SeatInventory inventory = getOrCreateInventory(eventId);

        if (inventory.getAvailableSeats() < quantity) {
            throw new InsufficientSeatsException(eventId, quantity, inventory.getAvailableSeats());
        }

        inventory.setReservedSeats(inventory.getReservedSeats() + quantity);
        SeatInventory saved = seatInventoryRepository.save(inventory);
        log.info("Reservadas {} plazas para el evento {} (disponibles: {})",
                quantity, eventId, saved.getAvailableSeats());
        return saved;
    }

    /**
     * Libera {@code quantity} plazas (por una cancelación).
     */
    @Transactional
    public SeatInventory releaseSeats(Long eventId, int quantity) {
        return seatInventoryRepository.findByEventId(eventId)
                .map(inventory -> {
                    inventory.setReservedSeats(Math.max(0, inventory.getReservedSeats() - quantity));
                    SeatInventory saved = seatInventoryRepository.save(inventory);
                    log.info("Liberadas {} plazas del evento {} (disponibles: {})",
                            quantity, eventId, saved.getAvailableSeats());
                    return saved;
                })
                .orElseGet(() -> {
                    log.warn("No existe inventario para el evento {}; nada que liberar", eventId);
                    return null;
                });
    }

    /**
     * Aforo disponible del evento (inicializa el inventario si no existe).
     */
    @Transactional
    public SeatInventory getAvailability(Long eventId) {
        return getOrCreateInventory(eventId);
    }

    private SeatInventory getOrCreateInventory(Long eventId) {
        return seatInventoryRepository.findByEventId(eventId)
                .orElseGet(() -> {
                    Event event = eventService.getEventById(eventId);
                    log.info("Inicializando aforo del evento {} con {} plazas", eventId, event.getTotalCapacity());
                    return seatInventoryRepository.save(SeatInventory.builder()
                            .eventId(eventId)
                            .totalSeats(event.getTotalCapacity())
                            .reservedSeats(0)
                            .build());
                });
    }
}
