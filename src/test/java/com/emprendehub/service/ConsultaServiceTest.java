package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.ConsultaResponse;
import com.emprendehub.dto.EnviarConsultaRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Consulta;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.ConsultaRepository;
import com.emprendehub.repository.NegocioRepository;
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
class ConsultaServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private NegocioRepository negocioRepository;

    /** Una consulta nueva deja aviso en el panel del dueño (H2). */
    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private ConsultaService service;

    private static final Long ID_NEGOCIO = 7L;

    private Usuario usuario(Long id, String nombre, String correo) {
        Usuario usuario = new Usuario(nombre, correo, "hash", Rol.CLIENTE);
        usuario.setId(id);
        return usuario;
    }

    private final Usuario cliente = usuario(3L, "Carlos Rueda", "carlos@gmail.com");
    private final Usuario duena = usuario(4L, "María García", "maria@gmail.com");

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

    private Negocio conNegocioPropio() {
        Negocio negocio = negocio();
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.of(negocio));
        return negocio;
    }

    private Consulta consulta(Negocio negocio) {
        Consulta consulta = new Consulta(negocio, cliente,
                "Reserva para 4 personas", "¿Tienen mesa el sábado a las 8?");
        consulta.setId(9L);
        return consulta;
    }

    private EnviarConsultaRequest peticion() {
        return new EnviarConsultaRequest("  Reserva para 4 personas  ",
                "  ¿Tienen mesa el sábado a las 8?  ");
    }

    // ---------- Enviar (D1) ----------

    @Test
    @DisplayName("La consulta se guarda en el buzón, sin leer y con su remitente")
    void enviar_guardaSinLeer() {
        Negocio negocio = conNegocioVisible();
        when(consultaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ConsultaResponse respuesta = service.enviar(cliente, ID_NEGOCIO, peticion());

        assertFalse(respuesta.leida(), "el buzón es una lista de pendientes");
        assertNull(respuesta.fechaLectura());
        assertEquals("Carlos Rueda", respuesta.nombreCliente());
        ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
        verify(consultaRepository).save(captor.capture());
        assertEquals(negocio, captor.getValue().getNegocio());
    }

    @Test
    @DisplayName("El asunto y el mensaje se guardan sin espacios sobrantes")
    void enviar_recortaLosTextos() {
        conNegocioVisible();
        when(consultaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ConsultaResponse respuesta = service.enviar(cliente, ID_NEGOCIO, peticion());

        assertEquals("Reserva para 4 personas", respuesta.asunto());
        assertEquals("¿Tienen mesa el sábado a las 8?", respuesta.mensaje());
    }

    @Test
    @DisplayName("No se puede escribir a un negocio sin publicar (B6)")
    void enviar_negocioNoVisible_lanzaNoEncontrado() {
        when(negocioRepository.buscarVisibleEnDirectorio(ID_NEGOCIO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.enviar(cliente, ID_NEGOCIO, peticion()));
        verify(consultaRepository, never()).save(any());
    }

    @Test
    @DisplayName("El dueño no se escribe a su propio buzón")
    void enviar_aSuPropioNegocio_lanza() {
        conNegocioVisible();

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.enviar(duena, ID_NEGOCIO, peticion()));

        assertTrue(error.getMessage().contains("tu propio buzón"));
    }

    // ---------- El correo del cliente (D2) ----------

    @Test
    @DisplayName("El buzón enseña el correo del cliente, que es la única forma de responder")
    void buzon_llevaElCorreoDelCliente() {
        // Es la única respuesta de la API que lleva un correo, y es a propósito:
        // la plataforma no envía correos ni permite responder desde dentro.
        Negocio negocio = conNegocioPropio();
        when(consultaRepository.buscarEnBuzon(eq(ID_NEGOCIO), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(consulta(negocio))));

        var buzon = service.buzon(duena, null, Pageable.ofSize(20));

        assertEquals("carlos@gmail.com", buzon.getContent().getFirst().correoCliente());
    }

    @Test
    @DisplayName("El filtro de no leídas llega al repositorio tal cual")
    void buzon_soloNoLeidas_trasladaElFiltro() {
        conNegocioPropio();
        when(consultaRepository.buscarEnBuzon(eq(ID_NEGOCIO), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buzon(duena, false, Pageable.ofSize(20));

        verify(consultaRepository).buscarEnBuzon(eq(ID_NEGOCIO), eq(false), any());
    }

    @Test
    @DisplayName("Quien no tiene negocio no tiene buzón")
    void buzon_sinNegocio_lanzaNoEncontrado() {
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.buzon(duena, null, Pageable.ofSize(20)));
    }

    // ---------- Leídas y no leídas (D3) ----------

    @Test
    @DisplayName("Marcar como leída sella la fecha de lectura")
    void marcarLectura_leida_sellaLaFecha() {
        Negocio negocio = conNegocioPropio();
        Consulta consulta = consulta(negocio);
        when(consultaRepository.findByIdAndNegocioId(9L, ID_NEGOCIO))
                .thenReturn(Optional.of(consulta));
        when(consultaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ConsultaResponse respuesta = service.marcarLectura(duena, 9L, true);

        assertTrue(respuesta.leida());
        assertNotNull(respuesta.fechaLectura());
    }

    @Test
    @DisplayName("Devolverla al montón le quita también la fecha de lectura")
    void marcarLectura_noLeida_borraLaFecha() {
        // Dejar la fecha puesta diría que se abrió y se ignoró, que no es lo que
        // el dueño quiere decir al desmarcarla.
        Negocio negocio = conNegocioPropio();
        Consulta consulta = consulta(negocio);
        consulta.marcarLectura(true);
        when(consultaRepository.findByIdAndNegocioId(9L, ID_NEGOCIO))
                .thenReturn(Optional.of(consulta));
        when(consultaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ConsultaResponse respuesta = service.marcarLectura(duena, 9L, false);

        assertFalse(respuesta.leida());
        assertNull(respuesta.fechaLectura());
    }

    @Test
    @DisplayName("Listar el buzón no marca nada como leído")
    void buzon_noMarcaNadaComoLeido() {
        // Abrir la pantalla para echar un vistazo no es haber atendido lo que
        // hay dentro: el estado solo sirve si lo decide el dueño.
        Negocio negocio = conNegocioPropio();
        when(consultaRepository.buscarEnBuzon(eq(ID_NEGOCIO), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(consulta(negocio))));

        var buzon = service.buzon(duena, null, Pageable.ofSize(20));

        assertFalse(buzon.getContent().getFirst().leida());
        verify(consultaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una consulta de otro buzón se comporta como si no existiera")
    void marcarLectura_consultaAjena_lanzaNoEncontrado() {
        conNegocioPropio();
        when(consultaRepository.findByIdAndNegocioId(99L, ID_NEGOCIO))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.marcarLectura(duena, 99L, true));
    }
}
