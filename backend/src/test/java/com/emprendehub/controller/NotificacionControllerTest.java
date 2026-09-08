package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.NotificacionResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.NotificacionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(NotificacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificacionControllerTest extends ControllerTestBase {

    @MockitoBean
    private NotificacionService notificacionService;

    private static final String RUTA = "/api/v1/notificaciones";

    private NotificacionResponse avisoDePrueba(boolean leida) {
        return new NotificacionResponse(2L, "OPINION_NUEVA",
                "Carlos opinó sobre tu negocio: 5 estrellas", leida,
                Instant.parse("2026-08-26T10:00:00Z"));
    }

    @Test
    @DisplayName("GET devuelve los avisos propios, los más recientes primero")
    void mias_devuelve200() throws Exception {
        when(notificacionService.mias(any(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(avisoDePrueba(false))));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("OPINION_NUEVA"))
                .andExpect(jsonPath("$.content[0].leida").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET con leida=false pide solo los pendientes (H3)")
    void mias_soloPendientes_trasladaElFiltro() throws Exception {
        when(notificacionService.mias(any(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(RUTA).param("leida", "false")).andExpect(status().isOk());

        verify(notificacionService).mias(any(), eq(false), any());
    }

    @Test
    @DisplayName("PATCH marca un aviso como leído")
    void marcarLectura_devuelve200() throws Exception {
        when(notificacionService.marcarLectura(any(), eq(2L), eq(true)))
                .thenReturn(avisoDePrueba(true));

        mockMvc.perform(patch(RUTA + "/2/lectura").param("leida", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leida").value(true));
    }

    @Test
    @DisplayName("PATCH sobre el aviso de otra persona devuelve 404")
    void marcarLectura_ajena_devuelve404() throws Exception {
        when(notificacionService.marcarLectura(any(), eq(99L), eq(true)))
                .thenThrow(new ResourceNotFoundException("Notificación", 99L));

        mockMvc.perform(patch(RUTA + "/99/lectura").param("leida", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /leer-todas responde cuántas se marcaron")
    void marcarTodas_devuelveCuantas() throws Exception {
        when(notificacionService.marcarTodasLeidas(any())).thenReturn(7);

        mockMvc.perform(patch(RUTA + "/leer-todas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marcadas").value(7));
    }
}
