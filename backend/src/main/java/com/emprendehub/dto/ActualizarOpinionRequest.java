package com.emprendehub.dto;

import com.emprendehub.model.Opinion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edición de la opinión propia. Mismos campos y mismas reglas que el alta. */
public record ActualizarOpinionRequest(

        // Integer y no int: sin nota, la validación responde por el campo en vez de
        // un error de lectura del JSON que no dice qué falta.
        @NotNull(message = "La calificación es obligatoria")
        @Min(value = Opinion.CALIFICACION_MINIMA, message = "La calificación va de 1 a 5")
        @Max(value = Opinion.CALIFICACION_MAXIMA, message = "La calificación va de 1 a 5")
        Integer calificacion,

        @Size(max = Opinion.MAXIMO_COMENTARIO,
                message = "El comentario no puede pasar de 300 caracteres")
        String comentario) {
}
