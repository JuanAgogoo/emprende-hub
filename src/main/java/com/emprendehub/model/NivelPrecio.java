package com.emprendehub.model;

import lombok.Getter;

/**
 * Franja de precios del negocio, la que el directorio muestra como $, $$ y $$$.
 *
 * <p>Lo elige el dueño al registrarse (G4). El prototipo ofrecía también un
 * filtro «Gratis» que se descarta: aplicado a un negocio no significa nada y
 * venía copiado de la pantalla de cursos.
 */
@Getter
public enum NivelPrecio {

    BAJO("$"),
    MEDIO("$$"),
    ALTO("$$$");

    private final String simbolo;

    NivelPrecio(String simbolo) {
        this.simbolo = simbolo;
    }
}
