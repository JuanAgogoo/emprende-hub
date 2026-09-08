package com.emprendehub.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.UsuarioRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Seguridad sobre peticiones HTTP reales, contra el servidor levantado.
 *
 * <p>Existe por un motivo concreto: <strong>MockMvc no ejecuta el reenvío
 * interno a {@code /error}</strong>. Por eso un fallo real —que un 403 acabara
 * convertido en 401 al construir el cuerpo del error— pasó desapercibido a
 * noventa y tres pruebas y solo apareció llamando a la API con curl.
 *
 * <p>Se usa el cliente HTTP del JDK para no depender de nada más.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeguridadHttpRealTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private String tokenCliente;

    @BeforeEach
    void prepararUsuarios() {
        Usuario cliente = new Usuario("María", "maria@test.co", "irrelevante", Rol.CLIENTE);
        Mockito.when(usuarioRepository.findByCorreo("maria@test.co"))
                .thenReturn(Optional.of(cliente));
        tokenCliente = jwtService.generarToken(cliente);
    }

    private HttpResponse<String> pedir(String ruta, String token) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + puerto + ruta))
                .GET();
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("El 403 sobrevive al reenvío a /error y llega con su cuerpo")
    void admin_conTokenDeCliente_devuelve403ConCuerpo() throws Exception {
        HttpResponse<String> respuesta = pedir("/api/v1/admin/cursos", tokenCliente);

        assertEquals(403, respuesta.statusCode());
        assertTrue(respuesta.body().contains("403"),
                "El cuerpo del error debería seguir hablando de 403, no de 401: "
                        + respuesta.body());
    }

    @Test
    @DisplayName("Sin token la respuesta es 401, también sobre HTTP real")
    void admin_sinToken_devuelve401() throws Exception {
        assertEquals(401, pedir("/api/v1/admin/cursos", null).statusCode());
    }

    @Test
    @DisplayName("Las rutas públicas responden 200 sin ninguna cabecera")
    void catalogos_sinToken_devuelve200() throws Exception {
        assertEquals(200, pedir("/api/v1/catalogos/ciudades", null).statusCode());
    }

    @Test
    @DisplayName("El directorio se explora sin sesión, también sobre HTTP real")
    void directorio_sinToken_devuelve200() throws Exception {
        assertEquals(200, pedir("/api/v1/directorio", null).statusCode());
    }

    @Test
    @DisplayName("Un negocio no publicado devuelve 404 sin sesión, y el 404 llega entero")
    void perfilPublico_inexistente_devuelve404ConCuerpo() throws Exception {
        // El 404 de una ruta pública también pasa por el reenvío interno a
        // /error. Si ese reenvío volviera a filtrarse sin token, el visitante
        // recibiría un 401 y sabría menos, no más, de lo que pasó (B6).
        HttpResponse<String> respuesta = pedir("/api/v1/directorio/999999", null);

        assertEquals(404, respuesta.statusCode());
        assertTrue(respuesta.body().contains("404"),
                "El cuerpo debería seguir hablando de 404: " + respuesta.body());
    }
}
