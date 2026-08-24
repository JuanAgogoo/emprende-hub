package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.ConsultaResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.ConsultaService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ConsultaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsultaControllerTest extends ControllerTestBase {

    @MockitoBean
    private ConsultaService consultaService;

    private ConsultaResponse consultaDePrueba(boolean leida) {
        return new ConsultaResponse(9L, "Reserva para 4 personas",
                "¿Tienen mesa el sábado a las 8?", "Carlos Rueda", "carlos@gmail.com",
                leida, Instant.parse("2026-08-20T10:00:00Z"),
                leida ? Instant.parse("2026-08-20T12:00:00Z") : null);
    }

    private String cuerpo(String asunto, String mensaje) {
        return """
                { "asunto": "%s", "mensaje": "%s" }
                """.formatted(asunto, mensaje);
    }

    // ---------- Enviar ----------

    @Test
    @DisplayName("POST devuelve 201 con la consulta sin leer")
    void enviar_devuelve201() throws Exception {
        when(consultaService.enviar(any(), eq(7L), any())).thenReturn(consultaDePrueba(false));

        mockMvc.perform(post("/api/v1/negocios/7/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Reserva para 4 personas", "¿Tienen mesa el sábado?")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leida").value(false))
                .andExpect(jsonPath("$.fechaLectura").doesNotExist());
    }

    @Test
    @DisplayName("POST sin asunto devuelve 400 con una clave por campo inválido")
    void enviar_sinAsunto_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/negocios/7/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("", "¿Tienen mesa el sábado?")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.asunto").exists());
    }

    @Test
    @DisplayName("POST con un mensaje de más de 500 caracteres devuelve 400")
    void enviar_mensajeLargo_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/negocios/7/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Reserva", "a".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("POST a un negocio sin publicar devuelve 404 (B6)")
    void enviar_negocioNoVisible_devuelve404() throws Exception {
        when(consultaService.enviar(any(), eq(7L), any()))
                .thenThrow(new ResourceNotFoundException("Negocio", 7L));

        mockMvc.perform(post("/api/v1/negocios/7/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Reserva", "¿Tienen mesa?")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST al buzón propio devuelve 400 con su motivo")
    void enviar_aSuPropioBuzon_devuelve400() throws Exception {
        when(consultaService.enviar(any(), eq(7L), any())).thenThrow(
                new ReglaDeNegocioException("No tiene sentido escribirte a tu propio buzón"));

        mockMvc.perform(post("/api/v1/negocios/7/consultas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Hola", "Me escribo a mí misma")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "No tiene sentido escribirte a tu propio buzón"));
    }

    // ---------- Leer el buzón ----------

    @Test
    @DisplayName("GET del buzón enseña el correo del cliente para poder responder (D2)")
    void buzon_llevaElCorreo() throws Exception {
        when(consultaService.buzon(any(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(consultaDePrueba(false))));

        mockMvc.perform(get("/api/v1/negocios/mio/consultas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].correoCliente").value("carlos@gmail.com"))
                .andExpect(jsonPath("$.content[0].nombreCliente").value("Carlos Rueda"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET con leida=false pide solo las pendientes")
    void buzon_soloNoLeidas_trasladaElFiltro() throws Exception {
        when(consultaService.buzon(any(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/negocios/mio/consultas").param("leida", "false"))
                .andExpect(status().isOk());

        verify(consultaService).buzon(any(), eq(false), any());
    }

    @Test
    @DisplayName("GET con un valor de leida que no es booleano devuelve 400")
    void buzon_filtroInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/negocios/mio/consultas").param("leida", "quizas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.leida").exists());
    }

    // ---------- Marcar (D3) ----------

    @Test
    @DisplayName("PATCH marca la consulta como leída y sella la fecha")
    void marcarLectura_leida_devuelve200() throws Exception {
        when(consultaService.marcarLectura(any(), eq(9L), eq(true)))
                .thenReturn(consultaDePrueba(true));

        mockMvc.perform(patch("/api/v1/negocios/mio/consultas/9/lectura")
                        .param("leida", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true))
                .andExpect(jsonPath("$.fechaLectura").exists());
    }

    @Test
    @DisplayName("PATCH con leida=false la devuelve al montón")
    void marcarLectura_noLeida_devuelve200() throws Exception {
        when(consultaService.marcarLectura(any(), eq(9L), eq(false)))
                .thenReturn(consultaDePrueba(false));

        mockMvc.perform(patch("/api/v1/negocios/mio/consultas/9/lectura")
                        .param("leida", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaLectura").doesNotExist());
    }

    @Test
    @DisplayName("PATCH sobre una consulta de otro buzón devuelve 404")
    void marcarLectura_consultaAjena_devuelve404() throws Exception {
        when(consultaService.marcarLectura(any(), eq(99L), eq(true)))
                .thenThrow(new ResourceNotFoundException("Consulta", 99L));

        mockMvc.perform(patch("/api/v1/negocios/mio/consultas/99/lectura")
                        .param("leida", "true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
