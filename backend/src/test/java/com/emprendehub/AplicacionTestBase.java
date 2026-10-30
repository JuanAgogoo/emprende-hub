package com.emprendehub;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas que levantan la aplicación entera con {@code @SpringBootTest}:
 * un PostgreSQL real en Docker, en lugar de la base de {@code application.yml}.
 *
 * <p>Sin esto, esas pruebas conectaban con el 5433 y solo pasaban con la base de
 * {@code docker compose} levantada. Ahora {@code ./gradlew check} pasa en un clon
 * limpio con solo tener Docker. {@code @ServiceConnection} hace que Spring tome
 * del contenedor la URL, el usuario y la contraseña.
 *
 * <p>Es el mismo patrón de contenedor único que {@code PostgresTestBase}, pero
 * <strong>no el mismo contenedor</strong>: la aplicación entera ejecuta sus
 * cargas iniciales al arrancar, y las pruebas de repositorio cuentan con las
 * tablas vacías. Compartiendo base, cada una pisaría a la otra.
 */
public abstract class AplicacionTestBase {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
