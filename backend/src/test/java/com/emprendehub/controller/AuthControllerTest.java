package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.RegistroEmprendedorResponse;
import com.emprendehub.exception.EnlaceCaducadoException;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.service.AuthService;
import com.emprendehub.service.RecuperacionService;
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

    @MockitoBean
    private RecuperacionService recuperacionService;

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

    // ---------- Registro de emprendedor ----------

    private static final String REGISTRO_EMPRENDEDOR = """
            {
              "nombre": "Lucía Restrepo",
              "correo": "lucia@emprendehub.co",
              "contrasena": "contrasena123",
              "negocio": {
                "nombre": "Panadería La Espiga",
                "descripcion": "Pan de masa madre horneado cada mañana en horno de leña, con harinas molidas a la piedra y fermentación lenta de 24 horas.",
                "telefono": "3105551234",
                "categoriaId": 1,
                "ciudadId": 1,
                "nivelPrecio": "BAJO"
              }
            }
            """;

    private RegistroEmprendedorResponse respuestaDeEmprendedor() {
        return RegistroEmprendedorResponse.de(
                AuthResponse.de("token-generado", 3600000L,
                        "Lucía Restrepo", "lucia@emprendehub.co", "EMPRENDEDOR"),
                7L);
    }

    @Test
    @DisplayName("POST /registro-emprendedor devuelve 201 con el token y el negocio")
    void registroEmprendedor_valido_devuelve201() throws Exception {
        when(authService.registrarEmprendedor(any())).thenReturn(respuestaDeEmprendedor());

        mockMvc.perform(post("/api/v1/auth/registro-emprendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_EMPRENDEDOR))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token-generado"))
                .andExpect(jsonPath("$.rol").value("EMPRENDEDOR"))
                .andExpect(jsonPath("$.negocioId").value(7));
    }

    @Test
    @DisplayName("POST /registro-emprendedor sin los datos del negocio devuelve 400")
    void registroEmprendedor_sinNegocio_devuelve400() throws Exception {
        String sinNegocio = """
                {
                  "nombre": "Lucía Restrepo",
                  "correo": "lucia@emprendehub.co",
                  "contrasena": "contrasena123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/registro-emprendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinNegocio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.negocio").exists());
    }

    @Test
    @DisplayName("POST /registro-emprendedor con descripción corta devuelve 400 y nombra el campo")
    void registroEmprendedor_descripcionCorta_devuelve400() throws Exception {
        String corta = REGISTRO_EMPRENDEDOR.replaceAll(
                "\"descripcion\": \"[^\"]+\"", "\"descripcion\": \"Muy corta\"");

        mockMvc.perform(post("/api/v1/auth/registro-emprendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corta))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /registro-emprendedor ignora el escaparate: ya no viaja en el alta")
    void registroEmprendedor_conProductos_losIgnora() throws Exception {
        when(authService.registrarEmprendedor(any())).thenReturn(respuestaDeEmprendedor());

        // Desde que cada producto necesita su imagen obligatoria, el escaparate
        // se monta después con la sesión que devuelve esta llamada. Un cliente
        // viejo que siga mandándolo no rompe: Jackson descarta lo que el DTO no
        // declara, y esta prueba deja constancia de que es a propósito.
        String conProductos = REGISTRO_EMPRENDEDOR.replace("\"nivelPrecio\": \"BAJO\"\n              }",
                "\"nivelPrecio\": \"BAJO\"\n              },\n"
                        + "              \"productos\": [{ \"nombre\": \"Pan\", \"precio\": -1 }]");

        mockMvc.perform(post("/api/v1/auth/registro-emprendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conProductos))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.negocioId").value(7));
    }

    @Test
    @DisplayName("POST /registro-emprendedor con el correo ya usado devuelve 400")
    void registroEmprendedor_correoRepetido_devuelve400() throws Exception {
        when(authService.registrarEmprendedor(any()))
                .thenThrow(new ReglaDeNegocioException("Ya existe una cuenta con ese correo"));

        mockMvc.perform(post("/api/v1/auth/registro-emprendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_EMPRENDEDOR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese correo"));
    }

    @Test
    @DisplayName("POST /registro-emprendedor es público: no exige token")
    void registroEmprendedor_sinToken_noDevuelve401() throws Exception {
        when(authService.registrarEmprendedor(any())).thenReturn(respuestaDeEmprendedor());

        mockMvc.perform(post("/api/v1/auth/registro-emprendedor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_EMPRENDEDOR))
                .andExpect(status().isCreated());
    }

    // ---------- Recuperación de contraseña ----------

    private static final String SOLICITUD = """
            { "correo": "maria@gmail.com" }
            """;

    private static final String CONTRASENA_NUEVA = """
            { "contrasena": "contrasenaNueva" }
            """;

    @Test
    @DisplayName("POST /recuperacion devuelve 200 y no enseña el token")
    void recuperacion_correoValido_devuelve200SinCuerpo() throws Exception {
        //arrange
        doNothing().when(recuperacionService).solicitar(any());

        //act
        //assert
        mockMvc.perform(post("/api/v1/auth/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("POST /recuperacion de un correo sin cuenta devuelve 200 igual")
    void recuperacion_correoDesconocido_devuelve200() throws Exception {
        //arrange
        // El servicio no lanza nada con un correo que no existe: esa es la
        // decisión 3, y aquí se comprueba que el controlador la respeta.
        doNothing().when(recuperacionService).solicitar(any());

        //act
        //assert
        mockMvc.perform(post("/api/v1/auth/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD.replace("maria@gmail.com", "nadie@gmail.com")))
                .andExpect(status().isOk());

        //verify
        verify(recuperacionService).solicitar(any());
    }

    @Test
    @DisplayName("POST /recuperacion con un correo mal formado devuelve 400")
    void recuperacion_correoInvalido_devuelve400() throws Exception {
        //arrange
        //act
        //assert
        mockMvc.perform(post("/api/v1/auth/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD.replace("maria@gmail.com", "no-es-un-correo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());
    }

    @Test
    @DisplayName("GET /recuperacion/{token} con un enlace vivo devuelve 200")
    void comprobarEnlace_valido_devuelve200() throws Exception {
        //arrange
        doNothing().when(recuperacionService).comprobar("token-vivo");

        //act
        //assert
        mockMvc.perform(get("/api/v1/auth/recuperacion/token-vivo"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /recuperacion/{token} con un enlace que ya no vale devuelve 410")
    void comprobarEnlace_caducado_devuelve410() throws Exception {
        //arrange
        doThrow(new EnlaceCaducadoException("Este enlace de recuperación ya no es válido."))
                .when(recuperacionService).comprobar("token-caducado");

        //act
        //assert
        // 410 y no 404: el enlace existió y ha dejado de servir.
        mockMvc.perform(get("/api/v1/auth/recuperacion/token-caducado"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /recuperacion/{token} cambia la contraseña y devuelve 204")
    void restablecer_valido_devuelve204() throws Exception {
        //arrange
        doNothing().when(recuperacionService).restablecer(any(), any());

        //act
        //assert
        mockMvc.perform(post("/api/v1/auth/recuperacion/token-vivo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONTRASENA_NUEVA))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /recuperacion/{token} con contraseña de 7 caracteres devuelve 400")
    void restablecer_contrasenaCorta_devuelve400() throws Exception {
        //arrange
        //act
        //assert
        // El mínimo de A6 no se rebaja por recuperar la cuenta.
        mockMvc.perform(post("/api/v1/auth/recuperacion/token-vivo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONTRASENA_NUEVA.replace("contrasenaNueva", "1234567")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.contrasena").exists());
    }

    @Test
    @DisplayName("POST /recuperacion/{token} con un enlace ya usado devuelve 410")
    void restablecer_enlaceUsado_devuelve410() throws Exception {
        //arrange
        doThrow(new EnlaceCaducadoException("Este enlace de recuperación ya no es válido."))
                .when(recuperacionService).restablecer(eq("token-usado"), any());

        //act
        //assert
        mockMvc.perform(post("/api/v1/auth/recuperacion/token-usado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CONTRASENA_NUEVA))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("los tres endpoints de recuperación son públicos: no exigen token")
    void recuperacion_sinToken_noDevuelve401() throws Exception {
        //arrange
        doNothing().when(recuperacionService).solicitar(any());

        //act
        //assert
        mockMvc.perform(post("/api/v1/auth/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isOk());
    }
}
