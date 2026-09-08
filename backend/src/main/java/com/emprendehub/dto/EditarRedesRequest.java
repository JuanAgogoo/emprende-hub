package com.emprendehub.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Enlaces a redes sociales del negocio (B8).
 *
 * <p>Los dos son opcionales y se aplican al instante: no son contenido que el
 * administrador tenga que revisar, sino la misma clase de dato de contacto que
 * el teléfono (B2).
 *
 * <p>Se valida que la dirección sea del dominio que dice ser. Sin esa
 * comprobación, el campo «Instagram» del perfil podría enlazar a cualquier
 * sitio, que es justo lo que un perfil público no debe hacer.
 */
public record EditarRedesRequest(

        @Pattern(regexp = "^$|^https://(www\\.)?instagram\\.com/[A-Za-z0-9._]{1,60}/?$",
                message = "El enlace debe ser un perfil de Instagram "
                        + "(https://instagram.com/tu-cuenta)")
        String instagram,

        @Pattern(regexp = "^$|^https://(www\\.)?linkedin\\.com/(in|company)/[A-Za-z0-9\\-]{1,80}/?$",
                message = "El enlace debe ser un perfil de LinkedIn "
                        + "(https://linkedin.com/in/tu-cuenta)")
        String linkedin) {
}
