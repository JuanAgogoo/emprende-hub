package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.BusquedaDirectorioRequest;
import com.emprendehub.dto.NegocioPublicoResponse;
import com.emprendehub.dto.PerfilNegocioResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Foto;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Producto;
import com.emprendehub.model.OrdenDirectorio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.FotoRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.ProductoRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class DirectorioServiceTest {

    @Mock
    private NegocioRepository repositorio;

    @Mock
    private FotoRepository fotoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private VisitaService visitaService;

    @InjectMocks
    private DirectorioService service;

    private static final BusquedaDirectorioRequest SIN_FILTROS =
            new BusquedaDirectorioRequest(null, null, null, null, null, null);

    private Negocio negocioAprobado() {
        Ciudad medellin = new Ciudad("Medellín");
        Barrio poblado = new Barrio("El Poblado");
        poblado.setCiudad(medellin);

        Negocio negocio = new Negocio(
                new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR),
                "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.",
                "3001234567", new CategoriaNegocio("Gastronomía", "🍴"),
                medellin, poblado, NivelPrecio.MEDIO);
        negocio.setId(7L);
        negocio.setEstado(EstadoNegocio.APROBADO);
        negocio.setFechaAprobacion(Instant.parse("2026-08-01T10:00:00Z"));
        negocio.setCalificacionPromedio(new BigDecimal("4.80"));
        negocio.setNumeroOpiniones(12);
        negocio.setMotivoRechazo("un rechazo antiguo que el público no debe ver");
        negocio.setInstagram("https://instagram.com/panaderia");
        return negocio;
    }

    private Foto foto(Negocio negocio, String archivo, int orden) {
        Foto foto = new Foto(negocio, archivo, orden);
        foto.setEstado(EstadoFoto.APROBADA);
        return foto;
    }

    /** Ejecuta una búsqueda y devuelve el {@code Pageable} que llegó al repositorio. */
    private Pageable capturarPagina(OrdenDirectorio orden, Pageable pedida) {
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscar(SIN_FILTROS, orden, pedida);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarEnDirectorio(any(), any(), any(), any(), any(), any(),
                captor.capture());
        return captor.getValue();
    }

    // ---------- Ordenación ----------

    @Test
    @DisplayName("Sin criterio se ordena por calificación, con los nulos al final")
    void buscar_sinOrden_ordenaPorCalificacionConNulosAlFinal() {
        Sort orden = capturarPagina(null, Pageable.ofSize(12)).getSort();

        Sort.Order calificacion = orden.getOrderFor("calificacionPromedio");
        assertEquals(Sort.Direction.DESC, calificacion.getDirection());
        assertEquals(Sort.NullHandling.NULLS_LAST, calificacion.getNullHandling(),
                "Un negocio sin opiniones no tiene calificación (C5) y no puede "
                        + "encabezar «Mejor calificados» justamente por eso");
        assertEquals(Sort.Direction.DESC, orden.getOrderFor("numeroOpiniones").getDirection());
    }

    @Test
    @DisplayName("«Nombre A-Z» ordena alfabéticamente")
    void buscar_ordenNombre_ordenaAlfabeticamente() {
        Sort orden = capturarPagina(OrdenDirectorio.NOMBRE, Pageable.ofSize(12)).getSort();

        assertEquals(Sort.Direction.ASC, orden.getOrderFor("nombre").getDirection());
        assertNull(orden.getOrderFor("calificacionPromedio"));
    }

    @Test
    @DisplayName("«Más recientes» ordena por fecha de aprobación, no de creación (G7)")
    void buscar_ordenRecientes_usaLaFechaDeAprobacion() {
        Sort orden = capturarPagina(OrdenDirectorio.RECIENTES, Pageable.ofSize(12)).getSort();

        assertEquals(Sort.Direction.DESC, orden.getOrderFor("fechaAprobacion").getDirection());
        assertNull(orden.getOrderFor("fechaCreacion"),
                "Ordenar por fecha de creación pondría delante a un negocio que "
                        + "lleva semanas esperando aprobación");
    }

    @Test
    @DisplayName("La ordenación que pida el cliente en el Pageable se descarta")
    void buscar_conOrdenacionEnElPageable_laIgnora() {
        // El criterio sale del enum, no de un nombre de columna enviado por el
        // cliente: así nadie ordena el directorio por una columna arbitraria.
        Pageable pedida = PageRequest.of(2, 5, Sort.by("motivoRechazo"));

        Pageable usada = capturarPagina(OrdenDirectorio.NOMBRE, pedida);

        assertNull(usada.getSort().getOrderFor("motivoRechazo"));
        assertEquals(2, usada.getPageNumber());
        assertEquals(5, usada.getPageSize());
    }

    // ---------- Filtros ----------

    @Test
    @DisplayName("Un texto en blanco no filtra: se trata como ausente")
    void buscar_textoEnBlanco_seTrataComoNulo() {
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscar(new BusquedaDirectorioRequest("   ", null, null, null, null, null),
                null, Pageable.ofSize(12));

        verify(repositorio).buscarEnDirectorio(isNull(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Los filtros llegan al repositorio tal como se piden")
    void buscar_conFiltros_losTrasladaAlRepositorio() {
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscar(new BusquedaDirectorioRequest("  pan  ", 3L, 1L, 2L,
                new BigDecimal("4.0"), NivelPrecio.MEDIO), null, Pageable.ofSize(12));

        verify(repositorio).buscarEnDirectorio(eq("pan"), eq(3L), eq(1L), eq(2L),
                eq(new BigDecimal("4.0")), eq(NivelPrecio.MEDIO), any());
    }

    // ---------- Lo que ve el público ----------

    @Test
    @DisplayName("La respuesta pública no lleva estado ni motivo de rechazo")
    void buscar_respuestaPublica_omiteLoQueEsDelDuenoYDelAdmin() {
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(negocioAprobado())));

        NegocioPublicoResponse respuesta =
                service.buscar(SIN_FILTROS, null, Pageable.ofSize(12)).getContent().getFirst();

        // El record no tiene esos campos: la comprobación es que la vista
        // pública lleva lo que debe y nada más.
        assertEquals(7L, respuesta.id());
        assertEquals("Panadería La Tradicional", respuesta.nombre());
        assertEquals("Gastronomía", respuesta.categoria());
        assertEquals("Medellín", respuesta.ciudad());
        assertEquals("El Poblado", respuesta.barrio());
        assertEquals("MEDIO", respuesta.nivelPrecio());
        assertEquals(new BigDecimal("4.80"), respuesta.calificacionPromedio());
        assertEquals(12, respuesta.numeroOpiniones());
        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), respuesta.fechaAprobacion());
    }

    @Test
    @DisplayName("Un negocio sin barrio se mapea con el barrio a nulo")
    void buscar_negocioSinBarrio_noRevienta() {
        Negocio negocio = negocioAprobado();
        negocio.setBarrio(null);
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(negocio)));

        NegocioPublicoResponse respuesta =
                service.buscar(SIN_FILTROS, null, Pageable.ofSize(12)).getContent().getFirst();

        assertNull(respuesta.barrio());
    }

    // ---------- Portada de la tarjeta (B9) ----------

    @Test
    @DisplayName("La tarjeta lleva como portada la primera foto aprobada (B9)")
    void buscar_tarjeta_llevaLaPrimeraFotoAprobada() {
        Negocio negocio = negocioAprobado();
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(negocio)));
        when(fotoRepository.findByNegocioIdInAndEstadoOrderByNegocioIdAscOrdenAsc(
                List.of(7L), EstadoFoto.APROBADA))
                .thenReturn(List.of(foto(negocio, "portada.jpg", 0),
                        foto(negocio, "segunda.jpg", 1)));

        NegocioPublicoResponse tarjeta =
                service.buscar(SIN_FILTROS, null, Pageable.ofSize(12)).getContent().getFirst();

        assertEquals("/fotos/portada.jpg", tarjeta.fotoPrincipal());
    }

    @Test
    @DisplayName("Un negocio sin fotos aprobadas sale con la portada a nulo")
    void buscar_sinFotos_portadaNula() {
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(negocioAprobado())));
        when(fotoRepository.findByNegocioIdInAndEstadoOrderByNegocioIdAscOrdenAsc(
                any(), any())).thenReturn(List.of());

        assertNull(service.buscar(SIN_FILTROS, null, Pageable.ofSize(12))
                .getContent().getFirst().fotoPrincipal());
    }

    @Test
    @DisplayName("Una página vacía no pregunta por portadas")
    void buscar_paginaVacia_noPideFotos() {
        when(repositorio.buscarEnDirectorio(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscar(SIN_FILTROS, null, Pageable.ofSize(12));

        verify(fotoRepository, never())
                .findByNegocioIdInAndEstadoOrderByNegocioIdAscOrdenAsc(any(), any());
    }

    // ---------- Destacados (C7) ----------

    @Test
    @DisplayName("Destacados exige cinco opiniones y devuelve seis por defecto (C7)")
    void destacados_sinLimite_pideCincoOpinionesYSeisNegocios() {
        when(repositorio.buscarDestacados(eq(5), any())).thenReturn(List.of(negocioAprobado()));

        List<NegocioPublicoResponse> destacados = service.destacados(null);

        assertEquals(1, destacados.size());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarDestacados(eq(5), captor.capture());
        assertEquals(6, captor.getValue().getPageSize());
        assertTrue(captor.getValue().getSort().isUnsorted(),
                "El orden va escrito en la consulta, no en el Pageable");
    }

    @Test
    @DisplayName("Un límite desmedido se recorta a doce en vez de fallar")
    void destacados_limiteExcesivo_seRecorta() {
        when(repositorio.buscarDestacados(eq(5), any())).thenReturn(List.of());

        service.destacados(500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarDestacados(eq(5), captor.capture());
        assertEquals(12, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("Un límite de cero o negativo se sube a uno: PageRequest no admite cero")
    void destacados_limiteCero_seSubeAUno() {
        when(repositorio.buscarDestacados(eq(5), any())).thenReturn(List.of());

        service.destacados(0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarDestacados(eq(5), captor.capture());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("Un límite razonable se respeta")
    void destacados_limiteRazonable_seRespeta() {
        when(repositorio.buscarDestacados(eq(5), any())).thenReturn(List.of());

        service.destacados(3);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarDestacados(eq(5), captor.capture());
        assertEquals(3, captor.getValue().getPageSize());
    }

    // ---------- Perfil público (B6) ----------

    @Test
    @DisplayName("El perfil de un negocio visible trae su galería y su escaparate")
    void perfilPublico_visible_loDevuelve() {
        Negocio negocio = negocioAprobado();
        when(repositorio.buscarVisibleEnDirectorio(7L)).thenReturn(Optional.of(negocio));
        when(fotoRepository.findByNegocioIdAndEstadoOrderByOrdenAsc(7L, EstadoFoto.APROBADA))
                .thenReturn(List.of(foto(negocio, "portada.jpg", 0)));
        when(productoRepository.findByNegocioIdOrderByNombreAsc(7L))
                .thenReturn(List.of(new Producto(negocio, "Pan de masa madre",
                        new BigDecimal("12000"), null, true, "pan.jpg")));

        PerfilNegocioResponse perfil = service.obtenerPerfilPublico(7L);

        assertEquals("Panadería La Tradicional", perfil.nombre());
        assertEquals("/fotos/portada.jpg", perfil.fotos().getFirst().url());
        assertTrue(perfil.fotos().getFirst().principal());
        assertEquals("Pan de masa madre", perfil.productos().getFirst().nombre());
        assertEquals("https://instagram.com/panaderia", perfil.instagram());
    }

    @Test
    @DisplayName("El perfil del público nunca enseña una foto sin revisar (B2)")
    void perfilPublico_soloPideLasAprobadas() {
        Negocio negocio = negocioAprobado();
        when(repositorio.buscarVisibleEnDirectorio(7L)).thenReturn(Optional.of(negocio));

        service.obtenerPerfilPublico(7L);

        verify(fotoRepository).findByNegocioIdAndEstadoOrderByOrdenAsc(7L, EstadoFoto.APROBADA);
    }

    @Test
    @DisplayName("Un negocio no visible se comporta como si no existiera (B6)")
    void perfilPublico_noVisible_lanzaNoEncontrado() {
        // El repositorio ya devuelve vacío tanto si no existe como si está
        // pendiente, rechazado o suspendido: aquí los cuatro casos son 404, no
        // 403, para no filtrar qué hay sin publicar.
        when(repositorio.buscarVisibleEnDirectorio(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPerfilPublico(99L));
    }

    // ---------- Perfil con registro de visita (H1) ----------

    @Test
    @DisplayName("Mirar un perfil visible anota la visita (H1)")
    void obtenerPerfilYRegistrarVisita_visible_anotaLaVisita() {
        Negocio negocio = negocioAprobado();
        when(repositorio.buscarVisibleEnDirectorio(7L)).thenReturn(Optional.of(negocio));

        service.obtenerPerfilYRegistrarVisita(7L, null, "1.2.3.4|curl");

        verify(visitaService).registrar(negocio, null, "1.2.3.4|curl");
    }

    @Test
    @DisplayName("Un perfil que no se ve no cuenta como visita (H1, B6)")
    void obtenerPerfilYRegistrarVisita_noVisible_noAnotaNada() {
        when(repositorio.buscarVisibleEnDirectorio(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerPerfilYRegistrarVisita(99L, null, "huella"));

        // El orden importa: primero se comprueba la visibilidad, y solo
        // entonces se cuenta. Un 404 no deja rastro en las métricas.
        verify(visitaService, never()).registrar(any(), any(), any());
    }

    @Test
    @DisplayName("El perfil devuelto es el mismo que sin registrar visita")
    void obtenerPerfilYRegistrarVisita_devuelveElPerfilCompleto() {
        when(repositorio.buscarVisibleEnDirectorio(7L))
                .thenReturn(Optional.of(negocioAprobado()));

        PerfilNegocioResponse perfil =
                service.obtenerPerfilYRegistrarVisita(7L, null, "huella");

        assertEquals("Panadería La Tradicional", perfil.nombre());
    }
}
