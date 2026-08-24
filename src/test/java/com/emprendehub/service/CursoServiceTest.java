package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.ActualizarCursoRequest;
import com.emprendehub.dto.CrearCursoRequest;
import com.emprendehub.dto.CursoResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.Curso;
import com.emprendehub.model.EstadoCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.repository.CursoRepository;
import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {

    @Mock
    private CursoRepository repositorio;

    @InjectMocks
    private CursoService service;

    private Curso crearCursoGratuitoDePrueba() {
        Curso curso = new Curso("Marketing Digital Básico", "Fundamentos de redes sociales",
                "4 horas", CategoriaCurso.MARKETING, NivelCurso.BASICO,
                true, null, "https://ejemplo.co/marketing", "📱");
        curso.setId(1L);
        return curso;
    }

    private CrearCursoRequest peticionGratuita() {
        return new CrearCursoRequest("Marketing Digital Básico", "Fundamentos de redes sociales",
                "4 horas", CategoriaCurso.MARKETING, NivelCurso.BASICO,
                true, null, "https://ejemplo.co/marketing", "📱");
    }

    private CrearCursoRequest peticionDePago(BigDecimal precio) {
        return new CrearCursoRequest("Estrategias de Ventas", "Técnicas de cierre",
                "8 horas", CategoriaCurso.VENTAS, NivelCurso.INTERMEDIO,
                false, precio, "https://ejemplo.co/ventas", "🎯");
    }

    // ---------- Regla de gratuidad y precio ----------

    @Test
    @DisplayName("crear: un curso gratuito con precio se rechaza y no se guarda")
    void crear_gratuitoConPrecio_lanzaExcepcion() {
        // Arrange
        CrearCursoRequest peticion = new CrearCursoRequest("Curso", "Descripción", "2 horas",
                CategoriaCurso.FINANZAS, NivelCurso.BASICO,
                true, new BigDecimal("30000"), "https://ejemplo.co", "💰");

        // Act & Assert
        var excepcion = assertThrows(ReglaDeNegocioException.class, () -> service.crear(peticion));

        assertTrue(excepcion.getMessage().contains("gratuito"));
        verify(repositorio, never()).save(any(Curso.class));
    }

    @Test
    @DisplayName("crear: un curso de pago sin precio se rechaza y no se guarda")
    void crear_dePagoSinPrecio_lanzaExcepcion() {
        CrearCursoRequest peticion = peticionDePago(null);

        var excepcion = assertThrows(ReglaDeNegocioException.class, () -> service.crear(peticion));

        assertTrue(excepcion.getMessage().contains("de pago"));
        verify(repositorio, never()).save(any(Curso.class));
    }

    @Test
    @DisplayName("crear: un curso gratuito válido se guarda sin precio")
    void crear_gratuitoValido_seGuardaSinPrecio() {
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        CursoResponse resultado = service.crear(peticionGratuita());

        assertTrue(resultado.gratuito());
        assertNull(resultado.precio());
    }

    @Test
    @DisplayName("crear: un curso de pago conserva su precio")
    void crear_dePagoValido_conservaElPrecio() {
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        CursoResponse resultado = service.crear(peticionDePago(new BigDecimal("49000")));

        assertEquals(0, new BigDecimal("49000").compareTo(resultado.precio()));
    }

    @Test
    @DisplayName("crear: el curso nace siempre en borrador, nunca publicado")
    void crear_naceEnBorrador() {
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        CursoResponse resultado = service.crear(peticionGratuita());

        assertEquals("BORRADOR", resultado.estado());
    }

    // ---------- Catálogo público ----------

    @Test
    @DisplayName("buscarPublicados: consulta solo por el estado PUBLICADO")
    void buscarPublicados_filtraPorEstadoPublicado() {
        Pageable pagina = PageRequest.of(0, 12);
        when(repositorio.buscar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(crearCursoGratuitoDePrueba())));

        var resultado = service.buscarPublicados(null, null, null, null, pagina);

        assertEquals(1, resultado.getTotalElements());
        ArgumentCaptor<EstadoCurso> estado = ArgumentCaptor.forClass(EstadoCurso.class);
        verify(repositorio).buscar(estado.capture(), any(), any(), any(), any(), any());
        assertEquals(EstadoCurso.PUBLICADO, estado.getValue());
    }

    @Test
    @DisplayName("buscarPublicados: un texto en blanco se trata como si no hubiera filtro")
    void buscarPublicados_textoEnBlanco_seNormalizaANulo() {
        when(repositorio.buscar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscarPublicados(null, null, null, "   ", PageRequest.of(0, 12));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(repositorio).buscar(any(), any(), any(), any(), texto.capture(), any());
        assertNull(texto.getValue());
    }

    @Test
    @DisplayName("buscarPublicados: el texto se recorta antes de consultar")
    void buscarPublicados_textoConEspacios_seRecorta() {
        when(repositorio.buscar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscarPublicados(null, null, null, "  marketing  ", PageRequest.of(0, 12));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(repositorio).buscar(any(), any(), any(), any(), texto.capture(), any());
        assertEquals("marketing", texto.getValue());
    }

    @Test
    @DisplayName("buscarTodos: no filtra por estado, así el admin ve los borradores")
    void buscarTodos_noFiltraPorEstado() {
        when(repositorio.buscar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscarTodos(null, null, null, null, PageRequest.of(0, 12));

        ArgumentCaptor<EstadoCurso> estado = ArgumentCaptor.forClass(EstadoCurso.class);
        verify(repositorio).buscar(estado.capture(), any(), any(), any(), any(), any());
        assertNull(estado.getValue());
    }

    @Test
    @DisplayName("obtenerPublicado: devuelve el curso cuando está publicado")
    void obtenerPublicado_existente_devuelveElCurso() {
        when(repositorio.findByIdAndEstado(1L, EstadoCurso.PUBLICADO))
                .thenReturn(Optional.of(crearCursoGratuitoDePrueba()));

        assertEquals("Marketing Digital Básico", service.obtenerPublicado(1L).titulo());
    }

    @Test
    @DisplayName("obtenerPublicado: un borrador se comporta como si no existiera")
    void obtenerPublicado_enBorrador_lanza404() {
        when(repositorio.findByIdAndEstado(9L, EstadoCurso.PUBLICADO)).thenReturn(Optional.empty());

        var excepcion = assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerPublicado(9L));

        assertTrue(excepcion.getMessage().contains("9"));
    }

    // ---------- Gestión ----------

    @Test
    @DisplayName("obtener: lanza excepción cuando el curso no existe")
    void obtener_inexistente_lanzaExcepcion() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(99L));
    }

    @Test
    @DisplayName("actualizar: cambia los campos y respeta la regla del precio")
    void actualizar_existente_modificaLosCampos() {
        Curso curso = crearCursoGratuitoDePrueba();
        when(repositorio.findById(1L)).thenReturn(Optional.of(curso));
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        var peticion = new ActualizarCursoRequest("Nuevo título", "Nueva descripción", "6 horas",
                CategoriaCurso.DIGITAL, NivelCurso.AVANZADO,
                false, new BigDecimal("79000"), "https://ejemplo.co/nuevo", "🤖");

        CursoResponse resultado = service.actualizar(1L, peticion);

        assertEquals("Nuevo título", resultado.titulo());
        assertEquals("DIGITAL", resultado.categoria());
        assertEquals(0, new BigDecimal("79000").compareTo(resultado.precio()));
    }

    @Test
    @DisplayName("actualizar: al pasar a gratuito se borra el precio anterior")
    void actualizar_aGratuito_borraElPrecio() {
        Curso curso = crearCursoGratuitoDePrueba();
        curso.setGratuito(false);
        curso.setPrecio(new BigDecimal("49000"));
        when(repositorio.findById(1L)).thenReturn(Optional.of(curso));
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        var peticion = new ActualizarCursoRequest("Título", "Descripción", "4 horas",
                CategoriaCurso.MARKETING, NivelCurso.BASICO,
                true, null, "https://ejemplo.co", "📱");

        assertNull(service.actualizar(1L, peticion).precio());
    }

    @Test
    @DisplayName("actualizar: lanza excepción cuando el curso no existe")
    void actualizar_inexistente_lanzaExcepcion() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());
        var peticion = new ActualizarCursoRequest("Título", "Descripción", "4 horas",
                CategoriaCurso.MARKETING, NivelCurso.BASICO,
                true, null, "https://ejemplo.co", "📱");

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(99L, peticion));
    }

    @Test
    @DisplayName("publicar: un borrador pasa a publicado")
    void publicar_borrador_pasaAPublicado() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(crearCursoGratuitoDePrueba()));
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("PUBLICADO", service.publicar(1L).estado());
    }

    @Test
    @DisplayName("publicar: publicar dos veces se rechaza")
    void publicar_yaPublicado_lanzaExcepcion() {
        Curso curso = crearCursoGratuitoDePrueba();
        curso.setEstado(EstadoCurso.PUBLICADO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(curso));

        assertThrows(ReglaDeNegocioException.class, () -> service.publicar(1L));
        verify(repositorio, never()).save(any(Curso.class));
    }

    @Test
    @DisplayName("pasarABorrador: un publicado se retira del catálogo")
    void pasarABorrador_publicado_vuelveABorrador() {
        Curso curso = crearCursoGratuitoDePrueba();
        curso.setEstado(EstadoCurso.PUBLICADO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(curso));
        when(repositorio.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("BORRADOR", service.pasarABorrador(1L).estado());
    }

    @Test
    @DisplayName("pasarABorrador: si ya estaba en borrador se rechaza")
    void pasarABorrador_yaEnBorrador_lanzaExcepcion() {
        when(repositorio.findById(1L)).thenReturn(Optional.of(crearCursoGratuitoDePrueba()));

        assertThrows(ReglaDeNegocioException.class, () -> service.pasarABorrador(1L));
        verify(repositorio, never()).save(any(Curso.class));
    }

    @Test
    @DisplayName("eliminar: borra el curso cuando existe")
    void eliminar_existente_borraElCurso() {
        when(repositorio.existsById(1L)).thenReturn(true);

        service.eliminar(1L);

        verify(repositorio).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar: lanza excepción y no borra nada cuando no existe")
    void eliminar_inexistente_lanzaExcepcionYNoBorra() {
        when(repositorio.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(99L));
        verify(repositorio, never()).deleteById(99L);
    }
}
