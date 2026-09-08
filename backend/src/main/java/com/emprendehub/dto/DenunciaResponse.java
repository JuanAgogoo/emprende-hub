package com.emprendehub.dto;

import java.time.Instant;

/**
 * Una denuncia como la ve el administrador en su cola.
 *
 * <p>Trae el texto denunciado, de quién es y sobre qué negocio: decidir si una
 * opinión se borra exige leerla, y obligar a abrir otra pantalla para verla
 * convertiría la revisión en un ir y venir.
 *
 * @param motivo el código del enum, que es lo que viaja en las peticiones
 * @param motivoDescripcion el texto que se enseña, ya traducido
 */
public record DenunciaResponse(
        Long id,
        Long opinionId,
        String negocio,
        String autorOpinion,
        int calificacion,
        String comentario,
        String motivo,
        String motivoDescripcion,
        String denunciante,
        Instant fecha) {
}
