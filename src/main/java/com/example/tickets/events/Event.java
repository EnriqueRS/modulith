package com.example.tickets.events;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento del catálogo (concierto, obra, festival...).
 *
 * <p>Esta clase está en el paquete raíz del módulo {@code events}, por lo que
 * Spring Modulith la considera parte de su API pública: otros módulos pueden
 * referenciarla. Si se moviera a un subpaquete, pasaría a ser interna y
 * {@code ModularityTests#verify()} fallaría la build al usarla desde fuera.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String venue;

    private Instant eventDate;

    /** Precio por entrada. */
    private BigDecimal price;

    /** Aforo total del evento. */
    private int totalCapacity;
}
