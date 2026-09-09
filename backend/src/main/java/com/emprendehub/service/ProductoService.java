package com.emprendehub.service;

import com.emprendehub.config.AlmacenamientoFotos;
import com.emprendehub.dto.ActualizarProductoRequest;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.exception.ResourceNotFoundException;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Producto;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.ProductoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * El escaparate de un negocio (F1, F2, F3).
 *
 * <p>Es deliberadamente pequeño. No hay reglas que inventar: los productos no se
 * venden, no tienen inventario y no pasan por revisión —cambiar el precio de una
 * pizza no es la clase de cambio que B2 quiere controlar—.
 *
 * <p>La única regla real es de propiedad: solo se toca el escaparate propio, y
 * eso lo garantiza la consulta, que busca por producto <em>y</em> negocio a la
 * vez en lugar de comprobar el dueño después.
 */
@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final NegocioRepository negocioRepository;
    private final AlmacenamientoFotos almacenamiento;

    public ProductoService(ProductoRepository productoRepository,
                           NegocioRepository negocioRepository,
                           AlmacenamientoFotos almacenamiento) {
        this.productoRepository = productoRepository;
        this.negocioRepository = negocioRepository;
        this.almacenamiento = almacenamiento;
    }

    public List<ProductoResponse> listarMios(Usuario solicitante) {
        Negocio negocio = buscarElMio(solicitante);
        return NegocioMapper.aRespuestasDeProducto(
                productoRepository.findByNegocioIdOrderByNombreAsc(negocio.getId()));
    }

    /**
     * Alta de un artículo del escaparate, con su imagen.
     *
     * <p>La foto es obligatoria y llega en la misma petición: crear el producto
     * primero y completarlo después es justo lo que permitiría que el hueco
     * exista, y un escaparate con huecos no es un escaparate.
     *
     * <p>Las reglas de la imagen —JPG o PNG, cinco megas— son las mismas de la
     * galería (B9) y se comprueban en el mismo sitio, sin reescribirlas.
     *
     * <p>El fichero se escribe antes de guardar la fila, igual que en
     * {@link FotoService}. Si la transacción fallara después quedaría un fichero
     * suelto que nadie referencia: no afecta a lo que ve el usuario.
     */
    @Transactional
    public ProductoResponse crear(Usuario solicitante, CrearProductoRequest peticion,
                                  MultipartFile foto) {
        Negocio negocio = buscarElMio(solicitante);
        String extension = AlmacenamientoFotos.extensionDe(foto);

        Producto producto = new Producto(negocio, peticion.nombre().trim(), peticion.precio(),
                normalizar(peticion.descripcion()), peticion.disponible(),
                almacenamiento.guardar(foto, extension));

        return NegocioMapper.aRespuestaDeProducto(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Usuario solicitante, Long productoId,
                                       ActualizarProductoRequest peticion) {
        Producto producto = buscarMio(solicitante, productoId);

        producto.setNombre(peticion.nombre().trim());
        producto.setPrecio(peticion.precio());
        producto.setDescripcion(normalizar(peticion.descripcion()));
        producto.setDisponible(peticion.disponible());

        return NegocioMapper.aRespuestaDeProducto(productoRepository.save(producto));
    }

    /** El interruptor de F3, que es todo el control de existencias que hay. */
    @Transactional
    public ProductoResponse cambiarDisponibilidad(Usuario solicitante, Long productoId,
                                                  boolean disponible) {
        Producto producto = buscarMio(solicitante, productoId);
        producto.setDisponible(disponible);
        return NegocioMapper.aRespuestaDeProducto(productoRepository.save(producto));
    }

    /** Al quitar el producto se va también su imagen: nadie más la referencia. */
    @Transactional
    public void eliminar(Usuario solicitante, Long productoId) {
        Producto producto = buscarMio(solicitante, productoId);
        productoRepository.delete(producto);
        almacenamiento.borrar(producto.getFoto());
    }

    // ---------- Apoyo ----------

    private Producto buscarMio(Usuario solicitante, Long productoId) {
        Negocio negocio = buscarElMio(solicitante);
        return productoRepository.findByIdAndNegocioId(productoId, negocio.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productoId));
    }

    private Negocio buscarElMio(Usuario solicitante) {
        return negocioRepository.findByUsuarioId(solicitante.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Negocio del usuario", solicitante.getId()));
    }

    /** Una descripción en blanco es lo mismo que no ponerla: es opcional (F2). */
    private String normalizar(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
