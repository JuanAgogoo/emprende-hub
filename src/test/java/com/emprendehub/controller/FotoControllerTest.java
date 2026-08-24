package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.FotoResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.service.FotoService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Contrato de la galería: subida multipart, códigos y forma del JSON.
 *
 * <p>Los filtros van desactivados; que estas rutas exijan sesión se comprueba en
 * {@code SeguridadAccesoTest}.
 */
@WebMvcTest(FotoController.class)
@AutoConfigureMockMvc(addFilters = false)
class FotoControllerTest extends ControllerTestBase {

    @MockitoBean
    private FotoService fotoService;

    private MockMultipartFile imagen(String tipo) {
        return new MockMultipartFile("archivo", "foto.jpg", tipo, "bytes-de-la-imagen".getBytes());
    }

    private FotoResponse fotoDePrueba() {
        return new FotoResponse(1L, "/fotos/a1b2c3.jpg", 0, true, "PENDIENTE");
    }

    @Test
    @DisplayName("POST de una imagen devuelve 201 con la foto pendiente de revisión")
    void subir_devuelve201() throws Exception {
        when(fotoService.subir(any(), any())).thenReturn(fotoDePrueba());

        mockMvc.perform(multipart("/api/v1/negocios/mio/fotos").file(imagen("image/jpeg")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("/fotos/a1b2c3.jpg"))
                .andExpect(jsonPath("$.principal").value(true))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("Una imagen que rompe una regla de B9 devuelve 400 con su motivo")
    void subir_reglaIncumplida_devuelve400() throws Exception {
        when(fotoService.subir(any(), any()))
                .thenThrow(new ReglaDeNegocioException("La imagen tiene que ser JPG o PNG"));

        mockMvc.perform(multipart("/api/v1/negocios/mio/fotos").file(imagen("image/gif")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La imagen tiene que ser JPG o PNG"));
    }

    @Test
    @DisplayName("GET devuelve la galería completa del dueño")
    void listar_devuelveLaGaleria() throws Exception {
        when(fotoService.listarMias(any())).thenReturn(List.of(fotoDePrueba(),
                new FotoResponse(2L, "/fotos/d4e5f6.png", 1, false, "APROBADA")));

        mockMvc.perform(get("/api/v1/negocios/mio/fotos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].principal").value(true))
                .andExpect(jsonPath("$[1].principal").value(false))
                .andExpect(jsonPath("$[1].estado").value("APROBADA"));
    }

    @Test
    @DisplayName("DELETE devuelve 204 y no responde cuerpo")
    void eliminar_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/negocios/mio/fotos/1"))
                .andExpect(status().isNoContent());

        verify(fotoService).eliminar(any(), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    @DisplayName("DELETE de una foto que no es del negocio devuelve 404")
    void eliminar_fotoAjena_devuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("Foto", 99L))
                .when(fotoService).eliminar(any(), any());

        mockMvc.perform(delete("/api/v1/negocios/mio/fotos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
