package com.emprendehub.model;

import lombok.Getter;

/**
 * Categorías de la formación del catálogo de cursos.
 *
 * <p>Es un conjunto cerrado de 5 valores, distinto e independiente de las 12
 * categorías de negocio: un curso nunca es de "Gastronomía" ni un negocio de
 * "marketing". Se modela como enum, y no como entidad, porque solo lo usa
 * {@code Curso} y nadie lo consulta por separado.
 */
@Getter
public enum CategoriaCurso {

    MARKETING("Marketing"),
    FINANZAS("Finanzas"),
    VENTAS("Ventas"),
    DIGITAL("Digital"),
    GESTION("Gestión");

    private final String nombre;

    CategoriaCurso(String nombre) {
        this.nombre = nombre;
    }
}
