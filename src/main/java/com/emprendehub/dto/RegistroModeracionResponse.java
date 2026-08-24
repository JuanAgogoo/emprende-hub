package com.emprendehub.dto;

import java.time.Instant;

/** Entrada del log de moderación tal como la lee el panel del administrador. */
public record RegistroModeracionResponse(
        Long id,
        String tipo,
        String descripcionTipo,
        String afectado,
        String detalle,
        String administrador,
        Instant fecha) {
}
