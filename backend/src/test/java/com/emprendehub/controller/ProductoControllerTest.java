package com.emprendehub.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.service.ProductoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(ProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductoControllerTest extends ControllerTestBase {

    @MockitoBean
    private ProductoService productoService;

    private static final String RUTA = "/api/v1/negocios/mio/productos";

    private ProductoResponse productoDePrueba() {
        return new ProductoResponse(3L, "Pan de masa madre", new BigDecimal("12000"),
                "Fermentado 24 horas", true, "/fotos/pan.jpg");
    }

    /** La imagen obligatoria que acompaña a toda alta (B9). */
    private MockMultipartFile imagen() {
        return new MockMultipartFile("foto", "pan.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private String cuerpo(String nombre, String precio) {
        return """
                { "nombre": "%s", "precio": %s, "descripcion": "Fermentado 24 horas",
                  "disponible": true }
                """.formatted(nombre, precio);
    }

    @Test
    @DisplayName("POST devuelve 201 con el producto creado")
    void crear_devuelve201() throws Exception {
        when(productoService.crear(any(), any(), any())).thenReturn(productoDePrueba());

        mockMvc.perform(multipart(RUTA).file(imagen())
                        .param("nombre", "Pan de masa madre")
                        .param("precio", "12000")
                        .param("descripcion", "Fermentado 24 horas")
                        .param("disponible", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Pan de masa madre"))
                .andExpect(jsonPath("$.precio").value(12000))
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    @DisplayName("POST sin nombre devuelve 400 con una clave por campo inválido")
    void crear_sinNombre_devuelve400() throws Exception {
        mockMvc.perform(multipart(RUTA).file(imagen())
                        .param("nombre", "")
                        .param("precio", "12000")
                        .param("disponible", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists());
    }

    @Test
    @DisplayName("POST con precio negativo devuelve 400 señalando el precio")
    void crear_precioNegativo_devuelve400() throws Exception {
        mockMvc.perform(multipart(RUTA).file(imagen())
                        .param("nombre", "Croissant")
                        .param("precio", "-100")
                        .param("disponible", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").exists());
    }

    @Test
    @DisplayName("GET devuelve el escaparate propio")
    void listar_devuelveElEscaparate() throws Exception {
        when(productoService.listarMios(any())).thenReturn(List.of(productoDePrueba()));

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Fermentado 24 horas"));
    }

    @Test
    @DisplayName("PUT actualiza el producto y devuelve 200")
    void actualizar_devuelve200() throws Exception {
        when(productoService.actualizar(any(), eq(3L), any())).thenReturn(productoDePrueba());

        mockMvc.perform(put(RUTA + "/3").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("Pan de masa madre", "12000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("PATCH cambia solo la disponibilidad (F3)")
    void cambiarDisponibilidad_devuelve200() throws Exception {
        when(productoService.cambiarDisponibilidad(any(), eq(3L), eq(false)))
                .thenReturn(new ProductoResponse(3L, "Pan de masa madre",
                        new BigDecimal("12000"), "Fermentado 24 horas", false,
                        "/fotos/pan.jpg"));

        mockMvc.perform(patch(RUTA + "/3/disponibilidad").param("disponible", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
    }

    @Test
    @DisplayName("DELETE devuelve 204 y no responde cuerpo")
    void eliminar_devuelve204() throws Exception {
        mockMvc.perform(delete(RUTA + "/3")).andExpect(status().isNoContent());

        verify(productoService).eliminar(any(), eq(3L));
    }
}
