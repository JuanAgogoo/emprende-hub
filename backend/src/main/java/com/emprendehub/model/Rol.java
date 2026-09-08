package com.emprendehub.model;

import lombok.Getter;

/**
 * Perfiles del sistema (sección A de las decisiones de dominio).
 *
 * <p>El taller del curso usa {@code USER} y {@code ADMIN}; aquí hacen falta tres
 * porque cliente y emprendedor hacen cosas distintas.
 *
 * <p>El visitante anónimo no aparece: no es un rol, es la ausencia de sesión.
 */
@Getter
public enum Rol {

    /** Opina, califica y escribe a los negocios. */
    CLIENTE("Cliente"),

    /** Todo lo del cliente, más la gestión de su propio negocio. */
    EMPRENDEDOR("Emprendedor"),

    /** Aprueba negocios, suspende cuentas, publica cursos y modera. */
    ADMIN("Admin");

    private final String nombre;

    Rol(String nombre) {
        this.nombre = nombre;
    }

    /** Spring Security espera el prefijo {@code ROLE_} en las autoridades. */
    public String comoAutoridad() {
        return "ROLE_" + name();
    }
}
