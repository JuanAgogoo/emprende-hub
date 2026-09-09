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

import com.emprendehub.dto.ActualizarProductoRequest;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Producto;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.config.AlmacenamientoFotos;
import com.emprendehub.repository.ProductoRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private AlmacenamientoFotos almacenamiento;

    @InjectMocks
    private ProductoService service;

    private final Usuario duena = duena();

    private Usuario duena() {
        Usuario usuario = new Usuario("María", "maria@test.co", "hash", Rol.EMPRENDEDOR);
        usuario.setId(4L);
        return usuario;
    }

    private Negocio negocio() {
        Negocio negocio = new Negocio(duena, "Panadería La Tradicional",
                "Pan de masa madre horneado cada mañana en horno de leña.", "3001234567",
                new CategoriaNegocio("Gastronomía", "🍴"), new Ciudad("Medellín"), null,
                NivelPrecio.MEDIO);
        negocio.setId(7L);
        return negocio;
    }

    private Negocio conNegocio() {
        Negocio negocio = negocio();
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.of(negocio));
        return negocio;
    }

    private Producto producto() {
        Producto producto = new Producto(negocio(), "Pan de masa madre",
                new BigDecimal("12000"), "Fermentado 24 horas", true, "pan.jpg");
        producto.setId(3L);
        return producto;
    }

    /** Una imagen que pasa las tres reglas de B9: sin ella no hay alta. */
    private MultipartFile imagenValida() {
        return new MockMultipartFile("foto", "pan.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private CrearProductoRequest peticion(String descripcion, boolean disponible) {
        return new CrearProductoRequest("  Pan de masa madre  ", new BigDecimal("12000"),
                descripcion, disponible);
    }

    // ---------- Alta ----------

    @Test
    @DisplayName("Crear guarda el producto en el negocio de quien lo pide")
    void crear_guardaEnElNegocioPropio() {
        Negocio negocio = conNegocio();
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductoResponse respuesta = service.crear(duena, peticion("Fermentado 24 horas", true), imagenValida());

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertEquals(negocio, captor.getValue().getNegocio());
        assertEquals(new BigDecimal("12000"), respuesta.precio());
        assertTrue(respuesta.disponible());
    }

    @Test
    @DisplayName("El nombre se guarda sin espacios sobrantes")
    void crear_recortaElNombre() {
        conNegocio();
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertEquals("Pan de masa madre",
                service.crear(duena, peticion("Fermentado 24 horas", true), imagenValida()).nombre());
    }

    @Test
    @DisplayName("Una descripción en blanco se guarda como ausente: es opcional (F2)")
    void crear_descripcionEnBlanco_quedaNula() {
        conNegocio();
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertNull(service.crear(duena, peticion("   ", true), imagenValida()).descripcion());
    }

    @Test
    @DisplayName("Un producto puede nacer no disponible (F3)")
    void crear_noDisponible_seRespeta() {
        conNegocio();
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.crear(duena, peticion(null, false), imagenValida()).disponible());
    }

    @Test
    @DisplayName("Sin imagen no hay producto: la foto es obligatoria")
    void crear_sinFoto_lanza() {
        // El servicio busca el negocio antes de mirar el fichero, igual que
        // FotoService: sin este montaje fallaría por el motivo equivocado.
        conNegocio();
        MultipartFile vacia = new MockMultipartFile("foto", new byte[0]);

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.crear(duena, peticion(null, true), vacia));

        assertTrue(error.getMessage().contains("imagen"));
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una imagen que no es JPG ni PNG se rechaza (B9)")
    void crear_tipoNoAdmitido_lanza() {
        conNegocio();
        MultipartFile pdf = new MockMultipartFile("foto", "hoja.pdf", "application/pdf",
                new byte[] {1, 2, 3});

        var error = assertThrows(ReglaDeNegocioException.class,
                () -> service.crear(duena, peticion(null, true), pdf));

        assertTrue(error.getMessage().contains("JPG o PNG"));
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Al borrar un producto se borra también su imagen")
    void eliminar_borraLaImagen() {
        Producto producto = producto();
        conNegocio();
        when(productoRepository.findByIdAndNegocioId(3L, 7L)).thenReturn(Optional.of(producto));

        service.eliminar(duena, 3L);

        verify(productoRepository).delete(producto);
        verify(almacenamiento).borrar("pan.jpg");
    }

    @Test
    @DisplayName("Quien no tiene negocio no puede crear productos")
    void crear_sinNegocio_lanzaNoEncontrado() {
        when(negocioRepository.findByUsuarioId(4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.crear(duena, peticion(null, true), imagenValida()));
        verify(productoRepository, never()).save(any());
    }

    // ---------- Edición ----------

    @Test
    @DisplayName("Actualizar cambia los cuatro campos del producto")
    void actualizar_cambiaLosCampos() {
        conNegocio();
        Producto producto = producto();
        when(productoRepository.findByIdAndNegocioId(3L, 7L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductoResponse respuesta = service.actualizar(duena, 3L,
                new ActualizarProductoRequest("Croissant", new BigDecimal("5500"),
                        "De mantequilla", false));

        assertEquals("Croissant", respuesta.nombre());
        assertEquals(new BigDecimal("5500"), respuesta.precio());
        assertEquals("De mantequilla", respuesta.descripcion());
        assertFalse(respuesta.disponible());
    }

    @Test
    @DisplayName("El producto de otro negocio se comporta como si no existiera")
    void actualizar_productoAjeno_lanzaNoEncontrado() {
        // La consulta busca por producto Y negocio: el ajeno no aparece, así que
        // no hay ninguna comprobación de propiedad que se pueda olvidar.
        conNegocio();
        when(productoRepository.findByIdAndNegocioId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(duena, 99L,
                new ActualizarProductoRequest("Croissant", new BigDecimal("5500"), null, true)));
    }

    @Test
    @DisplayName("El interruptor de disponibilidad no toca nada más (F3)")
    void cambiarDisponibilidad_soloCambiaEseCampo() {
        conNegocio();
        Producto producto = producto();
        when(productoRepository.findByIdAndNegocioId(3L, 7L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProductoResponse respuesta = service.cambiarDisponibilidad(duena, 3L, false);

        assertFalse(respuesta.disponible());
        assertEquals("Pan de masa madre", respuesta.nombre());
        assertEquals(new BigDecimal("12000"), respuesta.precio());
    }

    // ---------- Consulta y borrado ----------

    @Test
    @DisplayName("El escaparate propio llega ordenado por nombre")
    void listarMios_devuelveElEscaparate() {
        conNegocio();
        when(productoRepository.findByNegocioIdOrderByNombreAsc(7L))
                .thenReturn(List.of(producto()));

        List<ProductoResponse> escaparate = service.listarMios(duena);

        assertEquals(1, escaparate.size());
        assertEquals("Pan de masa madre", escaparate.getFirst().nombre());
    }

    @Test
    @DisplayName("Eliminar borra el producto propio")
    void eliminar_borraElProducto() {
        conNegocio();
        Producto producto = producto();
        when(productoRepository.findByIdAndNegocioId(3L, 7L)).thenReturn(Optional.of(producto));

        service.eliminar(duena, 3L);

        verify(productoRepository).delete(producto);
    }

    @Test
    @DisplayName("Eliminar un producto ajeno devuelve no encontrado")
    void eliminar_productoAjeno_lanzaNoEncontrado() {
        conNegocio();
        when(productoRepository.findByIdAndNegocioId(99L, 7L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(duena, 99L));
        verify(productoRepository, never()).delete(any());
    }
}
