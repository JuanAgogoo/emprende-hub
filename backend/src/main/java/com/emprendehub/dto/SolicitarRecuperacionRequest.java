package com.emprendehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición de un enlace para recuperar la contraseña.
 *
 * <p>Las mismas reglas de forma que el correo del registro. Que el correo sea
 * válido no quiere decir que exista una cuenta con él: eso no se contesta
 * nunca, ni aquí ni en la respuesta.
 */
public record SolicitarRecuperacionRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 180, message = "El correo no puede pasar de 180 caracteres")
        String correo) {
}
