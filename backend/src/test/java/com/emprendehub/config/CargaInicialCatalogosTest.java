package com.emprendehub.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.model.Barrio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CargaInicialCatalogosTest {

    @Mock
    private CategoriaNegocioRepository categoriaRepository;

    @Mock
    private CiudadRepository ciudadRepository;

    @Test
    @DisplayName("Las ciudades se guardan siempre en el mismo orden, y con él sus identificadores")
    void cargarCatalogos_guardaLasCiudadesEnOrdenFijo() throws Exception {
        //arrange
        when(categoriaRepository.count()).thenReturn(1L);
        when(ciudadRepository.count()).thenReturn(0L);

        //act
        new CargaInicialCatalogos().cargarCatalogos(categoriaRepository, ciudadRepository).run();

        //assert
        ArgumentCaptor<Ciudad> guardadas = ArgumentCaptor.forClass(Ciudad.class);
        verify(ciudadRepository, times(4)).save(guardadas.capture());
        assertEquals(List.of("Medellín", "Envigado", "Itagüí", "Bello"),
                guardadas.getAllValues().stream().map(Ciudad::getNombre).toList());
        assertEquals(List.of("El Poblado", "Laureles"),
                guardadas.getAllValues().get(0).getBarrios().stream()
                        .map(Barrio::getNombre).toList());
    }
}
