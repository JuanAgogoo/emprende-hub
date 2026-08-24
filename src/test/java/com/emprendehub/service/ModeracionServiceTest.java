package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CambioPendiente;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.DecisionModeracion;
import com.emprendehub.model.Denuncia;
import com.emprendehub.model.EstadoDenuncia;
import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.MotivoDenuncia;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.RegistroModeracion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.TipoEventoModeracion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.CambioPendienteRepository;
import com.emprendehub.repository.DenunciaRepository;
import com.emprendehub.repository.FotoRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.RegistroModeracionRepository;
import com.emprendehub.repository.UsuarioRepository;
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
class ModeracionServiceTest {

    private static final String DESCRIPCION =
            "Panadería artesanal con recetas familiares de más de cincuenta años, "
                    + "pan de masa madre horneado cada mañana en horno de leña.";

    @Mock private NegocioRepository negocioRepository;
    @Mock private CambioPendienteRepository cambioRepository;
    @Mock private RegistroModeracionRepository logRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @Mock private FotoRepository fotoRepository;

    /**
     * La moderación delega en el servicio de fotos: aprobar un negocio o una
     * propuesta publica también las imágenes que esperaban revisión (B2).
     */
    @Mock private FotoService fotoService;

    @Mock private DenunciaRepository denunciaRepository;

    /**
     * Borrar una opinión se delega en su servicio: arrastra las demás denuncias
     * y obliga a recalcular el promedio del negocio.
     */
    @Mock private OpinionService opinionService;

    @InjectMocks private ModeracionService service;

    private Usuario admin() {
        Usuario a = new Usuario("Admin", "admin@emprendehub.co", "hash", Rol.ADMIN);
        a.setId(1L);
        return a;
    }

