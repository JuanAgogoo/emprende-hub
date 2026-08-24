package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends ControllerTestBase {

    @MockitoBean
    private AuthService authService;

    private static final String REGISTRO_VALIDO = """
            {
              "nombre": "María García",
              "correo": "maria@gmail.com",
              "contrasena": "contrasena123"
            }
            """;

    private AuthResponse respuestaDePrueba() {
        return AuthResponse.de("token-generado", 3600000L,
                "María García", "maria@gmail.com", "CLIENTE");
    }

    @Test
    @DisplayName("POST /registro devuelve 201 con el token")
    void registro_valido_devuelve201() throws Exception {
        when(authService.registrarCliente(any())).thenReturn(respuestaDePrueba());

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-generado"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /registro con contraseña de menos de 8 caracteres devuelve 400")
    void registro_contrasenaCorta_devuelve400() throws Exception {
        String corta = REGISTRO_VALIDO.replace("contrasena123", "1234567");

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corta))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.contrasena").exists());
    }

    @Test
    @DisplayName("POST /registro con un correo mal formado devuelve 400")
    void registro_correoInvalido_devuelve400() throws Exception {
        String malFormado = REGISTRO_VALIDO.replace("maria@gmail.com", "no-es-un-correo");

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malFormado))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());
    }

    @Test
    @DisplayName("POST /registro con un correo ya usado devuelve 400")
    void registro_correoRepetido_devuelve400() throws Exception {
        when(authService.registrarCliente(any()))
                .thenThrow(new ReglaDeNegocioException("Ya existe una cuenta con ese correo"));

        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese correo"));
    }

    @Test
    @DisplayName("POST /login devuelve 200 con el token")
    void login_correcto_devuelve200() throws Exception {
        when(authService.iniciarSesion(any())).thenReturn(respuestaDePrueba());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"maria@gmail.com\",\"contrasena\":\"contrasena123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-generado"));
    }

    @Test
    @DisplayName("POST /login con credenciales incorrectas devuelve 401")
    void login_credencialesMalas_devuelve401() throws Exception {
        when(authService.iniciarSesion(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"maria@gmail.com\",\"contrasena\":\"mal\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    @DisplayName("POST /login de una cuenta suspendida devuelve 403")
    void login_cuentaSuspendida_devuelve403() throws Exception {
        when(authService.iniciarSesion(any()))
                .thenThrow(new DisabledException("La cuenta está suspendida"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"carlos@test.co\",\"contrasena\":\"contrasena123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("La cuenta está suspendida"));
    }
}
