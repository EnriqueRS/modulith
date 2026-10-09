package com.example.tickets.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio del módulo {@code notification}: envío de emails (simulado por consola).
 *
 * <p>No conoce a ningún otro módulo salvo el contrato de eventos de
 * {@code booking}: es un consumidor puro.
 */
@Service
@Slf4j
public class NotificationService {

    public void sendBookingConfirmation(String customerEmail, Long bookingId, String details) {
        log.info("""
                📬 Enviando email de confirmación a {} por la reserva {}
                {}
                """, customerEmail, bookingId, details);
    }

    public void sendBookingCancellation(String customerEmail, Long bookingId, String reason) {
        log.info("""
                📬 Enviando email de cancelación a {} por la reserva {}
                Motivo: {}
                """, customerEmail, bookingId, reason);
    }
}