    private Negocio negocioPendiente() {
        Usuario duena = new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR);
        duena.setId(2L);
        CategoriaNegocio cat = new CategoriaNegocio("Gastronomía", "🍴");
        Ciudad ciudad = new Ciudad("Medellín");
        Negocio n = new Negocio(duena, "Panadería La Tradicional", DESCRIPCION, "3001234567",
                cat, ciudad, null, NivelPrecio.MEDIO);
        n.setId(7L);
        return n;
    }

    // ---------- Aprobar y rechazar ----------

    @Test
    @DisplayName("resolverNegocio: aprobar pasa a APROBADO y sella la fecha")
    void aprobar_pendiente_pasaAAprobado() {
        Negocio negocio = negocioPendiente();
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.resolverNegocio(7L, new DecisionModeracion.Aprobar(), admin());

        assertEquals("APROBADO", respuesta.estado());
        assertNotNull(negocio.getFechaAprobacion());
    }

    @Test
    @DisplayName("resolverNegocio: rechazar guarda el motivo, que el dueño lee en su panel")
    void rechazar_pendiente_guardaElMotivo() {
        Negocio negocio = negocioPendiente();
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.resolverNegocio(7L,
                new DecisionModeracion.Rechazar("La descripción es insuficiente"), admin());

        assertEquals("RECHAZADO", respuesta.estado());
        assertEquals("La descripción es insuficiente", respuesta.motivoRechazo());
        assertNull(negocio.getFechaAprobacion());
    }

    @Test
    @DisplayName("Un rechazo sin motivo ni se puede construir")
    void rechazar_sinMotivo_noSePuedeConstruir() {
        assertThrows(IllegalArgumentException.class,
                () -> new DecisionModeracion.Rechazar("   "));
    }

    @Test
    @DisplayName("resolverNegocio: solo se resuelven los pendientes")
    void resolverNegocio_yaAprobado_lanzaExcepcion() {
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocio));

        assertThrows(ReglaDeNegocioException.class,
                () -> service.resolverNegocio(7L, new DecisionModeracion.Aprobar(), admin()));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("resolverNegocio: un negocio inexistente lanza 404")
    void resolverNegocio_inexistente_lanzaExcepcion() {
        when(negocioRepository.findWithDetalleById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.resolverNegocio(99L, new DecisionModeracion.Aprobar(), admin()));
    }

    @Test
    @DisplayName("Aprobar deja constancia en el log con el administrador que lo hizo")
    void aprobar_registraEnElLog() {
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocioPendiente()));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.resolverNegocio(7L, new DecisionModeracion.Aprobar(), admin());

        ArgumentCaptor<RegistroModeracion> entrada =
                ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(logRepository).save(entrada.capture());
        assertEquals(TipoEventoModeracion.NEGOCIO_APROBADO, entrada.getValue().getTipo());
        assertEquals("admin@emprendehub.co", entrada.getValue().getAdministrador());
    }

    @Test
    @DisplayName("Rechazar registra el motivo también en el log")
    void rechazar_registraElMotivoEnElLog() {
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocioPendiente()));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.resolverNegocio(7L, new DecisionModeracion.Rechazar("Faltan datos"), admin());

        ArgumentCaptor<RegistroModeracion> entrada =
                ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(logRepository).save(entrada.capture());
        assertEquals(TipoEventoModeracion.NEGOCIO_RECHAZADO, entrada.getValue().getTipo());
        assertEquals("Faltan datos", entrada.getValue().getDetalle());
    }

    // ---------- Cambios pendientes ----------

    @Test
    @DisplayName("resolverCambio: aprobar copia los valores propuestos al negocio")
    void aprobarCambio_copiaLosValores() {
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        CambioPendiente cambio = new CambioPendiente(negocio, "Panadería Nueva", DESCRIPCION);
        when(cambioRepository.findByNegocioId(7L)).thenReturn(Optional.of(cambio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.resolverCambio(7L, new DecisionModeracion.Aprobar(), admin());

        assertEquals("Panadería Nueva", respuesta.nombre());
        verify(cambioRepository).delete(cambio);
    }

    @Test
    @DisplayName("resolverCambio: rechazar descarta la propuesta sin tocar el negocio")
    void rechazarCambio_noTocaElNegocio() {
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        CambioPendiente cambio = new CambioPendiente(negocio, "Nombre Rechazado", DESCRIPCION);
        when(cambioRepository.findByNegocioId(7L)).thenReturn(Optional.of(cambio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.resolverCambio(7L,
                new DecisionModeracion.Rechazar("No procede"), admin());

        assertEquals("Panadería La Tradicional", respuesta.nombre());
        verify(cambioRepository).delete(cambio);
    }

    @Test
    @DisplayName("resolverCambio: sin propuesta viva lanza 404")
    void resolverCambio_sinPropuesta_lanzaExcepcion() {
        when(cambioRepository.findByNegocioId(7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.resolverCambio(7L, new DecisionModeracion.Aprobar(), admin()));
    }

    // ---------- Fotos que esperan revisión (B2, B9) ----------

    @Test
    @DisplayName("Aprobar el negocio publica también las fotos que subió al registrarse")
    void aprobarNegocio_publicaSusFotos() {
        Negocio negocio = negocioPendiente();
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.resolverNegocio(7L, new DecisionModeracion.Aprobar(), admin());

        // Nadie las revisó por separado: el negocio entero estaba sin revisar.
        verify(fotoService).aprobarPendientes(7L);
    }

    @Test
    @DisplayName("Rechazar el negocio no toca sus fotos: seguirá corrigiendo y reenviando")
    void rechazarNegocio_noTocaLasFotos() {
        Negocio negocio = negocioPendiente();
        when(negocioRepository.findWithDetalleById(7L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.resolverNegocio(7L, new DecisionModeracion.Rechazar("Faltan datos"), admin());

        verify(fotoService, never()).aprobarPendientes(any());
        verify(fotoService, never()).descartarPendientes(any());
    }

    @Test
    @DisplayName("Aprobar la propuesta publica las fotos nuevas (B2-bis)")
    void aprobarCambio_publicaLasFotos() {
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        when(cambioRepository.findByNegocioId(7L)).thenReturn(
                Optional.of(new CambioPendiente(negocio, "Panadería Renovada", DESCRIPCION)));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.resolverCambio(7L, new DecisionModeracion.Aprobar(), admin());

        verify(fotoService).aprobarPendientes(7L);
    }

    @Test
    @DisplayName("Rechazar la propuesta descarta las fotos que no se aceptaron")
    void rechazarCambio_descartaLasFotos() {
        // Si se quedaran pendientes, la siguiente propuesta del dueño las
        // publicaría de rebote, que es justo lo que el administrador impidió.
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        when(cambioRepository.findByNegocioId(7L)).thenReturn(
                Optional.of(new CambioPendiente(negocio, "Panadería Renovada", DESCRIPCION)));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.resolverCambio(7L, new DecisionModeracion.Rechazar("No se ve el local"), admin());

        verify(fotoService).descartarPendientes(7L);
        verify(fotoService, never()).aprobarPendientes(any());
    }

    // ---------- La cola de propuestas ----------

    @Test
    @DisplayName("La cola enseña el valor actual junto al propuesto y cuántas fotos esperan")
    void cambiosPendientes_comparaActualYPropuesto() {
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        when(cambioRepository.findAllByOrderByFechaSolicitudAsc()).thenReturn(
                List.of(new CambioPendiente(negocio, "Panadería Renovada", DESCRIPCION)));
        when(fotoRepository.countByNegocioIdAndEstado(7L, EstadoFoto.PENDIENTE)).thenReturn(2);

        var cola = service.cambiosPendientes();

        assertEquals(1, cola.size());
        assertEquals("Panadería La Tradicional", cola.getFirst().nombreActual());
        assertEquals("Panadería Renovada", cola.getFirst().nombrePropuesto());
        assertEquals(2, cola.getFirst().fotosPendientes());
    }

    @Test
    @DisplayName("Sin propuestas vivas la cola llega vacía")
    void cambiosPendientes_sinNada_devuelveVacio() {
        when(cambioRepository.findAllByOrderByFechaSolicitudAsc()).thenReturn(List.of());

        assertTrue(service.cambiosPendientes().isEmpty());
    }

    // ---------- Opiniones denunciadas (C3, C4) ----------

    private Opinion opinionDenunciada() {
        Negocio negocio = negocioPendiente();
        negocio.setEstado(EstadoNegocio.APROBADO);
        Usuario autor = new Usuario("Carlos Rueda", "carlos@test.co", "hash", Rol.CLIENTE);
        autor.setId(3L);
        Opinion opinion = new Opinion(negocio, autor, 1, "Texto denunciable");
        opinion.setId(11L);
        return opinion;
    }

    private Denuncia denuncia(Opinion opinion) {
        Usuario denunciante = new Usuario("Sofía", "sofia@test.co", "hash", Rol.CLIENTE);
        denunciante.setId(5L);
        Denuncia denuncia = new Denuncia(opinion, denunciante,
                MotivoDenuncia.LENGUAJE_INAPROPIADO);
        denuncia.setId(21L);
        return denuncia;
    }

    @Test
    @DisplayName("La cola trae el texto denunciado, de quién es y sobre qué negocio")
    void denunciasPendientes_traeLoNecesarioParaDecidir() {
        Denuncia denuncia = denuncia(opinionDenunciada());
        when(denunciaRepository.findByEstadoOrderByFechaAsc(
                eq(EstadoDenuncia.PENDIENTE), any()))
                .thenReturn(new PageImpl<>(List.of(denuncia)));

        var cola = service.denunciasPendientes(Pageable.ofSize(20));

        var primera = cola.getContent().getFirst();
        assertEquals("Texto denunciable", primera.comentario());
        assertEquals("Carlos Rueda", primera.autorOpinion());
        assertEquals("Panadería La Tradicional", primera.negocio());
        assertEquals("Lenguaje inapropiado", primera.motivoDescripcion());
    }

    @Test
    @DisplayName("Eliminar la opinión denunciada la borra y lo deja en el log con su motivo")
    void eliminarOpinion_borraYRegistra() {
        Opinion opinion = opinionDenunciada();
        when(denunciaRepository.findWithDetalleById(21L))
                .thenReturn(Optional.of(denuncia(opinion)));

        service.eliminarOpinionDenunciada(21L, "Insultos al personal", admin());

        // El borrado se delega: arrastra denuncias y recalcula el promedio.
        verify(opinionService).eliminarPorModeracion(opinion);
        ArgumentCaptor<RegistroModeracion> captor =
                ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(logRepository).save(captor.capture());
        assertEquals(TipoEventoModeracion.OPINION_ELIMINADA, captor.getValue().getTipo());
        assertEquals("Insultos al personal", captor.getValue().getDetalle());
    }

    @Test
    @DisplayName("Borrar una opinión sin motivo no se permite")
    void eliminarOpinion_sinMotivo_lanza() {
        // Es la acción menos reversible del panel: sin una razón escrita no
        // habría forma de explicar después por qué se hizo.
        assertThrows(ReglaDeNegocioException.class,
                () -> service.eliminarOpinionDenunciada(21L, "   ", admin()));

        verify(opinionService, never()).eliminarPorModeracion(any());
    }

    @Test
    @DisplayName("Desestimar deja la opinión publicada y la denuncia como revisada")
    void desestimar_cambiaElEstadoYRegistra() {
        Denuncia denuncia = denuncia(opinionDenunciada());
        when(denunciaRepository.findWithDetalleById(21L)).thenReturn(Optional.of(denuncia));

        service.desestimarDenuncia(21L, admin());

        assertEquals(EstadoDenuncia.DESESTIMADA, denuncia.getEstado());
        verify(opinionService, never()).eliminarPorModeracion(any());
        ArgumentCaptor<RegistroModeracion> captor =
                ArgumentCaptor.forClass(RegistroModeracion.class);
        verify(logRepository).save(captor.capture());
        assertEquals(TipoEventoModeracion.DENUNCIA_DESESTIMADA, captor.getValue().getTipo());
    }

    @Test
    @DisplayName("Una denuncia ya resuelta no se vuelve a desestimar")
    void desestimar_yaResuelta_lanza() {
        Denuncia denuncia = denuncia(opinionDenunciada());
        denuncia.setEstado(EstadoDenuncia.DESESTIMADA);
        when(denunciaRepository.findWithDetalleById(21L)).thenReturn(Optional.of(denuncia));

        assertThrows(ReglaDeNegocioException.class,
                () -> service.desestimarDenuncia(21L, admin()));
    }

    @Test
    @DisplayName("Una denuncia que no existe devuelve no encontrado")
    void resolver_denunciaInexistente_lanzaNoEncontrado() {
        when(denunciaRepository.findWithDetalleById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.desestimarDenuncia(99L, admin()));
    }

    // ---------- Cuentas ----------

    @Test
    @DisplayName("suspender: desactiva la cuenta y lo registra")
    void suspender_cuentaActiva_laDesactiva() {
        Usuario maria = new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR);
        maria.setId(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(maria));

        service.suspender(2L, admin());

        assertFalse(maria.isActivo());
        verify(usuarioRepository).save(maria);
        verify(logRepository).save(any(RegistroModeracion.class));
    }

    @Test
    @DisplayName("suspender: el administrador no puede suspenderse a sí mismo")
    void suspender_aSiMismo_lanzaExcepcion() {
        Usuario admin = admin();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        var excepcion = assertThrows(ReglaDeNegocioException.class,
                () -> service.suspender(1L, admin));

        assertTrue(excepcion.getMessage().contains("sí mismo"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("suspender: una cuenta ya suspendida se rechaza")
    void suspender_yaSuspendida_lanzaExcepcion() {
        Usuario maria = new Usuario("María", "maria@test.co", "hash", Rol.CLIENTE);
        maria.setId(2L);
        maria.setActivo(false);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(maria));

        assertThrows(ReglaDeNegocioException.class, () -> service.suspender(2L, admin()));
    }

    @Test
    @DisplayName("reactivar: vuelve a habilitar la cuenta")
    void reactivar_cuentaSuspendida_laActiva() {
        Usuario maria = new Usuario("María", "maria@test.co", "hash", Rol.CLIENTE);
        maria.setId(2L);
        maria.setActivo(false);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(maria));

        service.reactivar(2L, admin());

        assertTrue(maria.isActivo());
        verify(logRepository).save(any(RegistroModeracion.class));
    }

    @Test
    @DisplayName("reactivar: una cuenta que ya estaba activa se rechaza")
    void reactivar_yaActiva_lanzaExcepcion() {
        Usuario maria = new Usuario("María", "maria@test.co", "hash", Rol.CLIENTE);
        maria.setId(2L);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(maria));

        assertThrows(ReglaDeNegocioException.class, () -> service.reactivar(2L, admin()));
    }

    @Test
    @DisplayName("suspender: un usuario inexistente lanza 404")
    void suspender_inexistente_lanzaExcepcion() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.suspender(99L, admin()));
    }
}
