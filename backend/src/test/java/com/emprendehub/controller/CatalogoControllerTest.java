package com.emprendehub.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.BarrioResponse;
import com.emprendehub.dto.CategoriaNegocioResponse;
import com.emprendehub.dto.CiudadResponse;
import com.emprendehub.dto.OpcionResponse;
import com.emprendehub.service.CatalogoService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(CatalogoController.class)
class CatalogoControllerTest extends ControllerTestBase {

    @MockitoBean
    private CatalogoService catalogoService;

    @Test
    @DisplayName("GET /api/v1/catalogos/categorias-negocio devuelve 200 con la lista")
    void categoriasNegocio_devuelve200() throws Exception {
        when(catalogoService.obtenerCategoriasNegocio())
                .thenReturn(List.of(new CategoriaNegocioResponse(1L, "Gastronomía", "🍴")));

        mockMvc.perform(get("/api/v1/catalogos/categorias-negocio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Gastronomía"))
                .andExpect(jsonPath("$[0].icono").value("🍴"));
    }

    @Test
    @DisplayName("GET /api/v1/catalogos/ciudades devuelve las ciudades con sus barrios anidados")
    void ciudades_devuelveLosBarriosAnidados() throws Exception {
        when(catalogoService.obtenerCiudades()).thenReturn(List.of(
                new CiudadResponse(1L, "Medellín", List.of(new BarrioResponse(1L, "El Poblado")))));

        mockMvc.perform(get("/api/v1/catalogos/ciudades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Medellín"))
                .andExpect(jsonPath("$[0].barrios[0].nombre").value("El Poblado"));
    }

    @Test
    @DisplayName("GET /api/v1/catalogos/categorias-curso devuelve código y nombre")
    void categoriasCurso_devuelveCodigoYNombre() throws Exception {
        when(catalogoService.obtenerCategoriasCurso())
                .thenReturn(List.of(new OpcionResponse("MARKETING", "Marketing")));

        mockMvc.perform(get("/api/v1/catalogos/categorias-curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MARKETING"))
                .andExpect(jsonPath("$[0].nombre").value("Marketing"));
    }

    @Test
    @DisplayName("GET /api/v1/catalogos/niveles-curso devuelve 200")
    void nivelesCurso_devuelve200() throws Exception {
        when(catalogoService.obtenerNivelesCurso())
                .thenReturn(List.of(new OpcionResponse("BASICO", "Básico")));

        mockMvc.perform(get("/api/v1/catalogos/niveles-curso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("BASICO"));
    }

    @Test
    @DisplayName("Una ruta de catálogo que no existe devuelve 404")
    void rutaInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/catalogos/no-existe"))
                .andExpect(status().isNotFound());
    }
}
