package com.example.tickets.notification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario de notification: el email es simulado por log,
 * así que verificamos el contenido del log emitido.
 */
class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    private ListAppender<ILoggingEvent> logWatcher;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        serviceLogger = (Logger) LoggerFactory.getLogger(NotificationService.class);
        logWatcher = new ListAppender<>();
        logWatcher.start();
        serviceLogger.addAppender(logWatcher);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logWatcher);
    }

    @Test
    void shouldLogBookingConfirmation() {
        notificationService.sendBookingConfirmation("unit@example.com", 42L, "2 entradas, 60.00 €");

        assertThat(logWatcher.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.INFO);
                    assertThat(event.getFormattedMessage())
                            .contains("unit@example.com")
                            .contains("42")
                            .contains("2 entradas");
                });
    }

    @Test
    void shouldLogBookingCancellation() {
        notificationService.sendBookingCancellation("unit@example.com", 42L, "Cliente no puede asistir");

        assertThat(logWatcher.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("cancelación")
                        .contains("Cliente no puede asistir"));
    }
}
