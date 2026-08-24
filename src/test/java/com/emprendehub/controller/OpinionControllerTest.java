package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.OpinionResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.OpinionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(OpinionController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpinionControllerTest extends ControllerTestBase {

    @MockitoBean
    private OpinionService opinionService;

    private static final String RUTA = "/api/v1/negocios/7/opiniones";

    private OpinionResponse opinionDePrueba() {
        return new OpinionResponse(11L, "Carlos Rueda", 5, "La mejor de Medellín",
                Instant.parse("2026-08-20T10:00:00Z"), false);
    }

    private String cuerpo(int calificacion, String comentario) {
        return """
                { "calificacion": %d, "comentario": "%s" }
                """.formatted(calificacion, comentario);
    }

    @Test
    @DisplayName("GET devuelve la página de opiniones del negocio")
    void listar_devuelve200() throws Exception {
        when(opinionService.listarDeNegocio(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(opinionDePrueba())));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].autor").value("Carlos Rueda"))
                .andExpect(jsonPath("$.content[0].calificacion").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("La opinión publicada no expone el correo de quien la escribió")
    void listar_noExponeElCorreo() throws Exception {
        when(opinionService.listarDeNegocio(eq(7L), any()))
                .thenReturn(new PageImpl<>(List.of(opinionDePrueba())));

        mockMvc.perform(get(RUTA))
                .andExpect(jsonPath("$.content[0].correo").doesNotExist())
                .andExpect(jsonPath("$.content[0].autorId").doesNotExist());
    }

    @Test
    @DisplayName("GET sobre un negocio sin publicar devuelve 404 (B6)")
    void listar_negocioNoVisible_devuelve404() throws Exception {
        when(opinionService.listarDeNegocio(eq(7L), any()))
                .thenThrow(new ResourceNotFoundException("Negocio", 7L));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST devuelve 201 con la opinión ya publicada (C4)")
    void crear_devuelve201() throws Exception {
        when(opinionService.crear(any(), eq(7L), any())).thenReturn(opinionDePrueba());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(5, "La mejor de Medellín")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.editada").value(false));
    }

    @Test
    @DisplayName("POST con una calificación fuera de 1 a 5 devuelve 400")
    void crear_calificacionInvalida_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(6, "Excelente")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.calificacion").exists());
    }

    @Test
    @DisplayName("POST con un comentario de más de 300 caracteres devuelve 400")
    void crear_comentarioLargo_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(5, "a".repeat(301))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.comentario").exists());
    }

    @Test
    @DisplayName("POST sobre el negocio propio devuelve 400 con su motivo (A4)")
    void crear_sobreElPropio_devuelve400() throws Exception {
        when(opinionService.crear(any(), eq(7L), any())).thenThrow(
                new ReglaDeNegocioException("No puedes opinar sobre tu propio negocio"));

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(5, "El mío es el mejor")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "No puedes opinar sobre tu propio negocio"));
    }

    @Test
    @DisplayName("PUT /mia edita la opinión propia")
    void actualizar_devuelve200() throws Exception {
        when(opinionService.actualizar(any(), eq(7L), any())).thenReturn(
                new OpinionResponse(11L, "Carlos Rueda", 2, "Cambió a peor",
                        Instant.parse("2026-08-20T10:00:00Z"), true));

        mockMvc.perform(put(RUTA + "/mia").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(2, "Cambió a peor")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editada").value(true));
    }

    @Test
    @DisplayName("GET /mia de quien no ha opinado devuelve 404")
    void obtenerLaMia_sinOpinion_devuelve404() throws Exception {
        when(opinionService.obtenerLaMia(any(), eq(7L))).thenThrow(
                new ResourceNotFoundException("Opinión propia sobre el negocio", 7L));

        mockMvc.perform(get(RUTA + "/mia")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /mia devuelve 204 y no responde cuerpo")
    void eliminar_devuelve204() throws Exception {
        mockMvc.perform(delete(RUTA + "/mia")).andExpect(status().isNoContent());

        verify(opinionService).eliminar(any(), eq(7L));
    }

    @Test
    @DisplayName("DELETE /mia de quien no ha opinado devuelve 404")
    void eliminar_sinOpinion_devuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("Opinión propia sobre el negocio", 7L))
                .when(opinionService).eliminar(any(), eq(7L));

        mockMvc.perform(delete(RUTA + "/mia")).andExpect(status().isNotFound());
    }
}
