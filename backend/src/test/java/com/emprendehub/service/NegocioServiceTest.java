package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.BarrioRepository;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.CiudadRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NegocioServiceTest {

    private static final String DESCRIPCION_VALIDA =
            "Panadería artesanal con recetas familiares de más de cincuenta años, "
                    + "pan de masa madre horneado cada mañana en horno de leña.";

    @Mock private NegocioRepository negocioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CategoriaNegocioRepository categoriaRepository;
    @Mock private CiudadRepository ciudadRepository;
    @Mock private BarrioRepository barrioRepository;
    @Mock private com.emprendehub.repository.CambioPendienteRepository cambioRepository;

    /** A mano: el constructor lleva el interruptor de moderación (un boolean). */
    private NegocioService service;

    @BeforeEach
    void prepararConModeracion() {
        service = servicioCon(false);
    }

    private NegocioService servicioCon(boolean moderacionAutomatica) {
        return new NegocioService(negocioRepository, usuarioRepository, categoriaRepository,
                ciudadRepository, barrioRepository, cambioRepository, moderacionAutomatica);
    }

    private Usuario cliente() {
        Usuario u = new Usuario("María García", "maria@gmail.com", "hash", Rol.CLIENTE);
        u.setId(1L);
        return u;
    }

    private Ciudad medellin() {
        Ciudad c = new Ciudad("Medellín");
        c.setId(10L);
        return c;
    }

    private CategoriaNegocio gastronomia() {
        CategoriaNegocio c = new CategoriaNegocio("Gastronomía", "🍴");
        c.setId(5L);
        return c;
    }

    private RegistrarNegocioRequest peticion(Long barrioId) {
        return new RegistrarNegocioRequest("Panadería La Tradicional", DESCRIPCION_VALIDA,
                "3001234567", 5L, 10L, barrioId, NivelPrecio.MEDIO);
    }

    private void prepararCatalogos() {
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(gastronomia()));
        when(ciudadRepository.findById(10L)).thenReturn(Optional.of(medellin()));
    }

    // ---------- Alta correcta ----------

    @Test
    @DisplayName("registrar: el negocio nace en estado PENDIENTE")
    void registrar_valido_naceEnPendiente() {
        prepararCatalogos();
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        NegocioResponse respuesta = service.registrar(cliente(), peticion(null));

        assertEquals("PENDIENTE", respuesta.estado());
        assertEquals("Panadería La Tradicional", respuesta.nombre());
    }

    @Test
    @DisplayName("Sin moderación el negocio nace publicado y con su fecha sellada")
    void registrar_sinModeracion_naceAprobado() {
        // El interruptor es provisional: la regla del dominio sigue siendo B6, y
        // la prueba de arriba, que corre con moderación, es la que lo fija.
        NegocioService sinModeracion = servicioCon(true);
        prepararCatalogos();
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        NegocioResponse respuesta = sinModeracion.registrar(cliente(), peticion(null));

        assertEquals("APROBADO", respuesta.estado());

        // La fecha se comprueba sobre la entidad guardada porque la respuesta no
        // la lleva; importa porque de ella depende el orden RECIENTES (G7).
        ArgumentCaptor<Negocio> guardado = ArgumentCaptor.forClass(Negocio.class);
        verify(negocioRepository).save(guardado.capture());
        assertNotNull(guardado.getValue().getFechaAprobacion());
    }

    @Test
    @DisplayName("registrar: un cliente pasa a emprendedor conservando su cuenta")
    void registrar_cliente_asciendeAEmprendedor() {
        prepararCatalogos();
        Usuario maria = cliente();
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        service.registrar(maria, peticion(null));

        assertEquals(Rol.EMPRENDEDOR, maria.getRol());
        verify(usuarioRepository).save(maria);
    }

    @Test
    @DisplayName("registrar: sin barrio queda a null, porque no todas las ciudades tienen")
    void registrar_sinBarrio_quedaANulo() {
        prepararCatalogos();
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        assertNull(service.registrar(cliente(), peticion(null)).barrio());
        verify(barrioRepository, never()).findById(any());
    }

    @Test
    @DisplayName("registrar: un barrio de la misma ciudad se acepta")
    void registrar_barrioDeLaCiudad_seAcepta() {
        prepararCatalogos();
        Ciudad medellin = medellin();
        Barrio poblado = new Barrio("El Poblado");
        poblado.setId(20L);
        poblado.setCiudad(medellin);
        when(barrioRepository.findById(20L)).thenReturn(Optional.of(poblado));
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("El Poblado", service.registrar(cliente(), peticion(20L)).barrio());
    }

    @Test
    @DisplayName("registrar: sin opiniones la calificación es null, no cero")
    void registrar_sinOpiniones_calificacionEsNula() {
        prepararCatalogos();
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        NegocioResponse respuesta = service.registrar(cliente(), peticion(null));

        assertNull(respuesta.calificacionPromedio());
        assertEquals(0, respuesta.numeroOpiniones());
    }

    // ---------- Reglas que impiden el alta ----------

    @Test
    @DisplayName("registrar: el administrador no puede tener negocio")
    void registrar_admin_lanzaExcepcion() {
        Usuario admin = new Usuario("Admin", "admin@test.co", "hash", Rol.ADMIN);
        admin.setId(9L);

        var excepcion = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(admin, peticion(null)));

        assertTrue(excepcion.getMessage().contains("administrador"));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("registrar: una cuenta que ya tiene negocio no puede abrir otro")
    void registrar_yaTieneNegocio_lanzaExcepcion() {
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(true);

        var excepcion = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(cliente(), peticion(null)));

        assertTrue(excepcion.getMessage().contains("ya tiene un negocio"));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("registrar: un barrio de otra ciudad se rechaza")
    void registrar_barrioDeOtraCiudad_lanzaExcepcion() {
        prepararCatalogos();
        Ciudad envigado = new Ciudad("Envigado");
        envigado.setId(11L);
        Barrio ajeno = new Barrio("Zona Sur");
        ajeno.setId(30L);
        ajeno.setCiudad(envigado);
        when(barrioRepository.findById(30L)).thenReturn(Optional.of(ajeno));
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);

        var excepcion = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrar(cliente(), peticion(30L)));

        assertTrue(excepcion.getMessage().contains("no pertenece"));
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("registrar: una categoría inexistente lanza 404")
    void registrar_categoriaInexistente_lanzaExcepcion() {
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(cliente(), peticion(null)));
    }

    @Test
    @DisplayName("registrar: una ciudad inexistente lanza 404")
    void registrar_ciudadInexistente_lanzaExcepcion() {
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(gastronomia()));
        when(ciudadRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(cliente(), peticion(null)));
    }

    @Test
    @DisplayName("registrar: un barrio inexistente lanza 404")
    void registrar_barrioInexistente_lanzaExcepcion() {
        prepararCatalogos();
        when(negocioRepository.existsByUsuarioId(1L)).thenReturn(false);
        when(barrioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registrar(cliente(), peticion(99L)));
    }

    // ---------- Consulta del propio negocio ----------

    @Test
    @DisplayName("obtenerElMio: el dueño ve su negocio aunque esté pendiente")
    void obtenerElMio_pendiente_loDevuelve() {
        Negocio negocio = new Negocio(cliente(), "Panadería", DESCRIPCION_VALIDA, "3001234567",
                gastronomia(), medellin(), null, NivelPrecio.MEDIO);
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));

        assertEquals("PENDIENTE", service.obtenerElMio(cliente()).estado());
    }

    @Test
    @DisplayName("obtenerElMio: quien no tiene negocio recibe un 404")
    void obtenerElMio_sinNegocio_lanzaExcepcion() {
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerElMio(cliente()));
    }

    // ---------- Edición por el dueño (B1 y B2-bis) ----------

    private Negocio negocioAprobado() {
        Negocio n = new Negocio(cliente(), "Panadería La Tradicional", DESCRIPCION_VALIDA,
                "3001234567", gastronomia(), medellin(), null, NivelPrecio.MEDIO);
        n.setId(7L);
        n.setEstado(com.emprendehub.model.EstadoNegocio.APROBADO);
        return n;
    }

    private com.emprendehub.dto.EditarNegocioPublicoRequest edicion(String nombre) {
        return new com.emprendehub.dto.EditarNegocioPublicoRequest(nombre, DESCRIPCION_VALIDA);
    }

    @Test
    @DisplayName("proponerCambioPublico: no toca el negocio, guarda la propuesta aparte")
    void proponerCambio_noTocaElNegocio() {
        Negocio negocio = negocioAprobado();
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));
        when(cambioRepository.findByNegocioId(7L)).thenReturn(Optional.empty());

        NegocioResponse respuesta = service.proponerCambioPublico(cliente(), edicion("Nombre Nuevo"));

        // El negocio sigue publicado con su nombre anterior: el público no se entera.
        assertEquals("Panadería La Tradicional", respuesta.nombre());
        assertEquals("APROBADO", respuesta.estado());
        verify(cambioRepository).save(any());
        verify(negocioRepository, never()).save(any(Negocio.class));
    }

    @Test
    @DisplayName("proponerCambioPublico: una propuesta nueva sustituye a la anterior")
    void proponerCambio_conPropuestaViva_laSustituye() {
        Negocio negocio = negocioAprobado();
        var existente = new com.emprendehub.model.CambioPendiente(
                negocio, "Nombre Viejo", DESCRIPCION_VALIDA);
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));
        when(cambioRepository.findByNegocioId(7L)).thenReturn(Optional.of(existente));

        service.proponerCambioPublico(cliente(), edicion("Nombre Nuevo"));

        assertEquals("Nombre Nuevo", existente.getNombrePropuesto());
    }

    @Test
    @DisplayName("proponerCambioPublico: un negocio pendiente no propone cambios")
    void proponerCambio_negocioPendiente_lanzaExcepcion() {
        Negocio negocio = negocioAprobado();
        negocio.setEstado(com.emprendehub.model.EstadoNegocio.PENDIENTE);
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));

        assertThrows(ReglaDeNegocioException.class,
                () -> service.proponerCambioPublico(cliente(), edicion("X")));
    }

    @Test
    @DisplayName("actualizarContacto: el teléfono se aplica al instante, sin revisión")
    void actualizarContacto_seAplicaAlInstante() {
        Negocio negocio = negocioAprobado();
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.actualizarContacto(cliente(),
                new com.emprendehub.dto.EditarContactoRequest("6044440000"));

        assertEquals("6044440000", respuesta.telefono());
        assertEquals("APROBADO", respuesta.estado());
        verify(cambioRepository, never()).save(any());
    }

    // ---------- Redes sociales (B8) ----------

    @Test
    @DisplayName("actualizarRedes: los enlaces se aplican al instante, sin revisión")
    void actualizarRedes_seAplicanAlInstante() {
        Negocio negocio = negocioAprobado();
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.actualizarRedes(cliente(),
                new com.emprendehub.dto.EditarRedesRequest(
                        "https://instagram.com/panaderia",
                        "https://linkedin.com/company/panaderia"));

        assertEquals("https://instagram.com/panaderia", respuesta.instagram());
        assertEquals("https://linkedin.com/company/panaderia", respuesta.linkedin());
        assertEquals("APROBADO", respuesta.estado());
        // Son datos de contacto, no contenido que el administrador revise (B2).
        verify(cambioRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarRedes: dejar un campo en blanco es quitar la red")
    void actualizarRedes_enBlanco_quedaNula() {
        Negocio negocio = negocioAprobado();
        negocio.setInstagram("https://instagram.com/antigua");
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.actualizarRedes(cliente(),
                new com.emprendehub.dto.EditarRedesRequest("   ", null));

        assertNull(respuesta.instagram());
        assertNull(respuesta.linkedin());
    }

    @Test
    @DisplayName("actualizarRedes: quien no tiene negocio no tiene redes que cambiar")
    void actualizarRedes_sinNegocio_lanzaNoEncontrado() {
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarRedes(cliente(),
                new com.emprendehub.dto.EditarRedesRequest(null, null)));
    }

    @Test
    @DisplayName("corregirYReenviar: un rechazado vuelve a PENDIENTE y pierde el motivo")
    void corregirYReenviar_rechazado_vuelveAPendiente() {
        Negocio negocio = negocioAprobado();
        negocio.setEstado(com.emprendehub.model.EstadoNegocio.RECHAZADO);
        negocio.setMotivoRechazo("La descripción es insuficiente");
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocio));
        when(negocioRepository.save(any(Negocio.class))).thenAnswer(i -> i.getArgument(0));

        var respuesta = service.corregirYReenviar(cliente(), edicion("Panadería Corregida"));

        assertEquals("PENDIENTE", respuesta.estado());
        assertEquals("Panadería Corregida", respuesta.nombre());
        assertNull(respuesta.motivoRechazo());
    }

    @Test
    @DisplayName("corregirYReenviar: solo se reenvía lo rechazado")
    void corregirYReenviar_aprobado_lanzaExcepcion() {
        when(negocioRepository.findByUsuarioId(1L)).thenReturn(Optional.of(negocioAprobado()));

        assertThrows(ReglaDeNegocioException.class,
                () -> service.corregirYReenviar(cliente(), edicion("X")));
    }
}
