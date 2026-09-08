package com.emprendehub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de un cliente: el registro corto de la decisión A1.
 *
 * <p>Es deliberadamente breve. El asistente de cuatro pasos del prototipo sirve
 * para inscribir un negocio, no para que alguien que solo quiere comprar cree
 * su cuenta.
 */
public record RegistroClienteRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 180, message = "El correo no puede pasar de 180 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String contrasena) {
}
