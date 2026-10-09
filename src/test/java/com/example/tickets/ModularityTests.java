package com.example.tickets;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * ⭐ EL CORAZÓN DE SPRING MODULITH: verificación y documentación de la arquitectura.
 *
 * <p>{@code verify()} (basado en ArchUnit) comprueba automáticamente que:
 * <ul>
 *   <li>Ningún módulo accede a código interno (subpaquetes no expuestos) de otro.</li>
 *   <li>No existen dependencias circulares entre módulos.</li>
 *   <li>Los eventos entre módulos solo usan los tipos publicados.</li>
 * </ul>
 * Si alguien inyecta, por ejemplo, {@code EventRepository} (interno de events)
 * desde {@code booking}, ESTE TEST FALLA LA BUILD.
 *
 * <p>{@link Documenter} genera la documentación desde el código real:
 * PlantUML (global y por módulo) y modelado C4 en {@code target/spring-modulith-docs}.
 * La documentación ya no puede mentir.
 *
 * <p>Ejecutar con: {@code mvn test -Dtest=ModularityTests}
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(TicketsApplication.class);

    @Test
    void verifiesModularStructure() {
        // Imprime la estructura detectada (módulos, API expuesta, dependencias)
        System.out.println(modules.toString());
        modules.verify();
    }

    @Test
    void writesArchitectureDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()           // diagrama global
                .writeIndividualModulesAsPlantUml() // un diagrama por módulo
                .writeAggregatingDocument();        // documento C4 agregado
    }
}
