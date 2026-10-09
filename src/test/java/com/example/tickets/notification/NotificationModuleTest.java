package com.example.tickets.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

/**
 * Test de MÓDULO para notification con Spring Modulith.
 *
 * <p>STANDALONE: notification es un consumidor puro de eventos; no depende
 * síncronamente de ningún módulo, así que no necesita ningún mock.
 */
@ApplicationModuleTest
class NotificationModuleTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void shouldSendBookingConfirmation() {
        // El envío es simulado por log: comprobamos que la llamada no falla.
        // El contenido del log se verifica en NotificationServiceTest.
        notificationService.sendBookingConfirmation("module@example.com", 7L, "2 entradas");
    }
}
