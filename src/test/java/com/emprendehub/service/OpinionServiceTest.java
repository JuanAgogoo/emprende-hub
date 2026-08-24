package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.ActualizarOpinionRequest;
import com.emprendehub.dto.CrearOpinionRequest;
import com.emprendehub.dto.OpinionResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.DenunciaRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.OpinionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OpinionServiceTest {

    @Mock
    private OpinionRepository opinionRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private DenunciaRepository denunciaRepository;

    @InjectMocks
    private OpinionService service;

    private static final Long ID_NEGOCIO = 7L;

    private Usuario usuario(Long id, String nombre, String correo) {
        Usuario usuario = new Usuario(nombre, correo, "hash", Rol.CLIENTE);
        usuario.setId(id);
        return usuario;
    }

    private final Usuario cliente = usuario(3L, "Carlos Rueda", "carlos@test.co");
    private final Usuario duena = usuario(4L, "María García", "maria@test.co");

    private Negocio negocio() {
        Negocio negocio = new Negocio(duena, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                new CategoriaNegocio("Gastronomía", "🍴"), new Ciudad("Medellín"), null,
                NivelPrecio.MEDIO);
        negocio.setId(ID_NEGOCIO);
        negocio.setEstado(EstadoNegocio.APROBADO);
        return negocio;
    }

    private Negocio conNegocioVisible() {
        Negocio negocio = negocio();
        when(negocioRepository.buscarVisibleEnDirectorio(ID_NEGOCIO))
                .thenReturn(Optional.of(negocio));
        return negocio;
    }

    private Opinion opinion(Negocio negocio, Usuario autor, int calificacion) {
        Opinion opinion = new Opinion(negocio, autor, calificacion, "Muy bueno todo");
        opinion.setId(11L);
        return opinion;
    }

    /** El recuento y la media que devolvería la base de datos. */
    private OpinionRepository.ResumenDeCalificacion resumen(long total, Double promedio) {
        return new OpinionRepository.ResumenDeCalificacion() {
            @Override
            public long getTotal() {
                return total;
            }

            @Override
            public Double getPromedio() {
                return promedio;
            }
        };
    }

    private void conResumen(long total, Double promedio) {
        when(opinionRepository.resumirPorNegocio(ID_NEGOCIO))
                .thenReturn(resumen(total, promedio));
    }

    private CrearOpinionRequest peticion(int calificacion, String comentario) {
        return new CrearOpinionRequest(calificacion, comentario);
    }

    // ---------- Publicar (C1, C2, C4, A4) ----------

    @Test
    @DisplayName("Una opinión se publica al instante, sin pasar por revisión (C4)")
    void crear_sePublicaAlInstante() {
        conNegocioVisible();
        when(opinionRepository.existsByNegocioIdAndAutorId(ID_NEGOCIO, 3L)).thenReturn(false);
        when(opinionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        conResumen(1, 5.0);

        OpinionResponse respuesta = service.crear(cliente, ID_NEGOCIO, peticion(5, "Excelente"));

        assertEquals(5, respuesta.calificacion());
        assertEquals("Carlos Rueda", respuesta.autor());
        assertFalse(respuesta.editada());
    }

    @Test
    @DisplayName("Publicar deja al día el promedio y el recuento del negocio")
    void crear_recalculaElPromedio() {
        Negocio negocio = conNegocioVisible();
        when(opinionRepository.existsByNegocioIdAndAutorId(ID_NEGOCIO, 3L)).thenReturn(false);
        when(opinionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        conResumen(3, 4.333333);

        service.crear(cliente, ID_NEGOCIO, peticion(4, null));

        assertEquals(3, negocio.getNumeroOpiniones());
        assertEquals(new BigDecimal("4.33"), negocio.getCalificacionPromedio());
        verify(negocioRepository).save(negocio);
    }

    @Test
    @DisplayName("El emprendedor no puede opinar sobre su propio negocio (A4)")
    void crear_sobreElNegocioPropio_lanza() {
        conNegocioVisible();

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.crear(duena, ID_NEGOCIO, peticion(5, "El mío es el mejor")));

        assertTrue(error.getMessage().contains("tu propio negocio"));
        verify(opinionRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede opinar dos veces del mismo negocio (C2)")
    void crear_segundaVez_lanza() {
        conNegocioVisible();
        when(opinionRepository.existsByNegocioIdAndAutorId(ID_NEGOCIO, 3L)).thenReturn(true);

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.crear(cliente, ID_NEGOCIO, peticion(4, "Otra vez")));

        assertTrue(error.getMessage().contains("edita tu opinión"));
    }

    @Test
    @DisplayName("Un negocio que no está publicado no admite opiniones (B6)")
    void crear_negocioNoVisible_lanzaNoEncontrado() {
        when(negocioRepository.buscarVisibleEnDirectorio(ID_NEGOCIO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.crear(cliente, ID_NEGOCIO, peticion(5, "Bien")));
    }

    @Test
    @DisplayName("Un comentario en blanco se guarda como ausente: es opcional")
    void crear_comentarioEnBlanco_quedaNulo() {
        conNegocioVisible();
        when(opinionRepository.existsByNegocioIdAndAutorId(ID_NEGOCIO, 3L)).thenReturn(false);
        when(opinionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        conResumen(1, 5.0);

        assertNull(service.crear(cliente, ID_NEGOCIO, peticion(5, "   ")).comentario());
    }

    // ---------- Editar y borrar (C2) ----------

    @Test
    @DisplayName("El autor edita su opinión y queda marcada como editada")
    void actualizar_marcaLaEdicion() {
        Negocio negocio = negocio();
        Opinion opinion = opinion(negocio, cliente, 5);
        when(opinionRepository.findByNegocioIdAndAutorId(ID_NEGOCIO, 3L))
                .thenReturn(Optional.of(opinion));
        when(opinionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        conResumen(1, 2.0);

        OpinionResponse respuesta = service.actualizar(cliente, ID_NEGOCIO,
                new ActualizarOpinionRequest(2, "Cambió a peor"));

        assertEquals(2, respuesta.calificacion());
        assertTrue(respuesta.editada(), "quien la lea debe saber que el texto no es el original");
    }

    @Test
    @DisplayName("Editar recalcula el promedio: la nota pudo cambiar")
    void actualizar_recalculaElPromedio() {
        Negocio negocio = negocio();
        when(opinionRepository.findByNegocioIdAndAutorId(ID_NEGOCIO, 3L))
                .thenReturn(Optional.of(opinion(negocio, cliente, 5)));
        when(opinionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        conResumen(1, 2.0);

        service.actualizar(cliente, ID_NEGOCIO, new ActualizarOpinionRequest(2, null));

        assertEquals(new BigDecimal("2.00"), negocio.getCalificacionPromedio());
    }

    @Test
    @DisplayName("Quien no ha opinado no tiene nada que editar")
    void actualizar_sinOpinionPropia_lanzaNoEncontrado() {
        when(opinionRepository.findByNegocioIdAndAutorId(ID_NEGOCIO, 3L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(cliente,
                ID_NEGOCIO, new ActualizarOpinionRequest(3, null)));
    }

    @Test
    @DisplayName("Borrar la opinión propia deja el negocio sin calificación si era la única (C5)")
    void eliminar_ultimaOpinion_dejaElPromedioNulo() {
        Negocio negocio = negocio();
        negocio.setCalificacionPromedio(new BigDecimal("5.00"));
        negocio.setNumeroOpiniones(1);
        Opinion opinion = opinion(negocio, cliente, 5);
        when(opinionRepository.findByNegocioIdAndAutorId(ID_NEGOCIO, 3L))
                .thenReturn(Optional.of(opinion));
        when(denunciaRepository.findByOpinionId(11L)).thenReturn(List.of());
        conResumen(0, null);

        service.eliminar(cliente, ID_NEGOCIO);

        verify(opinionRepository).delete(opinion);
        // Cero significaría la peor nota posible; no tener ninguna es otra cosa.
        assertNull(negocio.getCalificacionPromedio());
        assertEquals(0, negocio.getNumeroOpiniones());
    }

    @Test
    @DisplayName("Borrar una opinión se lleva las denuncias que la señalaban")
    void eliminar_arrastraSusDenuncias() {
        Negocio negocio = negocio();
        Opinion opinion = opinion(negocio, cliente, 5);
        when(opinionRepository.findByNegocioIdAndAutorId(ID_NEGOCIO, 3L))
                .thenReturn(Optional.of(opinion));
        var denuncias = List.of(new com.emprendehub.model.Denuncia(
                opinion, duena, com.emprendehub.model.MotivoDenuncia.SPAM));
        when(denunciaRepository.findByOpinionId(11L)).thenReturn(denuncias);
        conResumen(0, null);

        service.eliminar(cliente, ID_NEGOCIO);

        // Si sobrevivieran, la cola del administrador se llenaría de avisos
        // sobre textos que ya no existen.
        verify(denunciaRepository).deleteAll(denuncias);
    }

    // ---------- Borrado por la moderación ----------

    @Test
    @DisplayName("El borrado del administrador también recalcula el promedio")
    void eliminarPorModeracion_recalcula() {
        // Es el cuarto momento en que una opinión cambia y el que más fácil se
        // escapa, porque no lo dispara su autor.
        Negocio negocio = negocio();
        negocio.setNumeroOpiniones(2);
        Opinion opinion = opinion(negocio, cliente, 1);
        when(denunciaRepository.findByOpinionId(11L)).thenReturn(List.of());
        conResumen(1, 5.0);

        service.eliminarPorModeracion(opinion);

        verify(opinionRepository).delete(opinion);
        assertEquals(1, negocio.getNumeroOpiniones());
        assertEquals(new BigDecimal("5.00"), negocio.getCalificacionPromedio());
    }

    // ---------- Lectura ----------

    @Test
    @DisplayName("Las opiniones de un negocio publicado se leen sin sesión")
    void listar_devuelveLasDelNegocio() {
        Negocio negocio = conNegocioVisible();
        when(opinionRepository.findByNegocioIdOrderByFechaCreacionDesc(
                ID_NEGOCIO, Pageable.ofSize(10)))
                .thenReturn(new PageImpl<>(List.of(opinion(negocio, cliente, 5))));

        var pagina = service.listarDeNegocio(ID_NEGOCIO, Pageable.ofSize(10));

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Carlos Rueda", pagina.getContent().getFirst().autor());
    }

    @Test
    @DisplayName("Un negocio sin publicar no enseña opiniones (B6)")
    void listar_negocioNoVisible_lanzaNoEncontrado() {
        when(negocioRepository.buscarVisibleEnDirectorio(ID_NEGOCIO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listarDeNegocio(ID_NEGOCIO, Pageable.ofSize(10)));
    }

    @Test
    @DisplayName("La opinión propia se consulta aparte, para saber si ya opinó")
    void obtenerLaMia_devuelveLaSuya() {
        when(opinionRepository.findByNegocioIdAndAutorId(ID_NEGOCIO, 3L))
                .thenReturn(Optional.of(opinion(negocio(), cliente, 4)));

        assertEquals(4, service.obtenerLaMia(cliente, ID_NEGOCIO).calificacion());
    }
}
