package com.emprendehub.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica la matriz de acceso de {@code docs/arquitectura.md} contra la cadena
 * de filtros completa y con tokens de verdad.
 *
 * <p>Es una prueba de integración a propósito: la autorización nace de cómo
 * encajan la configuración, el filtro y el servicio de tokens, y probar esas
 * piezas por separado no demostraría que el conjunto protege lo que debe.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadAccesoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    private String tokenAdmin;
    private String tokenCliente;

    @BeforeEach
    void prepararUsuarios() {
        Usuario admin = new Usuario("Admin", "admin@test.co", "irrelevante", Rol.ADMIN);
        Usuario cliente = new Usuario("María", "maria@test.co", "irrelevante", Rol.CLIENTE);

        org.mockito.Mockito.when(usuarioRepository.findByCorreo("admin@test.co"))
                .thenReturn(Optional.of(admin));
        org.mockito.Mockito.when(usuarioRepository.findByCorreo("maria@test.co"))
                .thenReturn(Optional.of(cliente));

        tokenAdmin = jwtService.generarToken(admin);
        tokenCliente = jwtService.generarToken(cliente);
    }

    // ---------- Rutas públicas ----------

    @Test
    @DisplayName("Los catálogos se consultan sin ninguna sesión")
    void catalogos_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/categorias-negocio"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El catálogo de cursos se consulta sin ninguna sesión")
    void cursos_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/cursos")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("El directorio se explora sin registrarse, como promete la portada")
    void directorio_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/directorio")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Los destacados de la portada también son públicos")
    void destacados_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/directorio/destacados")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un perfil que no existe devuelve 404 sin sesión, no 401")
    void perfilPublico_sinToken_devuelve404() throws Exception {
        // Importa la distinción: si la ruta no fuera pública, la respuesta sería
        // 401 y el visitante no sabría si el negocio existe o si le falta entrar.
        mockMvc.perform(get("/api/v1/directorio/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Las cifras de la portada se leen sin sesión")
    void estadisticas_sinToken_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/estadisticas/portada")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("El registro es público")
    void registro_sinToken_noDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Rutas de administración ----------

    @Test
    @DisplayName("La gestión sin token devuelve 401, no 403: falta autenticarse")
    void admin_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cursos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La gestión con un token de CLIENTE devuelve 403: falta permiso")
    void admin_conTokenDeCliente_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cursos")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La gestión con un token de ADMIN sí entra")
    void admin_conTokenDeAdmin_devuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cursos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un token corrupto no autentica: se responde 401")
    void admin_conTokenCorrupto_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cursos")
                        .header("Authorization", "Bearer esto.no.es.un.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una cabecera sin el prefijo Bearer se ignora")
    void admin_sinPrefijoBearer_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cursos")
                        .header("Authorization", tokenAdmin))
                .andExpect(status().isUnauthorized());
    }
}
