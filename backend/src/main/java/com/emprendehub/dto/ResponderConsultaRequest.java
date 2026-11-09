package com.emprendehub.dto;

import com.emprendehub.model.Consulta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** La respuesta del dueño a una consulta de su buzón (D2). */
public record ResponderConsultaRequest(

        @NotBlank(message = "La respuesta es obligatoria")
        @Size(max = Consulta.MAXIMO_MENSAJE,
                message = "La respuesta no puede pasar de 500 caracteres")
        String respuesta) {
}
