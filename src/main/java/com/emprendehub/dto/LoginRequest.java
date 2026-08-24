package com.emprendehub.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales de acceso. */
public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena) {
}
