package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.EstadisticasPortadaResponse;
import com.emprendehub.model.Rol;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private CategoriaNegocioRepository categoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EstadisticasService service;

    private void conCifras(long negocios, long categorias, long usuarios, Double promedio) {
        when(negocioRepository.contarVisiblesEnDirectorio()).thenReturn(negocios);
        when(categoriaRepository.count()).thenReturn(categorias);
        when(usuarioRepository.countByActivoTrueAndRolNot(Rol.ADMIN)).thenReturn(usuarios);
        when(negocioRepository.promedioDeCalificaciones()).thenReturn(promedio);
    }

    @Test
    @DisplayName("La portada devuelve las cuatro cifras calculadas (H4)")
    void portada_devuelveLasCuatroCifras() {
        conCifras(12, 12, 34, 4.666666);

        EstadisticasPortadaResponse portada = service.obtenerPortada();

        assertEquals(12, portada.negociosActivos());
        assertEquals(12, portada.categorias());
        assertEquals(34, portada.usuariosRegistrados());
    }

    @Test
    @DisplayName("La media se redondea a un decimal, como la enseña la portada")
    void portada_redondeaLaMediaAUnDecimal() {
        conCifras(12, 12, 34, 4.666666);

        assertEquals(new BigDecimal("4.7"), service.obtenerPortada().calificacionPromedio());
    }

    @Test
    @DisplayName("Sin ninguna opinión, la media es nula y no cero (C5)")
    void portada_sinCalificaciones_devuelveMediaNula() {
        // Cero significaría que la plataforma entera está calificada con la peor
        // nota posible. No hay media porque todavía no hay opiniones.
        conCifras(3, 12, 5, null);

        assertNull(service.obtenerPortada().calificacionPromedio());
    }

    @Test
    @DisplayName("Una plataforma recién sembrada devuelve ceros sin fallar")
    void portada_sinDatos_devuelveCeros() {
        conCifras(0, 0, 0, null);

        EstadisticasPortadaResponse portada = service.obtenerPortada();

        assertEquals(0, portada.negociosActivos());
        assertEquals(0, portada.usuariosRegistrados());
        assertNull(portada.calificacionPromedio());
    }

    @Test
    @DisplayName("Una media exacta conserva su decimal")
    void portada_mediaExacta_seQuedaComoEsta() {
        conCifras(2, 12, 2, 5.0);

        assertEquals(new BigDecimal("5.0"), service.obtenerPortada().calificacionPromedio());
    }
}
