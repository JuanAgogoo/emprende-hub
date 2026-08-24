package com.emprendehub.dto;

import com.emprendehub.model.Opinion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Alta de una opinión.
 *
 * <p>El comentario es opcional: calificar con estrellas sin escribir nada es una
 * opinión válida. La calificación no lo es, porque es lo único que entra en el
 * promedio del negocio.
 */
public record CrearOpinionRequest(

        @Min(value = Opinion.CALIFICACION_MINIMA, message = "La calificación va de 1 a 5")
        @Max(value = Opinion.CALIFICACION_MAXIMA, message = "La calificación va de 1 a 5")
        int calificacion,

        @Size(max = Opinion.MAXIMO_COMENTARIO,
                message = "El comentario no puede pasar de 300 caracteres")
        String comentario) {
}
