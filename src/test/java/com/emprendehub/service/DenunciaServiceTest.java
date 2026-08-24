package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.DenunciarOpinionRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Denuncia;
import com.emprendehub.model.EstadoDenuncia;
import com.emprendehub.model.MotivoDenuncia;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.DenunciaRepository;
import com.emprendehub.repository.OpinionRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DenunciaServiceTest {

    @Mock
    private DenunciaRepository denunciaRepository;

    @Mock
    private OpinionRepository opinionRepository;

    @InjectMocks
    private DenunciaService service;

    private Usuario usuario(Long id, String nombre) {
        Usuario usuario = new Usuario(nombre, nombre.toLowerCase() + "@test.co", "hash",
                Rol.CLIENTE);
        usuario.setId(id);
        return usuario;
    }

    private final Usuario autor = usuario(3L, "Carlos");
    private final Usuario dueno = usuario(4L, "Maria");

    private Opinion opinion() {
        Negocio negocio = new Negocio(dueno, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                new CategoriaNegocio("Gastronomía", "🍴"), new Ciudad("Medellín"), null,
                NivelPrecio.MEDIO);
        negocio.setId(7L);
        Opinion opinion = new Opinion(negocio, autor, 1, "Texto denunciable");
        opinion.setId(11L);
        return opinion;
    }

    private DenunciarOpinionRequest peticion() {
        return new DenunciarOpinionRequest(MotivoDenuncia.LENGUAJE_INAPROPIADO);
    }

    @Test
    @DisplayName("Cualquier usuario con sesión puede denunciar una opinión (C3)")
    void denunciar_creaLaDenunciaPendiente() {
        when(opinionRepository.findWithDetalleById(11L)).thenReturn(Optional.of(opinion()));
        when(denunciaRepository.existsByOpinionIdAndDenuncianteId(11L, 4L)).thenReturn(false);

        service.denunciar(dueno, 11L, peticion());

        ArgumentCaptor<Denuncia> captor = ArgumentCaptor.forClass(Denuncia.class);
        verify(denunciaRepository).save(captor.capture());
        assertEquals(EstadoDenuncia.PENDIENTE, captor.getValue().getEstado());
        assertEquals(MotivoDenuncia.LENGUAJE_INAPROPIADO, captor.getValue().getMotivo());
    }

    @Test
    @DisplayName("Denunciar no oculta la opinión: sigue publicada mientras se decide (C4)")
    void denunciar_noTocaLaOpinion() {
        when(opinionRepository.findWithDetalleById(11L)).thenReturn(Optional.of(opinion()));
        when(denunciaRepository.existsByOpinionIdAndDenuncianteId(11L, 4L)).thenReturn(false);

        service.denunciar(dueno, 11L, peticion());

        // Esconderla al primer aviso convertiría el botón en uno de censurar.
        verify(opinionRepository, never()).delete(any());
        verify(opinionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nadie denuncia su propia opinión: para eso puede borrarla")
    void denunciar_laPropia_lanza() {
        when(opinionRepository.findWithDetalleById(11L)).thenReturn(Optional.of(opinion()));

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.denunciar(autor, 11L, peticion()));

        assertTrue(error.getMessage().contains("puedes borrarla"));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("La misma persona no denuncia dos veces la misma opinión")
    void denunciar_dosVeces_lanza() {
        when(opinionRepository.findWithDetalleById(11L)).thenReturn(Optional.of(opinion()));
        when(denunciaRepository.existsByOpinionIdAndDenuncianteId(11L, 4L)).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> service.denunciar(dueno, 11L, peticion()));
    }

    @Test
    @DisplayName("Una opinión que no existe no se puede denunciar")
    void denunciar_opinionInexistente_lanzaNoEncontrado() {
        when(opinionRepository.findWithDetalleById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.denunciar(dueno, 99L, peticion()));
    }
}
