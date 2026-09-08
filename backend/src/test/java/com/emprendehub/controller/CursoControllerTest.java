package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.CursoResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.CursoService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CursoController.class)
class CursoControllerTest extends ControllerTestBase {

    @MockitoBean
    private CursoService cursoService;

    private CursoResponse cursoDePrueba() {
        return new CursoResponse(1L, "Marketing Digital Básico", "Fundamentos", "4 horas",
                "MARKETING", "BASICO", true, null, "https://ejemplo.co", "📱", "PUBLICADO");
    }

    @Test
    @DisplayName("GET /api/v1/cursos devuelve 200 con la página de resultados")
    void buscar_devuelve200ConLaPagina() throws Exception {
        when(cursoService.buscarPublicados(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cursoDePrueba())));

        mockMvc.perform(get("/api/v1/cursos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Marketing Digital Básico"))
                .andExpect(jsonPath("$.content[0].gratuito").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/cursos acepta los filtros como parámetros de consulta")
    void buscar_conFiltros_devuelve200() throws Exception {
        when(cursoService.buscarPublicados(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/cursos")
                        .param("categoria", "MARKETING")
                        .param("nivel", "BASICO")
                        .param("gratuito", "true")
                        .param("texto", "redes"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/cursos con una categoría inventada devuelve 400")
    void buscar_categoriaInvalida_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/cursos").param("categoria", "COCINA"))
                .andExpect(status().isBadRequest())
                // Este 400 salía con el cuerpo por defecto de Spring desde el
                // PR 4: era el único del sistema con otra forma. Ahora pasa por
                // el GlobalExceptionHandler como el resto.
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.categoria").exists());
    }

    @Test
    @DisplayName("GET /api/v1/cursos/{id} devuelve 200 cuando el curso está publicado")
    void obtener_publicado_devuelve200() throws Exception {
        when(cursoService.obtenerPublicado(1L)).thenReturn(cursoDePrueba());

        mockMvc.perform(get("/api/v1/cursos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Marketing Digital Básico"));
    }

    @Test
    @DisplayName("GET /api/v1/cursos/{id} devuelve 404 con el cuerpo de error esperado")
    void obtener_inexistente_devuelve404() throws Exception {
        when(cursoService.obtenerPublicado(99L))
                .thenThrow(new ResourceNotFoundException("Curso", 99L));

        mockMvc.perform(get("/api/v1/cursos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
