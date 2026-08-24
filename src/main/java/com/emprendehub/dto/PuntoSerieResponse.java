package com.emprendehub.dto;

import java.time.LocalDate;

/**
 * Un día de la gráfica de visitas.
 *
 * <p>La serie llega <strong>con los días vacíos incluidos</strong>, en cero: si
 * se saltaran, la gráfica del panel uniría el lunes con el jueves y aparentaría
 * una caída que no existió.
 */
public record PuntoSerieResponse(LocalDate fecha, long visitas) {
}
