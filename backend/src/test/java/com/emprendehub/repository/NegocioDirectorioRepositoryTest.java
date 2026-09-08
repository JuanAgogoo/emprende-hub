package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Barrio;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Las consultas del directorio contra PostgreSQL de verdad.
 *
 * <p>Existe porque las pruebas de servicio, con el repositorio simulado, no
 * pueden ver un error de SQL. Y aquí hay tres que solo aparecen así: el
 * {@code CAST} del texto opcional, que el filtro por ciudad no se coma a los
 * negocios sin barrio, y que los nulos de la calificación acaben al final y no
 * al principio.
 */
class NegocioDirectorioRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NegocioRepository repository;

    private Long idGastronomia;
    private Long idModa;
    private Long idMedellin;
    private Long idEnvigado;
    private Long idPoblado;
    private Long idPanaderia;
    private Long idPendiente;
    private Long idSuspendido;

    /** Sin ningún filtro, el directorio solo enseña estos cuatro. */
    private static final int VISIBLES = 4;

    @BeforeEach
    void prepararDirectorio() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        Ciudad envigado = entityManager.persistAndFlush(new Ciudad("Envigado"));
        Barrio poblado = new Barrio("El Poblado");
        poblado.setCiudad(medellin);
        entityManager.persistAndFlush(poblado);

        CategoriaNegocio gastronomia =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));
        CategoriaNegocio moda =
                entityManager.persistAndFlush(new CategoriaNegocio("Moda", "👗"));

        Instant ahora = Instant.now();

        Negocio panaderia = aprobado(
                "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.",
                gastronomia, medellin, poblado, NivelPrecio.MEDIO,
                new BigDecimal("4.80"), 12, ahora.minus(3, ChronoUnit.DAYS));

        // Sin barrio a propósito: tiene que seguir saliendo al filtrar por Medellín.
        aprobado("Moda Andina", "Ropa hecha con tejidos artesanales de la región.",
                moda, medellin, null, NivelPrecio.ALTO,
                new BigDecimal("4.20"), 6, ahora.minus(1, ChronoUnit.DAYS));

        // Recién aprobado y sin ninguna opinión: no tiene calificación (C5).
        aprobado("Café Nuevo", "Cafetería de especialidad recién abierta.",
                gastronomia, envigado, null, NivelPrecio.BAJO,
                null, 0, ahora.minus(2, ChronoUnit.HOURS));

        // Cinco estrellas pero solo tres opiniones: fuera de Destacados (C7).
        aprobado("Joyería Chispa", "Joyas de plata trabajadas a mano.",
                moda, medellin, poblado, NivelPrecio.MEDIO,
                new BigDecimal("5.00"), 3, ahora.minus(5, ChronoUnit.DAYS));

        Negocio pendiente = new Negocio(nuevaCuenta("pendiente"), "Taller Pendiente",
                "Taller de bicicletas que todavía espera revisión.",
                "3001112233", gastronomia, medellin, null, NivelPrecio.BAJO);
        entityManager.persistAndFlush(pendiente);

        Usuario suspendida = nuevaCuenta("suspendida");
        suspendida.setActivo(false);
        entityManager.persistAndFlush(suspendida);
        Negocio suspendido = new Negocio(suspendida, "Bar Suspendido",
                "Bar cuyo dueño fue suspendido por el administrador.",
                "3004445566", gastronomia, medellin, null, NivelPrecio.BAJO);
        suspendido.setEstado(EstadoNegocio.APROBADO);
        suspendido.setFechaAprobacion(ahora.minus(10, ChronoUnit.DAYS));
        entityManager.persistAndFlush(suspendido);

        idGastronomia = gastronomia.getId();
        idModa = moda.getId();
        idMedellin = medellin.getId();
        idEnvigado = envigado.getId();
        idPoblado = poblado.getId();
        idPanaderia = panaderia.getId();
        idPendiente = pendiente.getId();
        idSuspendido = suspendido.getId();
        entityManager.clear();
    }

    private Negocio aprobado(String nombre, String descripcion, CategoriaNegocio categoria,
                             Ciudad ciudad, Barrio barrio, NivelPrecio precio,
                             BigDecimal calificacion, int opiniones, Instant fechaAprobacion) {
        Negocio negocio = new Negocio(nuevaCuenta(nombre), nombre, descripcion,
                "3001234567", categoria, ciudad, barrio, precio);
        negocio.setEstado(EstadoNegocio.APROBADO);
        negocio.setFechaAprobacion(fechaAprobacion);
        negocio.setCalificacionPromedio(calificacion);
        negocio.setNumeroOpiniones(opiniones);
        return entityManager.persistAndFlush(negocio);
    }

    private Usuario nuevaCuenta(String semilla) {
        String correo = semilla.toLowerCase().replaceAll("[^a-z]", "") + "@test.co";
        return entityManager.persistAndFlush(
                new Usuario("Dueño de " + semilla, correo, "hash", Rol.EMPRENDEDOR));
    }

    /** Sin filtros ni criterio, como llega la primera visita al directorio. */
    private List<String> buscarTodo() {
        return nombresDe(sinFiltros(Pageable.ofSize(20)));
    }

    private List<String> nombresDe(List<Negocio> negocios) {
        return negocios.stream().map(Negocio::getNombre).toList();
    }

    private List<Negocio> sinFiltros(Pageable pagina) {
        return repository.buscarEnDirectorio(null, null, null, null, null, null, pagina)
                .getContent();
    }

    // ---------- Visibilidad ----------

    @Test
    @DisplayName("Con todos los filtros nulos la consulta funciona y no revienta")
    void buscar_sinNingunFiltro_devuelveLosVisibles() {
        // Es la prueba del CAST: con :texto a null, PostgreSQL lo infiere como
        // bytea y falla con «function lower(bytea) does not exist».
        assertEquals(VISIBLES, buscarTodo().size());
    }

    @Test
    @DisplayName("El directorio no enseña ni pendientes ni negocios de cuentas suspendidas")
    void buscar_excluyeLoQueNoEsVisible() {
        List<String> nombres = buscarTodo();

        assertTrue(nombres.stream().noneMatch(n -> n.equals("Taller Pendiente")),
                "Un negocio pendiente nunca sale en el directorio (B6): " + nombres);
        assertTrue(nombres.stream().noneMatch(n -> n.equals("Bar Suspendido")),
                "Un negocio de cuenta suspendida se oculta (B4): " + nombres);
    }

    @Test
    @DisplayName("La búsqueda trae resueltas categoría, ciudad y barrio")
    void buscar_traeLasRelacionesResueltas() {
        // Sin el @EntityGraph esto fallaría al mapear, porque open-in-view
        // está desactivado.
        Negocio negocio = sinFiltros(PageRequest.of(0, 20, Sort.by("nombre"))).getFirst();

        assertNotNull(negocio.getCategoria().getNombre());
        assertNotNull(negocio.getCiudad().getNombre());
    }

    // ---------- Filtros ----------

    @Test
    @DisplayName("El texto busca en el nombre, sin distinguir mayúsculas (G6)")
    void buscar_porNombre_ignoraMayusculas() {
        var pagina = repository.buscarEnDirectorio("PANADERÍA", null, null, null, null, null,
                Pageable.ofSize(20));

        assertEquals(List.of("Panadería La Tradicional"), nombresDe(pagina.getContent()));
    }

    @Test
    @DisplayName("El texto también busca en la descripción, no solo en el nombre (G6)")
    void buscar_porDescripcion_encuentraElNegocio() {
        var pagina = repository.buscarEnDirectorio("tejidos", null, null, null, null, null,
                Pageable.ofSize(20));

        assertEquals(List.of("Moda Andina"), nombresDe(pagina.getContent()));
    }

    @Test
    @DisplayName("Filtrar por ciudad incluye a los negocios que no tienen barrio")
    void buscar_porCiudad_incluyeLosQueNoTienenBarrio() {
        // Si n.barrio.id generase un JOIN, «Moda Andina» se quedaría fuera por
        // no tener barrio, y el filtro de ciudad de G3 estaría roto.
        var pagina = repository.buscarEnDirectorio(null, null, idMedellin, null, null, null,
                PageRequest.of(0, 20, Sort.by("nombre")));

        assertEquals(List.of("Joyería Chispa", "Moda Andina", "Panadería La Tradicional"),
                nombresDe(pagina.getContent()));
    }

    @Test
    @DisplayName("Filtrar por barrio afina dentro de la ciudad (G3)")
    void buscar_porBarrio_soloLosDeEseBarrio() {
        var pagina = repository.buscarEnDirectorio(null, null, idMedellin, idPoblado, null, null,
                PageRequest.of(0, 20, Sort.by("nombre")));

        assertEquals(List.of("Joyería Chispa", "Panadería La Tradicional"),
                nombresDe(pagina.getContent()));
    }

    @Test
    @DisplayName("Los filtros de categoría, precio y ciudad se combinan entre sí")
    void buscar_filtrosCombinados_seAcumulan() {
        var pagina = repository.buscarEnDirectorio(null, idGastronomia, idEnvigado, null, null,
                NivelPrecio.BAJO, Pageable.ofSize(20));

        assertEquals(List.of("Café Nuevo"), nombresDe(pagina.getContent()));
    }

    @Test
    @DisplayName("Filtrar por categoría deja fuera al resto")
    void buscar_porCategoria_soloEsaCategoria() {
        var pagina = repository.buscarEnDirectorio(null, idModa, null, null, null, null,
                PageRequest.of(0, 20, Sort.by("nombre")));

        assertEquals(List.of("Joyería Chispa", "Moda Andina"), nombresDe(pagina.getContent()));
    }

    @Test
    @DisplayName("La calificación mínima deja fuera a quien todavía no tiene ninguna (C5)")
    void buscar_porCalificacionMinima_excluyeAlQueNoTieneCalificacion() {
        var pagina = repository.buscarEnDirectorio(null, null, null, null,
                new BigDecimal("4.00"), null, PageRequest.of(0, 20, Sort.by("nombre")));

        // «Café Nuevo» tiene la calificación a nulo: no es un cero, es que no
        // hay ninguna, y ninguna comparación con nulo es cierta.
        assertEquals(List.of("Joyería Chispa", "Moda Andina", "Panadería La Tradicional"),
                nombresDe(pagina.getContent()));
    }

    // ---------- Ordenación ----------

    @Test
    @DisplayName("Al ordenar por calificación, el que no tiene ninguna va al final")
    void buscar_ordenadoPorCalificacion_poneLosNulosAlFinal() {
        // Sin nullsLast, PostgreSQL coloca los nulos primero al ordenar de mayor
        // a menor: el negocio sin ninguna opinión encabezaría «Mejor calificados».
        Sort orden = Sort.by(Sort.Order.desc("calificacionPromedio").nullsLast(),
                Sort.Order.desc("numeroOpiniones"));

        assertEquals(
                List.of("Joyería Chispa", "Panadería La Tradicional", "Moda Andina", "Café Nuevo"),
                nombresDe(sinFiltros(PageRequest.of(0, 20, orden))));
    }

    @Test
    @DisplayName("«Más recientes» ordena por fecha de aprobación, no de creación (G7)")
    void buscar_ordenadoPorRecientes_usaLaFechaDeAprobacion() {
        // Todos se crearon en el mismo instante en este método: si el orden
        // saliera de la fecha de creación, esta lista sería otra.
        Sort orden = Sort.by(Sort.Order.desc("fechaAprobacion").nullsLast());

        assertEquals(
                List.of("Café Nuevo", "Moda Andina", "Panadería La Tradicional", "Joyería Chispa"),
                nombresDe(sinFiltros(PageRequest.of(0, 20, orden))));
    }

    @Test
    @DisplayName("La paginación reparte los resultados y cuenta el total")
    void buscar_paginado_devuelveElTotalCompleto() {
        var pagina = repository.buscarEnDirectorio(null, null, null, null, null, null,
                PageRequest.of(0, 2, Sort.by("nombre")));

        assertEquals(2, pagina.getContent().size());
        assertEquals(VISIBLES, pagina.getTotalElements());
        assertEquals(2, pagina.getTotalPages());
    }

    // ---------- Perfil público ----------

    @Test
    @DisplayName("Un negocio aprobado y activo se ve por su identificador público")
    void buscarVisible_aprobado_loDevuelve() {
        var negocio = repository.buscarVisibleEnDirectorio(idPanaderia);

        assertTrue(negocio.isPresent());
        assertEquals("Panadería La Tradicional", negocio.get().getNombre());
    }

    @Test
    @DisplayName("Un negocio pendiente no es accesible por su identificador (B6)")
    void buscarVisible_pendiente_devuelveVacio() {
        assertTrue(repository.buscarVisibleEnDirectorio(idPendiente).isEmpty());
    }

    @Test
    @DisplayName("El negocio de una cuenta suspendida tampoco es accesible (B4)")
    void buscarVisible_deCuentaSuspendida_devuelveVacio() {
        assertTrue(repository.buscarVisibleEnDirectorio(idSuspendido).isEmpty());
    }

    // ---------- Destacados y cifras ----------

    @Test
    @DisplayName("Destacados exige cinco opiniones y ordena por calificación (C7)")
    void destacados_exigeElMinimoDeOpiniones() {
        List<Negocio> destacados = repository.buscarDestacados(5, Pageable.ofSize(10));

        // «Joyería Chispa» tiene 5,00 con solo tres opiniones: se queda fuera,
        // que es exactamente lo que C7 quiere evitar en la portada.
        assertEquals(List.of("Panadería La Tradicional", "Moda Andina"), nombresDe(destacados));
    }

    @Test
    @DisplayName("Destacados respeta el límite que se le pide")
    void destacados_respetaElLimite() {
        assertEquals(1, repository.buscarDestacados(5, Pageable.ofSize(1)).size());
    }

    @Test
    @DisplayName("Las cifras de la portada cuentan solo lo que se ve (H4)")
    void contarVisibles_cuentaLoMismoQueEnseñaElDirectorio() {
        assertEquals(VISIBLES, repository.contarVisiblesEnDirectorio());
    }

    @Test
    @DisplayName("La media de la portada ignora a los negocios sin calificación (C5)")
    void promedio_ignoraLosQueNoTienenCalificacion() {
        Double promedio = repository.promedioDeCalificaciones();

        // (4,80 + 4,20 + 5,00) / 3, sin contar a «Café Nuevo».
        assertNotNull(promedio);
        assertEquals(4.666, promedio, 0.001);
    }
}
