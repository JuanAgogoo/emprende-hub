package com.emprendehub.dto;

import com.emprendehub.model.Consulta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Consulta de un cliente a un negocio.
 *
 * <p>No lleva ni nombre ni correo de quien pregunta: contactar exige sesión, así
 * que esos datos salen de la cuenta y no de un formulario donde cualquiera
 * podría escribir el correo de otra persona.
 */
public record EnviarConsultaRequest(

        @NotBlank(message = "El asunto es obligatorio")
        @Size(max = Consulta.MAXIMO_ASUNTO,
                message = "El asunto no puede pasar de 120 caracteres")
        String asunto,

        @NotBlank(message = "El mensaje es obligatorio")
        @Size(max = Consulta.MAXIMO_MENSAJE,
                message = "El mensaje no puede pasar de 500 caracteres")
        String mensaje) {
}
