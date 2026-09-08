package com.emprendehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio del teléfono.
 *
 * <p>Se aplica al instante: no es un campo que el administrador tenga que
 * revisar, y obligar a esperar tres días para corregir un dígito no tendría
 * sentido (B2).
 */
public record EditarContactoRequest(

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^(3\\d{9}|60\\d{8})$",
                message = "El teléfono debe ser un móvil (3XXXXXXXXX) o un fijo (60XXXXXXXX)")
        String telefono) {
}
