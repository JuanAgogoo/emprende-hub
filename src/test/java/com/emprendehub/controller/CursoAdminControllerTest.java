package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.CursoResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.CursoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CursoAdminController.class)
class CursoAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CursoService cursoService;

    private static final String CURSO_VALIDO = """
            {
              "titulo": "Marketing Digital Básico",
              "descripcion": "Fundamentos de redes sociales",
              "duracion": "4 horas",
              "categoria": "MARKETING",
              "nivel": "BASICO",
              "gratuito": true,
              "urlRecurso": "https://ejemplo.co/marketing",
              "emoji": "📱"
            }
            """;

    private CursoResponse cursoDePrueba() {
        return new CursoResponse(1L, "Marketing Digital Básico", "Fundamentos", "4 horas",
                "MARKETING", "BASICO", true, null, "https://ejemplo.co", "📱", "BORRADOR");
    }

    @Test
    @DisplayName("POST /api/v1/admin/cursos devuelve 201 con el curso creado")
    void crear_valido_devuelve201() throws Exception {
        when(cursoService.crear(any())).thenReturn(cursoDePrueba());

        mockMvc.perform(post("/api/v1/admin/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CURSO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Marketing Digital Básico"))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @DisplayName("POST sin título devuelve 400 con una clave por campo inválido")
    void crear_sinTitulo_devuelve400ConElCampo() throws Exception {
        String sinTitulo = CURSO_VALIDO.replace("\"titulo\": \"Marketing Digital Básico\",", "");

        mockMvc.perform(post("/api/v1/admin/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sinTitulo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.titulo").exists());
    }

    @Test
    @DisplayName("POST con precio negativo devuelve 400 señalando el precio")
    void crear_precioNegativo_devuelve400ConElCampo() throws Exception {
        String conPrecioNegativo = CURSO_VALIDO
                .replace("\"gratuito\": true,", "\"gratuito\": false,\n  \"precio\": -5,");

        mockMvc.perform(post("/api/v1/admin/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conPrecioNegativo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").exists());
    }

    @Test
    @DisplayName("POST que rompe una regla de negocio devuelve 400 con su mensaje")
    void crear_reglaIncumplida_devuelve400() throws Exception {
        when(cursoService.crear(any()))
                .thenThrow(new ReglaDeNegocioException("Un curso gratuito no puede llevar precio"));

        mockMvc.perform(post("/api/v1/admin/cursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CURSO_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Un curso gratuito no puede llevar precio"));
    }

    @Test
    @DisplayName("PATCH /{id}/publicar devuelve 200 con el curso publicado")
    void publicar_devuelve200() throws Exception {
        when(cursoService.publicar(1L)).thenReturn(new CursoResponse(1L, "Curso", "D", "4 horas",
                "MARKETING", "BASICO", true, null, "https://ejemplo.co", "📱", "PUBLICADO"));

        mockMvc.perform(patch("/api/v1/admin/cursos/1/publicar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));
    }

    @Test
    @DisplayName("PATCH /{id}/publicar sobre uno ya publicado devuelve 400")
    void publicar_yaPublicado_devuelve400() throws Exception {
        when(cursoService.publicar(1L))
                .thenThrow(new ReglaDeNegocioException("El curso ya estaba publicado"));

        mockMvc.perform(patch("/api/v1/admin/cursos/1/publicar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /{id} devuelve 204 y no responde cuerpo")
    void eliminar_existente_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/cursos/1"))
                .andExpect(status().isNoContent());

        verify(cursoService).eliminar(1L);
    }

    @Test
    @DisplayName("DELETE /{id} de un curso inexistente devuelve 404")
    void eliminar_inexistente_devuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("Curso", 99L)).when(cursoService).eliminar(eq(99L));

        mockMvc.perform(delete("/api/v1/admin/cursos/99"))
                .andExpect(status().isNotFound());
    }
}
