package com.emprendehub.model;

import lombok.Getter;

/**
 * Los hechos de los que se avisa al emprendedor (H2).
 *
 * <p>Es una lista cerrada y corta a propósito: <strong>cada una nace de algo que
 * pasó</strong>, no de un cálculo. Quedan fuera las de hitos de visitas
 * («superaste las 1.000 este mes»), que el prototipo enseñaba y H2 descarta: no
 * son un hecho del que haya que enterarse, son una felicitación.
 *
 * <p>Sin correos (I1), el panel es el único canal de avisos, así que estas
 * cuatro dejan de ser decorativas.
 */
@Getter
public enum TipoNotificacion {

    OPINION_NUEVA("Nueva opinión"),
    CONSULTA_NUEVA("Nueva consulta"),
    NEGOCIO_APROBADO("Negocio aprobado"),
    NEGOCIO_RECHAZADO("Negocio rechazado");

    private final String descripcion;

    TipoNotificacion(String descripcion) {
        this.descripcion = descripcion;
    }
}
