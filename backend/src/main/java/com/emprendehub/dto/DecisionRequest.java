package com.emprendehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de una decisión que exige motivo: rechazar un negocio, rechazar un
 * cambio propuesto o borrar una opinión denunciada.
 *
 * <p>Aprobar no lleva cuerpo, así que todas las rutas que reciben este DTO lo
 * necesitan con motivo. Antes era opcional aquí y lo exigía el constructor de
 * {@code DecisionModeracion.Rechazar}, cuya excepción no traducía nadie: un
 * motivo vacío devolvía 500 en vez de 400.
 */
public record DecisionRequest(

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 1000, message = "El motivo no puede pasar de 1000 caracteres")
        String motivo) {
}
