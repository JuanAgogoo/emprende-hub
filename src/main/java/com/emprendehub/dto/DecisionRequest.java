package com.emprendehub.dto;

import jakarta.validation.constraints.Size;

/**
 * Cuerpo de un rechazo.
 *
 * <p>La aprobación no necesita cuerpo, por eso el motivo es opcional aquí y se
 * exige en el servicio solo cuando la decisión es rechazar.
 */
public record DecisionRequest(

        @Size(max = 1000, message = "El motivo no puede pasar de 1000 caracteres")
        String motivo) {
}
