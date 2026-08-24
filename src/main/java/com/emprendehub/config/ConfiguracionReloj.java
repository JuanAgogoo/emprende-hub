package com.emprendehub.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj del sistema, en la zona del proyecto.
 *
 * <p>Existe por dos motivos. El primero es H1: la regla de «una visita por
 * perfil, sesión y día» depende de dónde cae la medianoche, y con UTC un negocio
 * de Medellín cambiaría de día a las siete de la tarde.
 *
 * <p>El segundo es que la agregación por semana y por mes se pueda probar. Con
 * {@code LocalDate.now()} escrito dentro del servicio, una prueba de la
 * variación porcentual dependería del día en que se ejecute; con un
 * {@code Clock} inyectado se le pasa uno fijo y el resultado es siempre el mismo.
 */
@Configuration
public class ConfiguracionReloj {

    /** La misma zona que declara {@code application.yml} para Jackson y JPA. */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA);
    }
}
