package com.emprendehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La contraseña nueva, al final del recorrido de recuperación.
 *
 * <p>Mismo mínimo de ocho caracteres que el alta (A6): recuperar la cuenta no
 * es la ocasión de rebajar la regla. La repetición del campo es cosa del
 * formulario; al servidor llega una sola vez.
 */
public record RestablecerContrasenaRequest(

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String contrasena) {
}
