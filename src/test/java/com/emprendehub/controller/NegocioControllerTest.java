package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.service.NegocioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato del controlador de negocios: códigos y validaciones de entrada.
 *
 * <p>Los filtros van desactivados a propósito; que estas rutas exijan sesión se
 * comprueba en {@code SeguridadAccesoTest}.
 */
@WebMvcTest(NegocioController.class)
@AutoConfigureMockMvc(addFilters = false)
class NegocioControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NegocioService negocioService;

    private static final String DESCRIPCION =
            "Panadería artesanal con recetas familiares de más de cincuenta años, "
                    + "pan de masa madre horneado cada mañana en horno de leña.";

    private String cuerpo(String telefono, String descripcion) {
        return """
                {
                  "nombre": "Panadería La Tradicional",
                  "descripcion": "%s",
                  "telefono": "%s",
                  "categoriaId": 5,
                  "ciudadId": 10,
                  "nivelPrecio": "MEDIO"
                }
                """.formatted(descripcion, telefono);
    }

    private NegocioResponse respuesta() {
        return new NegocioResponse(1L, "Panadería La Tradicional", DESCRIPCION, "3001234567",
                "Gastronomía", "Medellín", "El Poblado", "MEDIO", "PENDIENTE", null, null, 0);
    }

    @Test
    @DisplayName("POST /api/v1/negocios devuelve 201 con el negocio pendiente")
    void registrar_valido_devuelve201() throws Exception {
        when(negocioService.registrar(any(), any())).thenReturn(respuesta());

        mvc.perform(post("/api/v1/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("3001234567", DESCRIPCION)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("POST acepta un teléfono fijo, no solo móviles")
    void registrar_telefonoFijo_devuelve201() throws Exception {
        when(negocioService.registrar(any(), any())).thenReturn(respuesta());

        mvc.perform(post("/api/v1/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("6044440000", DESCRIPCION)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST con un teléfono que no es ni fijo ni móvil devuelve 400")
    void registrar_telefonoInvalido_devuelve400() throws Exception {
        mvc.perform(post("/api/v1/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("123", DESCRIPCION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.telefono").exists());
    }

    @Test
    @DisplayName("POST con una descripción de menos de 80 caracteres devuelve 400")
    void registrar_descripcionCorta_devuelve400() throws Exception {
        mvc.perform(post("/api/v1/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("3001234567", "Muy corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.descripcion").exists());
    }

    @Test
    @DisplayName("POST de quien ya tiene negocio devuelve 400 con el motivo")
    void registrar_yaTieneNegocio_devuelve400() throws Exception {
        when(negocioService.registrar(any(), any()))
                .thenThrow(new ReglaDeNegocioException("Esta cuenta ya tiene un negocio registrado"));

        mvc.perform(post("/api/v1/negocios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("3001234567", DESCRIPCION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Esta cuenta ya tiene un negocio registrado"));
    }
}
