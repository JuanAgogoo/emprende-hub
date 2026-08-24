package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.CategoriaNegocioResponse;
import com.emprendehub.dto.CiudadResponse;
import com.emprendehub.dto.OpcionResponse;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private CategoriaNegocioRepository categoriaNegocioRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @InjectMocks
    private CatalogoService service;

    private Ciudad crearMedellinDePrueba() {
        Ciudad medellin = new Ciudad("Medellín");
        // A propósito en orden inverso: el servicio debe ordenarlos.
        Barrio laureles = new Barrio("Laureles");
        Barrio poblado = new Barrio("El Poblado");
        medellin.agregarBarrio(laureles);
        medellin.agregarBarrio(poblado);
        return medellin;
    }

    @Test
    @DisplayName("obtenerCategoriasNegocio: mapea cada categoría a su respuesta")
    void obtenerCategoriasNegocio_devuelveLasCategoriasMapeadas() {
        // Arrange
        when(categoriaNegocioRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(new CategoriaNegocio("Gastronomía", "🍴")));

        // Act
        List<CategoriaNegocioResponse> resultado = service.obtenerCategoriasNegocio();

        // Assert
        assertEquals(1, resultado.size());
        assertEquals("Gastronomía", resultado.getFirst().nombre());
        assertEquals("🍴", resultado.getFirst().icono());
        verify(categoriaNegocioRepository, times(1)).findAllByOrderByNombreAsc();
    }

    @Test
    @DisplayName("obtenerCategoriasNegocio: devuelve lista vacía si no hay ninguna")
    void obtenerCategoriasNegocio_sinDatos_devuelveListaVacia() {
        when(categoriaNegocioRepository.findAllByOrderByNombreAsc()).thenReturn(List.of());

        assertTrue(service.obtenerCategoriasNegocio().isEmpty());
    }

    @Test
    @DisplayName("obtenerCiudades: anida los barrios dentro de su ciudad")
    void obtenerCiudades_anidaLosBarrios() {
        when(ciudadRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(crearMedellinDePrueba()));

        List<CiudadResponse> resultado = service.obtenerCiudades();

        assertEquals(1, resultado.size());
        assertEquals("Medellín", resultado.getFirst().nombre());
        assertEquals(2, resultado.getFirst().barrios().size());
    }

    @Test
    @DisplayName("obtenerCiudades: ordena los barrios por nombre")
    void obtenerCiudades_ordenaLosBarriosPorNombre() {
        when(ciudadRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(crearMedellinDePrueba()));

        List<CiudadResponse> resultado = service.obtenerCiudades();

        assertEquals("El Poblado", resultado.getFirst().barrios().get(0).nombre());
        assertEquals("Laureles", resultado.getFirst().barrios().get(1).nombre());
    }

    @Test
    @DisplayName("obtenerCiudades: una ciudad sin barrios devuelve la lista vacía")
    void obtenerCiudades_sinBarrios_devuelveListaVacia() {
        when(ciudadRepository.findAllByOrderByNombreAsc())
                .thenReturn(List.of(new Ciudad("Envigado")));

        assertTrue(service.obtenerCiudades().getFirst().barrios().isEmpty());
    }

    @Test
    @DisplayName("obtenerCategoriasCurso: devuelve las 5 del enum sin tocar la base de datos")
    void obtenerCategoriasCurso_devuelveLasCinco() {
        List<OpcionResponse> resultado = service.obtenerCategoriasCurso();

        assertEquals(5, resultado.size());
        assertEquals("MARKETING", resultado.getFirst().codigo());
        assertEquals("Marketing", resultado.getFirst().nombre());
    }

    @Test
    @DisplayName("obtenerNivelesCurso: devuelve los 3 niveles en orden de dificultad")
    void obtenerNivelesCurso_devuelveLosTres() {
        List<OpcionResponse> resultado = service.obtenerNivelesCurso();

        assertEquals(3, resultado.size());
        assertEquals("BASICO", resultado.get(0).codigo());
        assertEquals("INTERMEDIO", resultado.get(1).codigo());
        assertEquals("AVANZADO", resultado.get(2).codigo());
    }
}
