package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.MetricasVisitasResponse;
import com.emprendehub.dto.PeriodoMetricaResponse;
import com.emprendehub.dto.PuntoSerieResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.VisitaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(MetricasController.class)
@AutoConfigureMockMvc(addFilters = false)
class MetricasControllerTest extends ControllerTestBase {

    @MockitoBean
    private VisitaService visitaService;

    private static final String RUTA = "/api/v1/negocios/mio/metricas/visitas";

    @Test
    @DisplayName("GET devuelve las cifras del panel con su variación")
    void visitas_devuelve200() throws Exception {
        when(visitaService.metricasDeMiNegocio(any())).thenReturn(new MetricasVisitasResponse(
                1034,
                new PeriodoMetricaResponse(248, 221, new BigDecimal("12.2")),
                new PeriodoMetricaResponse(1034, 957, new BigDecimal("8.0")),
                List.of(new PuntoSerieResponse(LocalDate.of(2026, 8, 26), 40))));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHistorico").value(1034))
                .andExpect(jsonPath("$.semana.actual").value(248))
                .andExpect(jsonPath("$.semana.variacionPorcentual").value(12.2))
                .andExpect(jsonPath("$.mes.anterior").value(957))
                .andExpect(jsonPath("$.serie[0].fecha").value("2026-08-26"))
                .andExpect(jsonPath("$.serie[0].visitas").value(40));
    }

    @Test
    @DisplayName("Sin periodo anterior la variación viaja como nula, no como cero")
    void visitas_sinPeriodoAnterior_variacionNula() throws Exception {
        when(visitaService.metricasDeMiNegocio(any())).thenReturn(new MetricasVisitasResponse(
                5,
                new PeriodoMetricaResponse(5, 0, null),
                new PeriodoMetricaResponse(5, 0, null),
                List.of()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semana.variacionPorcentual").doesNotExist())
                .andExpect(jsonPath("$.semana.anterior").value(0));
    }

    @Test
    @DisplayName("Quien no tiene negocio no tiene panel de visitas")
    void visitas_sinNegocio_devuelve404() throws Exception {
        when(visitaService.metricasDeMiNegocio(any()))
                .thenThrow(new ResourceNotFoundException("Negocio del usuario", 4L));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
