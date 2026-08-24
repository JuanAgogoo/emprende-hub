package com.emprendehub.dto;

import java.time.Instant;

/**
 * Un aviso del panel del emprendedor.
 *
 * @param tipo el código del enum, por si el cliente quiere pintar cada uno
 *     con su icono
 * @param texto lo que se escribió cuando ocurrió el hecho, no algo reconstruido
 *     después
 */
public record NotificacionResponse(
        Long id,
        String tipo,
        String texto,
        boolean leida,
        Instant fecha) {
}
