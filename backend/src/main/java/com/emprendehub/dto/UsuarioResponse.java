package com.emprendehub.dto;

import java.time.Instant;

/**
 * Una cuenta tal como la ve el administrador en su listado (HU-038).
 *
 * <p>Lleva el correo, que no sale en ninguna respuesta pública: quien gestiona
 * las cuentas necesita saber a quién suspende, y dos personas pueden llamarse
 * igual.
 *
 * @param activo {@code false} si está suspendida (B4)
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String rol,
        Instant fechaRegistro,
        boolean activo) {
}
