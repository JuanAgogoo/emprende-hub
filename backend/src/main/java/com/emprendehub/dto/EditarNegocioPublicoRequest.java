package com.emprendehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de los campos que ve el público (B2).
 *
 * <p>No se aplica al momento: se guarda como propuesta y espera revisión, sin
 * que el negocio deje de estar publicado con sus valores anteriores (B2-bis).
 */
public record EditarNegocioPublicoRequest(

        @NotBlank(message = "El nombre del negocio es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 80, max = 2000, message = "La descripción debe tener al menos 80 caracteres")
        String descripcion) {
}
