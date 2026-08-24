package com.emprendehub.service;

import com.emprendehub.dto.NegocioPublicoResponse;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.model.Negocio;

/**
 * Traducción de {@link Negocio} a sus dos respuestas.
 *
 * <p>Está aparte porque la usan {@code NegocioService}, {@code ModeracionService}
 * y {@code DirectorioService}, y duplicarla acabaría con las versiones
 * divergiendo.
 *
 * <p>Son dos métodos y no uno con un interruptor: la vista pública omite el
 * estado y el motivo del rechazo, y esa diferencia debe ser imposible de
 * olvidar. Un solo método con un booleano se equivoca callado.
 */
final class NegocioMapper {

    private NegocioMapper() {
    }

    static NegocioResponse aRespuesta(Negocio negocio) {
        return new NegocioResponse(
                negocio.getId(), negocio.getNombre(), negocio.getDescripcion(),
                negocio.getTelefono(), negocio.getCategoria().getNombre(),
                negocio.getCiudad().getNombre(),
                negocio.getBarrio() == null ? null : negocio.getBarrio().getNombre(),
                negocio.getNivelPrecio().name(), negocio.getEstado().name(),
                negocio.getMotivoRechazo(), negocio.getCalificacionPromedio(),
                negocio.getNumeroOpiniones());
    }

    /** Lo que ve el público: sin estado ni motivo de rechazo, y sin correo. */
    static NegocioPublicoResponse aRespuestaPublica(Negocio negocio) {
        return new NegocioPublicoResponse(
                negocio.getId(), negocio.getNombre(), negocio.getDescripcion(),
                negocio.getTelefono(), negocio.getCategoria().getNombre(),
                negocio.getCiudad().getNombre(),
                negocio.getBarrio() == null ? null : negocio.getBarrio().getNombre(),
                negocio.getNivelPrecio().name(), negocio.getCalificacionPromedio(),
                negocio.getNumeroOpiniones(), negocio.getFechaAprobacion());
    }
}
