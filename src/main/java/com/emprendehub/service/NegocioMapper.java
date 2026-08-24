package com.emprendehub.service;

import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.model.Negocio;

/**
 * Traducción de {@link Negocio} a su respuesta.
 *
 * <p>Está aparte porque la usan tanto {@code NegocioService} como
 * {@code ModeracionService}, y duplicarla acabaría con las dos versiones
 * divergiendo.
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
}
