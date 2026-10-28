package com.example.tickets.notification.internal;

import com.example.tickets.booking.BookingEvents.BookingCancelledEvent;
import com.example.tickets.booking.BookingEvents.BookingConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Handler de eventos de {@code booking} para el módulo {@code notification}.
 *
 * <p>Gracias a {@code @ApplicationModuleListener}:
 * <ol>
 *   <li>El email solo se envía si la reserva quedó persistida (AFTER_COMMIT).</li>
 *   <li>El envío es asíncrono y en su propia transacción.</li>
 *   <li>Si el envío falla, la publicación queda incompleta en
 *       {@code event_publication} y se reintenta al arrancar la app.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationHandler {

    private final NotificationService notificationService;

    @ApplicationModuleListener
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        log.info("📬 [Notification] BookingConfirmed para reserva {}", event.bookingId());

        notificationService.sendBookingConfirmation(
                event.customerEmail(),
                event.bookingId(),
                "Has reservado %d entrada(s). Total: %s €".formatted(event.quantity(), event.totalAmount())
        );
    }

    @ApplicationModuleListener
    public void onBookingCancelled(BookingCancelledEvent event) {
        log.info("📬 [Notification] BookingCancelled para reserva {}", event.bookingId());

        notificationService.sendBookingCancellation(
                event.customerEmail(),
                event.bookingId(),
                event.reason()
        );
    }
}
