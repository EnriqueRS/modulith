package com.example.tickets.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Grupos de Swagger UI que replican la estructura de módulos.
 *
 * <p>La documentación REST se ordena igual que las carpetas: un grupo por
 * módulo, cada uno con los endpoints de su paquete. Nota didáctica:
 * {@code notification} no define grupo con rutas propias — es consumidor
 * puro de eventos y no expone REST.
 */
@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi eventsGroup() {
        return GroupedOpenApi.builder()
                .group("01-events")
                .pathsToMatch("/api/events/**")
                .build();
    }

    @Bean
    public GroupedOpenApi bookingGroup() {
        return GroupedOpenApi.builder()
                .group("02-booking")
                .pathsToMatch("/api/bookings/**")
                .build();
    }

    @Bean
    public GroupedOpenApi seatsGroup() {
        return GroupedOpenApi.builder()
                .group("03-seats")
                .pathsToMatch("/api/seats/**")
                .build();
    }
}
