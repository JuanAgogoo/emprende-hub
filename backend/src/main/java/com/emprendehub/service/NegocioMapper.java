package com.emprendehub.service;

import com.emprendehub.dto.FotoResponse;
import com.emprendehub.dto.NegocioPublicoResponse;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.PerfilNegocioResponse;
import com.emprendehub.dto.ProductoResponse;
import com.emprendehub.model.Foto;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.Producto;
import java.util.List;

/**
 * Traducción de {@link Negocio} y de lo que cuelga de él.
 *
 * <p>Está aparte porque la usan {@code NegocioService}, {@code ModeracionService},
 * {@code DirectorioService}, {@code FotoService} y {@code ProductoService}, y
 * duplicarla acabaría con las versiones divergiendo.
 *
 * <p>Son métodos distintos y no uno con un interruptor: la vista pública omite
 * el estado y el motivo del rechazo, y esa diferencia debe ser imposible de
 * olvidar. Un solo método con un booleano se equivoca callado.
 */
final class NegocioMapper {

    /** Prefijo con el que se sirven las imágenes, el mismo que publica {@code /fotos/**}. */
    private static final String RUTA_PUBLICA_FOTOS = "/fotos/";

    private NegocioMapper() {
    }

    /** Lo que ve el dueño y lo que ve el administrador. */
    static NegocioResponse aRespuesta(Negocio negocio) {
        return new NegocioResponse(
                negocio.getId(), negocio.getNombre(), negocio.getDescripcion(),
                negocio.getTelefono(), negocio.getCategoria().getNombre(),
                negocio.getCiudad().getNombre(),
                negocio.getBarrio() == null ? null : negocio.getBarrio().getNombre(),
                negocio.getNivelPrecio().name(), negocio.getEstado().name(),
                negocio.getMotivoRechazo(), negocio.getCalificacionPromedio(),
                negocio.getNumeroOpiniones(), negocio.getInstagram(), negocio.getLinkedin());
    }

    /** La tarjeta del directorio: sin estado, sin motivo de rechazo y sin correo. */
    static NegocioPublicoResponse aRespuestaPublica(Negocio negocio, String fotoPrincipal) {
        return new NegocioPublicoResponse(
                negocio.getId(), negocio.getNombre(), negocio.getDescripcion(),
                negocio.getTelefono(), negocio.getCategoria().getNombre(),
                negocio.getCiudad().getNombre(),
                negocio.getBarrio() == null ? null : negocio.getBarrio().getNombre(),
                negocio.getNivelPrecio().name(), negocio.getCalificacionPromedio(),
                negocio.getNumeroOpiniones(), negocio.getFechaAprobacion(), fotoPrincipal);
    }

    /** El perfil completo: lo de la tarjeta más la galería y el escaparate. */
    static PerfilNegocioResponse aPerfil(Negocio negocio, List<Foto> fotos,
                                         List<Producto> productos) {
        return new PerfilNegocioResponse(
                negocio.getId(), negocio.getNombre(), negocio.getDescripcion(),
                negocio.getTelefono(), negocio.getCategoria().getNombre(),
                negocio.getCiudad().getNombre(),
                negocio.getBarrio() == null ? null : negocio.getBarrio().getNombre(),
                negocio.getNivelPrecio().name(), negocio.getCalificacionPromedio(),
                negocio.getNumeroOpiniones(), negocio.getFechaAprobacion(),
                negocio.getInstagram(), negocio.getLinkedin(),
                aRespuestasDeFoto(fotos), aRespuestasDeProducto(productos));
    }

    // ---------- Galería ----------

    /**
     * Traduce la galería marcando la principal.
     *
     * <p>La marca es la posición, no una columna: es principal la primera de la
     * lista que llega, que viene ordenada (B9). Así el propio orden decide y no
     * hay forma de acabar con dos principales o con ninguna.
     */
    static List<FotoResponse> aRespuestasDeFoto(List<Foto> fotos) {
        return java.util.stream.IntStream.range(0, fotos.size())
                .mapToObj(i -> aRespuestaDeFoto(fotos.get(i), i == 0))
                .toList();
    }

    static FotoResponse aRespuestaDeFoto(Foto foto, boolean principal) {
        return new FotoResponse(foto.getId(), urlDe(foto.getNombreArchivo()),
                foto.getOrden(), principal, foto.getEstado().name());
    }

    static String urlDe(String nombreArchivo) {
        return nombreArchivo == null ? null : RUTA_PUBLICA_FOTOS + nombreArchivo;
    }

    // ---------- Escaparate ----------

    static List<ProductoResponse> aRespuestasDeProducto(List<Producto> productos) {
        return productos.stream().map(NegocioMapper::aRespuestaDeProducto).toList();
    }

    static ProductoResponse aRespuestaDeProducto(Producto producto) {
        return new ProductoResponse(producto.getId(), producto.getNombre(),
                producto.getPrecio(), producto.getDescripcion(), producto.isDisponible(),
                urlDe(producto.getFoto()));
    }
}
