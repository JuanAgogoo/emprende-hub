package com.emprendehub.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Consulta;
import com.emprendehub.repository.ConsultaRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.OpinionRepository;
import java.nio.file.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Siembra de verdad, con una base propia y vacía: el resto de las pruebas la
 * apaga, y un fallo aquí tumba el arranque de cualquier máquina nueva.
 */
@SpringBootTest
class CargaInicialDemoTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws Exception {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("emprendehub.semilla-demo", () -> "true");
        String fotos = Files.createTempDirectory("fotos-semilla").toString();
        registro.add("emprendehub.fotos.directorio", () -> fotos);
    }

    @Autowired
    private NegocioRepository negocios;

    @Autowired
    private ConsultaRepository consultas;

    @Autowired
    private OpinionRepository opiniones;

    @Test
    @DisplayName("La semilla arranca con la base vacía y cada consulta viene de quien calificó (D4)")
    void laSemilla_respetaLaReglaDeLasConsultas() {
        assertEquals(40, negocios.count());

        var todas = consultas.findAll();
        assertTrue(todas.size() > 0);
        for (Consulta consulta : todas) {
            assertTrue(opiniones.existsByNegocioIdAndAutorId(
                            consulta.getNegocio().getId(), consulta.getCliente().getId()),
                    "consulta sin calificación previa: " + consulta.getId());
        }
    }
}
