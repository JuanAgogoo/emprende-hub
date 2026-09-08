package com.emprendehub.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.EstadisticasPortadaResponse;
import com.emprendehub.service.EstadisticasService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(EstadisticasController.class)
class EstadisticasControllerTest extends ControllerTestBase {

    @MockitoBean
    private EstadisticasService estadisticasService;

    @Test
    @DisplayName("GET /api/v1/estadisticas/portada devuelve las cuatro cifras (H4)")
    void portada_devuelve200ConLasCifras() throws Exception {
        when(estadisticasService.obtenerPortada()).thenReturn(
                new EstadisticasPortadaResponse(12, 12, 34, new BigDecimal("4.7")));

        mockMvc.perform(get("/api/v1/estadisticas/portada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.negociosActivos").value(12))
                .andExpect(jsonPath("$.categorias").value(12))
                .andExpect(jsonPath("$.usuariosRegistrados").value(34))
                .andExpect(jsonPath("$.calificacionPromedio").value(4.7));
    }

    @Test
    @DisplayName("Sin ninguna opinión la media viaja como nula, no como cero (C5)")
    void portada_sinCalificaciones_devuelveNulo() throws Exception {
        when(estadisticasService.obtenerPortada())
                .thenReturn(new EstadisticasPortadaResponse(3, 12, 5, null));

        mockMvc.perform(get("/api/v1/estadisticas/portada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionPromedio").doesNotExist());
    }
}
