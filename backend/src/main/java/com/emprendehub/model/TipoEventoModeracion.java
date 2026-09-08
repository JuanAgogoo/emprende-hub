package com.emprendehub.model;

import lombok.Getter;

/** Acciones del administrador que quedan registradas en el log (sección L). */
@Getter
public enum TipoEventoModeracion {

    NEGOCIO_APROBADO("Negocio aprobado"),
    NEGOCIO_RECHAZADO("Negocio rechazado"),
    CAMBIO_APROBADO("Cambio pendiente aprobado"),
    CAMBIO_RECHAZADO("Cambio pendiente rechazado"),
    CUENTA_SUSPENDIDA("Cuenta suspendida"),
    CUENTA_REACTIVADA("Cuenta reactivada"),
    CURSO_PUBLICADO("Curso publicado"),
    OPINION_ELIMINADA("Opinión eliminada"),
    DENUNCIA_DESESTIMADA("Denuncia desestimada");

    private final String descripcion;

    TipoEventoModeracion(String descripcion) {
        this.descripcion = descripcion;
    }
}
