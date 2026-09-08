package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.config.AlmacenamientoFotos;
import com.emprendehub.dto.FotoResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Barrio;
import com.emprendehub.model.CambioPendiente;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.EstadoFoto;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.Foto;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.CambioPendienteRepository;
import com.emprendehub.repository.FotoRepository;
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
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FotoServiceTest {

    @Mock
    private FotoRepository fotoRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private CambioPendienteRepository cambioRepository;

    @Mock
    private AlmacenamientoFotos almacenamiento;

    @InjectMocks
    private FotoService service;

    private final Usuario duena = duena();

    private Usuario duena() {
        Usuario usuario = new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR);
        usuario.setId(4L);
        return usuario;
    }

    private Negocio negocio(EstadoNegocio estado) {
        Ciudad medellin = new Ciudad("Medellín");
        Barrio poblado = new Barrio("El Poblado");
        poblado.setCiudad(medellin);

        Negocio negocio = new Negocio(duena, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                new CategoriaNegocio("Gastronomía", "🍴"), medellin, poblado, NivelPrecio.MEDIO);
        negocio.setId(7L);
        negocio.setEstado(estado);
        return negocio;
    }

    /** Deja preparado el negocio de la dueña, que casi todos los casos necesitan. */
    private Negocio conNegocio(EstadoNegocio estado) {
        Negocio negocio = negocio(estado);
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.of(negocio));
        return negocio;
    }

    /** Un archivo que pasa las tres validaciones de B9. */
    private MultipartFile archivo(String tipo) {
        MultipartFile archivo = mock(MultipartFile.class);
        when(archivo.isEmpty()).thenReturn(false);
        when(archivo.getSize()).thenReturn(120_000L);
        when(archivo.getContentType()).thenReturn(tipo);
        return archivo;
    }

    private Foto foto(Long id, int orden, EstadoFoto estado) {
        Foto foto = new Foto(negocio(EstadoNegocio.APROBADO), "imagen" + id + ".jpg", orden);
        foto.setId(id);
        foto.setEstado(estado);
        return foto;
    }

    // ---------- Subida: las tres reglas de B9 ----------

    @Test
    @DisplayName("La primera foto entra en el orden 0 y es la principal (B9)")
    void subir_primeraFoto_esLaPrincipal() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(0L);
        when(almacenamiento.guardar(any(), eq("jpg"))).thenReturn("nueva.jpg");

        FotoResponse respuesta = service.subir(duena, archivo("image/jpeg"));

        assertEquals(0, respuesta.orden());
        assertTrue(respuesta.principal());
        assertEquals("/fotos/nueva.jpg", respuesta.url());
    }

    @Test
    @DisplayName("Una foto nueva nace pendiente de revisión (B2)")
    void subir_naceePendiente() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(0L);
        when(almacenamiento.guardar(any(), any())).thenReturn("nueva.jpg");

        assertEquals("PENDIENTE", service.subir(duena, archivo("image/png")).estado());
    }

    @Test
    @DisplayName("La segunda foto va detrás y ya no es la principal")
    void subir_segundaFoto_noEsPrincipal() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(1L);
        when(almacenamiento.guardar(any(), any())).thenReturn("otra.jpg");

        FotoResponse respuesta = service.subir(duena, archivo("image/jpeg"));

        assertEquals(1, respuesta.orden());
        assertFalse(respuesta.principal());
    }

    @Test
    @DisplayName("La séptima foto se rechaza: la galería admite seis (B9)")
    void subir_pasandoDelMaximo_lanza() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(6L);

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.subir(duena, archivo("image/jpeg")));

        assertTrue(error.getMessage().contains("6"));
        verify(almacenamiento, never()).guardar(any(), any());
    }

    @Test
    @DisplayName("El máximo cuenta también las que esperan revisión")
    void subir_conPendientesQueLlenanLaGaleria_lanza() {
        // Si contara solo las aprobadas, se podrían tener seis publicadas y seis
        // más esperando, y la galería pasaría de seis en cuanto se aprobaran.
        conNegocio(EstadoNegocio.APROBADO);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(6L);

        assertThrows(ReglaDeNegocioException.class,
                () -> service.subir(duena, archivo("image/jpeg")));
    }

    @Test
    @DisplayName("Un GIF no se admite: solo JPG y PNG (B9)")
    void subir_tipoNoAdmitido_lanza() {
        conNegocio(EstadoNegocio.PENDIENTE);

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.subir(duena, archivo("image/gif")));

        assertTrue(error.getMessage().contains("JPG o PNG"));
    }

    @Test
    @DisplayName("Sin tipo declarado tampoco se admite")
    void subir_sinTipo_lanza() {
        conNegocio(EstadoNegocio.PENDIENTE);

        assertThrows(ReglaDeNegocioException.class,
                () -> service.subir(duena, archivo(null)));
    }

    @Test
    @DisplayName("Una imagen de más de 5 MB se rechaza (B9)")
    void subir_demasiadoGrande_lanza() {
        conNegocio(EstadoNegocio.PENDIENTE);
        MultipartFile grande = mock(MultipartFile.class);
        when(grande.isEmpty()).thenReturn(false);
        when(grande.getSize()).thenReturn(FotoService.TAMANO_MAXIMO_BYTES + 1);

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.subir(duena, grande));

        assertTrue(error.getMessage().contains("5 MB"));
    }

    @Test
    @DisplayName("Justo 5 MB sí entra: el límite es inclusivo")
    void subir_enElLimite_seAcepta() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(0L);
        when(almacenamiento.guardar(any(), any())).thenReturn("justa.jpg");
        MultipartFile justa = mock(MultipartFile.class);
        when(justa.isEmpty()).thenReturn(false);
        when(justa.getSize()).thenReturn(FotoService.TAMANO_MAXIMO_BYTES);
        when(justa.getContentType()).thenReturn("image/jpeg");

        assertEquals("/fotos/justa.jpg", service.subir(duena, justa).url());
    }

    @Test
    @DisplayName("Una petición sin archivo se rechaza antes de tocar nada")
    void subir_sinArchivo_lanza() {
        conNegocio(EstadoNegocio.PENDIENTE);
        MultipartFile vacio = mock(MultipartFile.class);
        when(vacio.isEmpty()).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class, () -> service.subir(duena, vacio));
    }

    @Test
    @DisplayName("El PNG se guarda con extensión png, no con la del nombre original")
    void subir_png_usaSuPropiaExtension() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(0L);
        when(almacenamiento.guardar(any(), eq("png"))).thenReturn("nueva.png");

        service.subir(duena, archivo("image/png"));

        verify(almacenamiento).guardar(any(), eq("png"));
    }

    // ---------- La subida y la revisión (B2, B2-bis) ----------

    @Test
    @DisplayName("Subir a un negocio publicado abre una propuesta de cambio (B2)")
    void subir_negocioAprobado_abreLaPropuesta() {
        Negocio negocio = conNegocio(EstadoNegocio.APROBADO);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(1L);
        when(almacenamiento.guardar(any(), any())).thenReturn("nueva.jpg");
        when(cambioRepository.findByNegocioId(7L)).thenReturn(Optional.empty());

        service.subir(duena, archivo("image/jpeg"));

        ArgumentCaptor<CambioPendiente> captor = ArgumentCaptor.forClass(CambioPendiente.class);
        verify(cambioRepository).save(captor.capture());
        // Una propuesta solo de fotos guarda los textos actuales: al aprobarla
        // se copian sobre sí mismos y lo que se publica son las imágenes.
        assertEquals(negocio.getNombre(), captor.getValue().getNombrePropuesto());
    }

    @Test
    @DisplayName("Un negocio sin aprobar no abre propuesta: sus fotos van con él")
    void subir_negocioPendiente_noAbrePropuesta() {
        conNegocio(EstadoNegocio.PENDIENTE);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(0L);
        when(almacenamiento.guardar(any(), any())).thenReturn("nueva.jpg");

        service.subir(duena, archivo("image/jpeg"));

        verify(cambioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Con una propuesta ya abierta no se crea otra")
    void subir_conPropuestaAbierta_noLaDuplica() {
        Negocio negocio = conNegocio(EstadoNegocio.APROBADO);
        when(fotoRepository.countByNegocioId(7L)).thenReturn(0L);
        when(almacenamiento.guardar(any(), any())).thenReturn("nueva.jpg");
        when(cambioRepository.findByNegocioId(7L)).thenReturn(
                Optional.of(new CambioPendiente(negocio, "Otro nombre", "Otra descripción")));

        service.subir(duena, archivo("image/jpeg"));

        verify(cambioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aprobar las pendientes se delega en la consulta que las publica")
    void aprobarPendientes_delegaEnElRepositorio() {
        when(fotoRepository.aprobarPendientes(7L)).thenReturn(3);

        assertEquals(3, service.aprobarPendientes(7L));
    }

    @Test
    @DisplayName("Descartar borra las filas, los ficheros y recoloca lo que queda")
    void descartarPendientes_borraFilasYFicheros() {
        List<Foto> pendientes = List.of(foto(2L, 1, EstadoFoto.PENDIENTE));
        when(fotoRepository.findByNegocioIdAndEstado(7L, EstadoFoto.PENDIENTE))
                .thenReturn(pendientes);
        when(fotoRepository.findByNegocioIdOrderByOrdenAsc(7L))
                .thenReturn(List.of(foto(1L, 0, EstadoFoto.APROBADA)));

        assertEquals(1, service.descartarPendientes(7L));

        verify(fotoRepository).deleteAll(pendientes);
        verify(almacenamiento).borrar("imagen2.jpg");
    }

    @Test
    @DisplayName("Descartar sin nada pendiente no toca el disco")
    void descartarPendientes_sinPendientes_noHaceNada() {
        when(fotoRepository.findByNegocioIdAndEstado(7L, EstadoFoto.PENDIENTE))
                .thenReturn(List.of());

        assertEquals(0, service.descartarPendientes(7L));

        verify(almacenamiento, never()).borrar(any());
        verify(fotoRepository, never()).deleteAll(any());
    }

    // ---------- Galería y borrado ----------

    @Test
    @DisplayName("La galería del dueño marca principal solo a la primera")
    void listarMias_marcaLaPrimera() {
        conNegocio(EstadoNegocio.APROBADO);
        when(fotoRepository.findByNegocioIdOrderByOrdenAsc(7L)).thenReturn(
                List.of(foto(1L, 0, EstadoFoto.APROBADA), foto(2L, 1, EstadoFoto.PENDIENTE)));

        List<FotoResponse> galeria = service.listarMias(duena);

        assertTrue(galeria.get(0).principal());
        assertFalse(galeria.get(1).principal());
        // El dueño sí ve las suyas sin revisar; el público, no.
        assertEquals("PENDIENTE", galeria.get(1).estado());
    }

    @Test
    @DisplayName("Al borrar una foto se recolocan las demás sin dejar huecos")
    void eliminar_recolocaLosOrdenes() {
        conNegocio(EstadoNegocio.APROBADO);
        Foto primera = foto(1L, 0, EstadoFoto.APROBADA);
        Foto tercera = foto(3L, 2, EstadoFoto.APROBADA);
        when(fotoRepository.findByIdAndNegocioId(1L, 7L)).thenReturn(Optional.of(primera));
        when(fotoRepository.findByNegocioIdOrderByOrdenAsc(7L)).thenReturn(List.of(tercera));

        service.eliminar(duena, 1L);

        verify(fotoRepository).delete(primera);
        verify(almacenamiento).borrar("imagen1.jpg");
        // La que estaba en el orden 2 pasa al 0 y se convierte en la principal.
        assertEquals(0, tercera.getOrden());
    }

    @Test
    @DisplayName("No se puede borrar una foto de otro negocio")
    void eliminar_fotoAjena_lanzaNoEncontrado() {
        conNegocio(EstadoNegocio.APROBADO);
        when(fotoRepository.findByIdAndNegocioId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(duena, 99L));
        verify(almacenamiento, never()).borrar(any());
    }

    @Test
    @DisplayName("Quien no tiene negocio no tiene galería")
    void listarMias_sinNegocio_lanzaNoEncontrado() {
        when(negocioRepository.findByUsuarioId(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listarMias(duena));
    }
}
