package com.example.tickets.seats;

import com.example.tickets.events.EventInfo;
import com.example.tickets.events.EventService;
import com.example.tickets.seats.internal.InsufficientSeatsException;
import com.example.tickets.seats.internal.SeatInventory;
import com.example.tickets.seats.internal.SeatInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Servicio del módulo {@code seats}: API pública de consulta de aforo.
 *
 * <p>Dependencia síncrona: {@code seats -> events} (solo {@link EventService},
 * para conocer la capacidad al inicializar el inventario). Devuelve
 * {@link SeatAvailability}, nunca la entidad interna.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatInventoryRepository seatInventoryRepository;
    private final EventService eventService;

    /**
     * Reserva {@code quantity} plazas. Uso interno del módulo (listener).
     *
     * @throws InsufficientSeatsException si no hay aforo suficiente. La excepción
     *         NO se captura en los handlers: propagarla es lo que deja la
     *         publicación incompleta y activa el reintento de Modulith.
     */
    @Transactional
    public SeatAvailability reserveSeats(Long eventId, int quantity) {
        SeatInventory inventory = getOrCreateInventory(eventId);

        if (inventory.getAvailableSeats() < quantity) {
            throw new InsufficientSeatsException(eventId, quantity, inventory.getAvailableSeats());
        }

        inventory.setReservedSeats(inventory.getReservedSeats() + quantity);
        SeatInventory saved = seatInventoryRepository.save(inventory);
        log.info("Reservadas {} plazas para el evento {} (disponibles: {})",
                quantity, eventId, saved.getAvailableSeats());
        return toAvailability(saved);
    }

    /**
     * Libera {@code quantity} plazas (por una cancelación).
     */
    @Transactional
    public Optional<SeatAvailability> releaseSeats(Long eventId, int quantity) {
        return seatInventoryRepository.findByEventId(eventId)
                .map(inventory -> {
                    inventory.setReservedSeats(Math.max(0, inventory.getReservedSeats() - quantity));
                    SeatInventory saved = seatInventoryRepository.save(inventory);
                    log.info("Liberadas {} plazas del evento {} (disponibles: {})",
                            quantity, eventId, saved.getAvailableSeats());
                    return toAvailability(saved);
                })
                .or(() -> {
                    log.warn("No existe inventario para el evento {}; nada que liberar", eventId);
                    return Optional.empty();
                });
    }

    /**
     * Aforo actual del evento, en solo lectura. Vacío si aún no hay inventario.
     */
    @Transactional(readOnly = true)
    public Optional<SeatAvailability> getAvailability(Long eventId) {
        return seatInventoryRepository.findByEventId(eventId).map(this::toAvailability);
    }

    private SeatInventory getOrCreateInventory(Long eventId) {
        return seatInventoryRepository.findByEventId(eventId)
                .orElseGet(() -> {
                    EventInfo event = eventService.getEventById(eventId);
                    log.info("Inicializando aforo del evento {} con {} plazas", eventId, event.totalCapacity());
                    return seatInventoryRepository.save(SeatInventory.builder()
                            .eventId(eventId)
                            .totalSeats(event.totalCapacity())
                            .reservedSeats(0)
                            .build());
                });
    }

    private SeatAvailability toAvailability(SeatInventory inventory) {
        return new SeatAvailability(
                inventory.getEventId(),
                inventory.getTotalSeats(),
                inventory.getReservedSeats(),
                inventory.getAvailableSeats());
    }
}
