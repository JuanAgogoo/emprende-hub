package com.emprendehub.model;

import lombok.Getter;

/** Dificultad de un curso. Conjunto cerrado de 3 valores. */
@Getter
public enum NivelCurso {

    BASICO("Básico"),
    INTERMEDIO("Intermedio"),
    AVANZADO("Avanzado");

    private final String nombre;

    NivelCurso(String nombre) {
        this.nombre = nombre;
    }
}
