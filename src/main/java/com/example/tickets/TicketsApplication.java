package com.example.tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación de reserva de entradas para eventos, estructurada como Spring Modulith.
 *
 * <p>Cada paquete directo bajo {@code com.example.tickets} es un módulo:
 * {@code events} (catálogo), {@code booking} (reservas), {@code seats} (aforo)
 * y {@code notification} (emails). Spring Modulith lo detecta automáticamente.
 */
@SpringBootApplication
public class TicketsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketsApplication.class, args);
    }
}
