package com.emprendehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Alta de un emprendedor con su negocio, en una sola petición.
 *
 * <p>No es el registro de cliente con más campos: quien llega a publicar un
 * negocio trae desde el principio su nombre, su descripción, su contacto, su
 * ubicación y su escaparate. Hacerlo entrar por {@link RegistroClienteRequest}
 * obligaría a meter un formulario dentro de otro.
 *
 * <p>Las validaciones no se reescriben: el negocio y los productos reutilizan
 * las de {@link RegistrarNegocioRequest} y {@link CrearProductoRequest}, que se
 * aplican en cascada gracias a {@code @Valid}.
 *
 * <p>Las fotos se quedan fuera a propósito. Son binarios y ya tienen su endpoint
 * multipart, que además valida tipo, tamaño y máximo; traerlas aquí obligaría a
 * mezclar JSON y ficheros en la misma petición y a duplicar esa validación.
 */
public record RegistroEmprendedorRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 180, message = "El correo no puede pasar de 180 caracteres")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String contrasena,

        @NotNull(message = "Los datos del negocio son obligatorios")
        @Valid
        RegistrarNegocioRequest negocio,

        /**
         * Opcional. Va aparte del negocio porque el alta de A1-bis tampoco las
         * pide: las redes tienen su propio endpoint y su propia validación de
         * dominio (B8), que aquí se reutiliza tal cual.
         */
        @Valid
        EditarRedesRequest redes,

        /** Opcional: quien todavía no tiene escaparate lo monta después. */
        @Valid
        List<CrearProductoRequest> productos) {
}
