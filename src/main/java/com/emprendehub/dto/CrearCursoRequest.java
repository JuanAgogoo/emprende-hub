package com.emprendehub.dto;

import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.NivelCurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Alta de un curso.
 *
 * <p>Aquí solo se valida la forma. La coherencia entre {@code gratuito} y
 * {@code precio} es una regla de negocio y vive en el servicio, porque depende
 * de dos campos a la vez.
 */
public record CrearCursoRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede pasar de 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 1000, message = "La descripción no puede pasar de 1000 caracteres")
        String descripcion,

        @NotBlank(message = "La duración es obligatoria")
        @Size(max = 40, message = "La duración no puede pasar de 40 caracteres")
        String duracion,

        @NotNull(message = "La categoría es obligatoria")
        CategoriaCurso categoria,

        @NotNull(message = "El nivel es obligatorio")
        NivelCurso nivel,

        @NotNull(message = "Hay que indicar si el curso es gratuito")
        Boolean gratuito,

        @Positive(message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @NotBlank(message = "La URL del recurso es obligatoria")
        @Size(max = 500, message = "La URL no puede pasar de 500 caracteres")
        String urlRecurso,

        @NotBlank(message = "El emoji es obligatorio")
        @Size(max = 8, message = "El emoji no puede pasar de 8 caracteres")
        String emoji) {
}
