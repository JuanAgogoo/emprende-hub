package com.emprendehub.dto;

import java.math.BigDecimal;

/**
 * Curso tal como lo consume el catálogo.
 *
 * <p>El {@code precio} viaja a null cuando el curso es gratuito, y
 * {@code urlRecurso} es el enlace externo al que se envía al usuario: la
 * plataforma no aloja el contenido ni cobra por él.
 */
public record CursoResponse(
        Long id,
        String titulo,
        String descripcion,
        String duracion,
        String categoria,
        String nivel,
        boolean gratuito,
        BigDecimal precio,
        String urlRecurso,
        String emoji,
        String estado) {
}
