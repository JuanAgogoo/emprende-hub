package com.emprendehub.dto;

import com.emprendehub.model.MotivoDenuncia;
import jakarta.validation.constraints.NotNull;

/**
 * Denuncia de una opinión (C3).
 *
 * <p>El motivo sale de una <strong>lista cerrada</strong> (C6): no hay campo de
 * texto libre, así que un motivo inventado devuelve 400 y la cola del
 * administrador se puede agrupar y contar.
 */
public record DenunciarOpinionRequest(

        @NotNull(message = "Hay que indicar por qué se denuncia")
        MotivoDenuncia motivo) {
}
