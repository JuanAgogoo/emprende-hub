package com.emprendehub.dto;

import java.time.Instant;

/**
 * Una propuesta de cambio esperando revisión, como la ve el administrador.
 *
 * <p>Lleva el valor actual junto al propuesto porque revisar es comparar: sin el
 * de antes, el administrador tendría que abrir el perfil en otra pestaña para
 * saber qué está cambiando.
 *
 * @param fotosPendientes cuántas imágenes nuevas espera publicar esta propuesta.
 *     Una propuesta puede ser solo de fotos, y entonces el nombre y la
 *     descripción propuestos son iguales a los actuales
 */
public record CambioPendienteResponse(
        Long negocioId,
        String nombreActual,
        String nombrePropuesto,
        String descripcionActual,
        String descripcionPropuesta,
        int fotosPendientes,
        Instant fechaSolicitud) {
}
