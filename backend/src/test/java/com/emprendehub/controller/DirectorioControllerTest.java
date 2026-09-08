package com.emprendehub.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emprendehub.dto.BusquedaDirectorioRequest;
import com.emprendehub.dto.FotoResponse;
import com.emprendehub.dto.NegocioPublicoResponse;
import com.emprendehub.dto.PerfilNegocioResponse;
import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.OrdenDirectorio;
import com.emprendehub.service.DirectorioService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Contrato del directorio: forma del JSON, conversión de los filtros y códigos.
 *
 * <p>Que estas rutas sean públicas se comprueba en {@code SeguridadAccesoTest},
 * con la cadena de filtros entera.
 */
@WebMvcTest(DirectorioController.class)
class DirectorioControllerTest extends ControllerTestBase {

    @MockitoBean
    private DirectorioService directorioService;

    private NegocioPublicoResponse negocioDePrueba() {
        return new NegocioPublicoResponse(7L, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                "Gastronomía", "Medellín", "El Poblado", "MEDIO",
                new BigDecimal("4.80"), 12, Instant.parse("2026-08-01T10:00:00Z"),
                "/fotos/portada.jpg");
    }

    private PerfilNegocioResponse perfilDePrueba() {
        return new PerfilNegocioResponse(7L, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                "Gastronomía", "Medellín", "El Poblado", "MEDIO",
                new BigDecimal("4.80"), 12, Instant.parse("2026-08-01T10:00:00Z"),
                "https://instagram.com/panaderia", null,
                List.of(new FotoResponse(1L, "/fotos/portada.jpg", 0, true, "APROBADA")),
                List.of(new ProductoResponse(1L, "Pan de masa madre",
                        new BigDecimal("12000"), "Fermentado 24 horas", true)));
    }

    @Test
    @DisplayName("GET /api/v1/directorio devuelve 200 con la página de resultados")
    void buscar_devuelve200ConLaPagina() throws Exception {
        when(directorioService.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(negocioDePrueba())));

