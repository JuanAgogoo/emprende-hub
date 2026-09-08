package com.emprendehub.dto;

import com.emprendehub.model.NivelPrecio;
import java.math.BigDecimal;

/**
 * Los filtros del directorio, que llegan como parámetros de consulta.
 *
 * <p>Van juntos en un record en lugar de sueltos en la firma del servicio
 * porque son cinco y siempre viajan a la vez. <strong>Todos son opcionales</strong>:
 * cada uno a nulo desactiva su condición, así que una sola consulta cubre todas
 * las combinaciones.
 *
 * <p>Es un carrier sin lógica. La normalización del texto y la ordenación viven
 * en {@code DirectorioService}, que es donde se prueban.
 *
 * @param texto busca en nombre y descripción, sin distinguir mayúsculas (G6)
 * @param calificacionMinima deja fuera a los negocios sin ninguna opinión, que no
 *     tienen calificación y no deben ser castigados por ser nuevos (C5)
 */
public record BusquedaDirectorioRequest(
        String texto,
        Long categoriaId,
        Long ciudadId,
        Long barrioId,
        BigDecimal calificacionMinima,
        NivelPrecio nivelPrecio) {
}
