package com.emprendehub.repository;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas de repositorio: un PostgreSQL real en Docker.
 *
 * <p>{@code @AutoConfigureTestDatabase} con {@code NONE} desactiva la
 * sustitución por una base en memoria que Spring haría por defecto, que es justo
 * lo que el enunciado prohíbe.
 *
 * <p>El contenedor sigue el patrón de <em>contenedor único</em>: se arranca en
 * el bloque estático y se comparte durante toda la ejecución. No se usan
 * {@code @Testcontainers} ni {@code @Container} a propósito, porque esa
 * extensión detiene el contenedor al terminar cada clase de prueba y la
 * siguiente que heredara de aquí lo encontraría parado. Ryuk se encarga de
 * retirarlo cuando acaba la JVM.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class PostgresTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
