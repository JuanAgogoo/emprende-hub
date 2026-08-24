package com.emprendehub.repository;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de las pruebas de repositorio.
 *
 * <p>Levanta un PostgreSQL real en Docker. {@code @AutoConfigureTestDatabase}
 * con {@code NONE} desactiva la sustitución automática por una base en memoria
 * que Spring haría por defecto, que es justo lo que el enunciado prohíbe.
 *
 * <p>El contenedor es estático: se comparte entre todas las clases de prueba en
 * vez de arrancar uno por clase.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
abstract class PostgresTestBase {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");
}
