package com.emprendehub.model;

import lombok.Getter;

/**
 * Estado de un negocio en el ciclo de moderación (sección B).
 *
 * <p>Nace {@code PENDIENTE} y el administrador decide. Un negocio que no esté
 * {@code APROBADO} solo lo ven su dueño y el administrador (B6); para el resto
 * del mundo no existe.
 *
 * <p>La suspensión no aparece aquí: vive en el usuario, no en el negocio (B4).
 */
@Getter
public enum EstadoNegocio {

    PENDIENTE("Pendiente de aprobación"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado");

    private final String nombre;

    EstadoNegocio(String nombre) {
        this.nombre = nombre;
    }
}
