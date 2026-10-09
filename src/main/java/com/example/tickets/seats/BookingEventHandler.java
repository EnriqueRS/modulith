package com.example.tickets.seats;

import com.example.tickets.booking.BookingEvents.BookingCancelledEvent;
import com.example.tickets.booking.BookingEvents.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Handler de eventos de {@code booking} para el módulo {@code seats}.
 *
 * <p>{@link ApplicationModuleListener} equivale a
 * {@code @TransactionalEventListener(AFTER_COMMIT) + @Async + @Transactional(REQUIRES_NEW)}:
 * <ul>
 *   <li>Se ejecuta SOLO si la transacción de la reserva hizo commit.</li>
 *   <li>Corre de forma asíncrona, en su propia transacción.</li>
 *   <li>La publicación queda registrada en la tabla {@code event_publication}:
 *       si este handler falla, queda INCOMPLETA (visible en la tabla y gestionable
 *       vía {@code IncompleteEventPublications}) y se reintenta al arrancar
 *       ({@code spring.modulith.events.republish-outstanding-events-on-restart=true}).</li>
 * </ul>
 *
 * <p>Por eso aquí NO capturamos excepciones: dejarlas propagar es lo que
 * activa el mecanismo de reintento de Spring Modulith.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventHandler {

    private final SeatService seatService;

    @ApplicationModuleListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("🎟️ [Seats] BookingConfirmed para reserva {}: reservando {} plazas del evento {}",
                event.bookingId(), event.quantity(), event.eventId());

        seatService.reserveSeats(event.eventId(), event.quantity());

        log.info("✅ [Seats] Aforo reservado para la reserva {}", event.bookingId());
    }

    @ApplicationModuleListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("❌ [Seats] BookingCancelled para reserva {}: liberando {} plazas del evento {}",
                event.bookingId(), event.quantity(), event.eventId());

        seatService.releaseSeats(event.eventId(), event.quantity());

        log.info("✅ [Seats] Aforo liberado para la reserva {}", event.bookingId());
    }
}
