package com.emprendehub.model;

import lombok.Getter;

/**
 * Estado de publicación de un curso (E4).
 *
 * <p>Solo los {@code PUBLICADO} aparecen en el catálogo público; los
 * {@code BORRADOR} únicamente los ve el administrador desde su panel.
 */
@Getter
public enum EstadoCurso {

    BORRADOR("Borrador"),
    PUBLICADO("Publicado");

    private final String nombre;

    EstadoCurso(String nombre) {
        this.nombre = nombre;
    }
}
