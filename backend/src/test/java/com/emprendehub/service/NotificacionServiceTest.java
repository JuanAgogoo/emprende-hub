package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.NotificacionResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Notificacion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.TipoNotificacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.NotificacionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository repositorio;

    @InjectMocks
    private NotificacionService service;

    private Usuario duena() {
        Usuario usuario = new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR);
        usuario.setId(4L);
        return usuario;
    }

    private Notificacion notificacion(boolean leida) {
        Notificacion notificacion = new Notificacion(duena(), TipoNotificacion.OPINION_NUEVA,
                "Carlos opinó sobre tu negocio: 5 estrellas");
        notificacion.setId(2L);
        notificacion.setLeida(leida);
        return notificacion;
    }

    // ---------- Creación desde los hechos (H2) ----------

    @Test
    @DisplayName("Un aviso nace sin leer y con el texto de lo que pasó")
    void avisar_naceSinLeer() {
        service.avisar(duena(), TipoNotificacion.CONSULTA_NUEVA, "Nueva consulta de Carlos");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repositorio).save(captor.capture());
        assertFalse(captor.getValue().isLeida());
        assertEquals(TipoNotificacion.CONSULTA_NUEVA, captor.getValue().getTipo());
        assertEquals("Nueva consulta de Carlos", captor.getValue().getTexto());
    }

    @Test
    @DisplayName("Un texto larguísimo se recorta en vez de tumbar lo que lo provocó")
    void avisar_textoLargo_seRecorta() {
        // El aviso es lo accesorio: que un nombre largo impidiera publicar una
        // opinión sería absurdo.
        service.avisar(duena(), TipoNotificacion.OPINION_NUEVA, "a".repeat(400));

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repositorio).save(captor.capture());
        assertEquals(Notificacion.MAXIMO_TEXTO, captor.getValue().getTexto().length());
        assertTrue(captor.getValue().getTexto().endsWith("…"));
    }

    @Test
    @DisplayName("Un texto nulo no revienta: queda vacío")
    void avisar_textoNulo_quedaVacio() {
        service.avisar(duena(), TipoNotificacion.NEGOCIO_APROBADO, null);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(repositorio).save(captor.capture());
        assertEquals("", captor.getValue().getTexto());
    }

    // ---------- Consulta ----------

    @Test
    @DisplayName("Los avisos propios llegan con su tipo y su texto")
    void mias_devuelveLosPropios() {
        when(repositorio.buscarDe(eq(4L), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(notificacion(false))));

        NotificacionResponse aviso =
                service.mias(duena(), null, Pageable.ofSize(20)).getContent().getFirst();

        assertEquals("OPINION_NUEVA", aviso.tipo());
        assertEquals("Carlos opinó sobre tu negocio: 5 estrellas", aviso.texto());
        assertFalse(aviso.leida());
    }

    @Test
    @DisplayName("El filtro de no leídas llega al repositorio tal cual (H3)")
    void mias_soloNoLeidas_trasladaElFiltro() {
        when(repositorio.buscarDe(eq(4L), eq(false), any())).thenReturn(new PageImpl<>(List.of()));

        service.mias(duena(), false, Pageable.ofSize(20));

        verify(repositorio).buscarDe(eq(4L), eq(false), any());
    }

    // ---------- Marcado (H3) ----------

    @Test
    @DisplayName("Marcar como leído cambia el estado del aviso propio")
    void marcarLectura_leida() {
        Notificacion notificacion = notificacion(false);
        when(repositorio.findByIdAndDestinatarioId(2L, 4L)).thenReturn(Optional.of(notificacion));
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

        assertTrue(service.marcarLectura(duena(), 2L, true).leida());
    }

    @Test
    @DisplayName("Un aviso se puede devolver a pendiente")
    void marcarLectura_noLeida() {
        Notificacion notificacion = notificacion(true);
        when(repositorio.findByIdAndDestinatarioId(2L, 4L)).thenReturn(Optional.of(notificacion));
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.marcarLectura(duena(), 2L, false).leida());
    }

    @Test
    @DisplayName("El aviso de otra persona se comporta como si no existiera")
    void marcarLectura_ajena_lanzaNoEncontrado() {
        when(repositorio.findByIdAndDestinatarioId(99L, 4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.marcarLectura(duena(), 99L, true));
    }

    @Test
    @DisplayName("Marcar todas de una vez responde cuántas eran")
    void marcarTodasLeidas_devuelveCuantas() {
        List<Notificacion> pendientes = List.of(notificacion(false), notificacion(false));
        when(repositorio.findByDestinatarioIdAndLeidaFalse(4L)).thenReturn(pendientes);

        assertEquals(2, service.marcarTodasLeidas(duena()));

        assertTrue(pendientes.stream().allMatch(Notificacion::isLeida));
        verify(repositorio).saveAll(pendientes);
    }

    @Test
    @DisplayName("Sin nada pendiente, marcar todas no guarda nada útil y devuelve cero")
    void marcarTodasLeidas_sinPendientes_devuelveCero() {
        when(repositorio.findByDestinatarioIdAndLeidaFalse(4L)).thenReturn(List.of());

        assertEquals(0, service.marcarTodasLeidas(duena()));
        verify(repositorio, never()).save(any());
    }
}
