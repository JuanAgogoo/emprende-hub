package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.service.DenunciaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DenunciaController.class)
@AutoConfigureMockMvc(addFilters = false)
class DenunciaControllerTest extends ControllerTestBase {

    @MockitoBean
    private DenunciaService denunciaService;

    private static final String RUTA = "/api/v1/opiniones/11/denuncias";

    @Test
    @DisplayName("POST con un motivo de la lista devuelve 204")
    void denunciar_devuelve204() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"LENGUAJE_INAPROPIADO\"}"))
                .andExpect(status().isNoContent());

        verify(denunciaService).denunciar(any(), eq(11L), any());
    }

    @Test
    @DisplayName("POST sin motivo devuelve 400: la lista es cerrada (C6)")
    void denunciar_sinMotivo_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.motivo").exists());
    }

    @Test
    @DisplayName("POST con un motivo inventado devuelve 400 y enumera los admitidos")
    void denunciar_motivoInventado_devuelve400() throws Exception {
        // No hay texto libre: un motivo fuera de la lista no llega al servicio.
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"NO_ME_GUSTA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Denunciar dos veces devuelve 400 con su motivo")
    void denunciar_dosVeces_devuelve400() throws Exception {
        doThrow(new ReglaDeNegocioException("Ya denunciaste esta opinión"))
                .when(denunciaService).denunciar(any(), eq(11L), any());

        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"SPAM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ya denunciaste esta opinión"));
    }
}