        mockMvc.perform(get("/api/v1/directorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("Panadería La Tradicional"))
                .andExpect(jsonPath("$.content[0].ciudad").value("Medellín"))
                .andExpect(jsonPath("$.content[0].calificacionPromedio").value(4.80))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("La tarjeta pública no expone ni el estado ni el motivo del rechazo")
    void buscar_noExponeCamposPrivados() throws Exception {
        when(directorioService.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(negocioDePrueba())));

        mockMvc.perform(get("/api/v1/directorio"))
                .andExpect(jsonPath("$.content[0].estado").doesNotExist())
                .andExpect(jsonPath("$.content[0].motivoRechazo").doesNotExist())
                .andExpect(jsonPath("$.content[0].correo").doesNotExist());
    }

    @Test
    @DisplayName("Los filtros de la consulta llegan al servicio ya convertidos")
    void buscar_conFiltros_losTrasladaAlServicio() throws Exception {
        when(directorioService.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/directorio")
                        .param("texto", "pan")
                        .param("categoriaId", "3")
                        .param("ciudadId", "1")
                        .param("barrioId", "2")
                        .param("calificacionMinima", "4.0")
                        .param("nivelPrecio", "MEDIO")
                        .param("orden", "RECIENTES"))
                .andExpect(status().isOk());

        ArgumentCaptor<BusquedaDirectorioRequest> captor =
                ArgumentCaptor.forClass(BusquedaDirectorioRequest.class);
        verify(directorioService).buscar(captor.capture(), eq(OrdenDirectorio.RECIENTES), any());

        BusquedaDirectorioRequest filtros = captor.getValue();
        assertEquals("pan", filtros.texto());
        assertEquals(3L, filtros.categoriaId());
        assertEquals(1L, filtros.ciudadId());
        assertEquals(2L, filtros.barrioId());
        assertEquals(new BigDecimal("4.0"), filtros.calificacionMinima());
        assertEquals(NivelPrecio.MEDIO, filtros.nivelPrecio());
    }

    @Test
    @DisplayName("Sin ningún parámetro los filtros llegan a nulo y el orden también")
    void buscar_sinParametros_todoLlegaANulo() throws Exception {
        when(directorioService.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/directorio")).andExpect(status().isOk());

        ArgumentCaptor<BusquedaDirectorioRequest> captor =
                ArgumentCaptor.forClass(BusquedaDirectorioRequest.class);
        verify(directorioService).buscar(captor.capture(), eq(null), any());

        assertEquals(new BusquedaDirectorioRequest(null, null, null, null, null, null),
                captor.getValue());
    }

    @Test
    @DisplayName("Un criterio de orden inventado devuelve 400 y enumera los admitidos")
    void buscar_ordenInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/directorio").param("orden", "MAS_BARATOS"))
                .andExpect(status().isBadRequest())
                // El cuerpo es el del GlobalExceptionHandler, con message, y no
                // el de Spring por defecto, que trae path y no lo lleva.
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.message").value(
                        containsString("CALIFICACION, NOMBRE, RECIENTES")))
                .andExpect(jsonPath("$.orden").exists());
    }

    @Test
    @DisplayName("Un nivel de precio inventado devuelve 400 con su clave de campo")
    void buscar_nivelPrecioInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/directorio").param("nivelPrecio", "GRATIS"))
                .andExpect(status().isBadRequest())
                // G4 eliminó «Gratis», que venía copiada de la pantalla de cursos.
                .andExpect(jsonPath("$.nivelPrecio").value(
                        containsString("BAJO, MEDIO, ALTO")));
    }

    @Test
    @DisplayName("Un identificador que no es un número devuelve 400, no un 500")
    void perfil_idQueNoEsNumero_devuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/directorio/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("GET /api/v1/directorio/destacados devuelve la lista, no una página")
    void destacados_devuelve200ConLaLista() throws Exception {
        when(directorioService.destacados(null)).thenReturn(List.of(negocioDePrueba()));

        mockMvc.perform(get("/api/v1/directorio/destacados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Panadería La Tradicional"))
                .andExpect(jsonPath("$[0].numeroOpiniones").value(12));
    }

    @Test
    @DisplayName("El límite de destacados llega al servicio, que decide si lo acepta")
    void destacados_conLimite_loTraslada() throws Exception {
        when(directorioService.destacados(3)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/directorio/destacados").param("limite", "3"))
                .andExpect(status().isOk());

        verify(directorioService).destacados(3);
    }

    @Test
    @DisplayName("GET /api/v1/directorio/{id} devuelve el perfil con galería y escaparate")
    void perfil_visible_devuelve200() throws Exception {
        when(directorioService.obtenerPerfilYRegistrarVisita(eq(7L), any(), any()))
                .thenReturn(perfilDePrueba());

        mockMvc.perform(get("/api/v1/directorio/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("3001234567"))
                .andExpect(jsonPath("$.instagram").value("https://instagram.com/panaderia"))
                .andExpect(jsonPath("$.fotos[0].url").value("/fotos/portada.jpg"))
                .andExpect(jsonPath("$.fotos[0].principal").value(true))
                .andExpect(jsonPath("$.productos[0].nombre").value("Pan de masa madre"))
                .andExpect(jsonPath("$.productos[0].disponible").value(true))
                .andExpect(jsonPath("$.estado").doesNotExist());
    }

    @Test
    @DisplayName("El controlador pasa al servicio la huella de la petición (H1)")
    void perfil_pasaLaHuellaDeLaPeticion() throws Exception {
        when(directorioService.obtenerPerfilYRegistrarVisita(eq(7L), any(), any()))
                .thenReturn(perfilDePrueba());

        mockMvc.perform(get("/api/v1/directorio/7").header("User-Agent", "curl/8"))
                .andExpect(status().isOk());

        // Sin sesión el visitante llega a null; la huella la arma el controlador
        // con lo poco que distingue una petición anónima de otra.
        ArgumentCaptor<String> huella = ArgumentCaptor.forClass(String.class);
        verify(directorioService)
                .obtenerPerfilYRegistrarVisita(eq(7L), eq(null), huella.capture());
        assertTrue(huella.getValue().contains("curl/8"));
    }

    @Test
    @DisplayName("Un negocio no publicado devuelve 404, nunca 403 (B6)")
    void perfil_noVisible_devuelve404() throws Exception {
        when(directorioService.obtenerPerfilYRegistrarVisita(eq(99L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Negocio", 99L));

        mockMvc.perform(get("/api/v1/directorio/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
