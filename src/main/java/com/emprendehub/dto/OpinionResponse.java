package com.emprendehub.dto;

import java.time.Instant;

/**
 * Una opinión tal como se publica.
 *
 * <p>Del autor sale su <strong>nombre y nada más</strong>. El correo es el
 * identificador de acceso y no se publica nunca, ni siquiera aquí, donde el
 * prototipo llegaba a firmar una reseña como {@code anonimo@mail.com}.
 *
 * @param editada cierto si su autor la cambió después de publicarla (C2). Se
 *     enseña para que quien la lea sepa que el texto no es el original
 */
public record OpinionResponse(
        Long id,
        String autor,
        int calificacion,
        String comentario,
        Instant fechaCreacion,
        boolean editada) {
}
