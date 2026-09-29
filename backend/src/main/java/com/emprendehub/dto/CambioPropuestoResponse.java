package com.emprendehub.dto;

import java.time.Instant;

/**
 * La propuesta de cambio **vista por su propio dueño**.
 *
 * <p>Sin esto, quien edita guarda, recarga la pantalla y vuelve a leer los
 * valores de antes sin ninguna señal de que hay algo en cola: parece que el
 * cambio se perdió, y es la revisión funcionando (B2-bis).
 *
 * <p>No es la misma vista que la del administrador
 * ({@link CambioPendienteResponse}): aquí no hacen falta los valores actuales,
 * porque llegan en el mismo negocio que envuelve esta propuesta.
 *
 * @param categoria la categoría propuesta, o {@code null} si la propuesta no la
 *     cambia
 */
public record CambioPropuestoResponse(
        String nombre,
        String descripcion,
        String categoria,
        Instant fechaSolicitud) {
}
