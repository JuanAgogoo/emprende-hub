package com.emprendehub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de los campos que ve el público (B2).
 *
 * <p>Qué pasa con él depende del estado del negocio y del interruptor de
 * moderación: con la revisión encendida y el negocio ya publicado se guarda como
 * propuesta y espera turno, sin que el negocio deje de estar publicado con sus
 * valores anteriores (B2-bis). En cualquier otro caso se aplica al momento.
 *
 * @param categoriaId opcional. Nulo significa que la categoría no cambia, que es
 *     lo que manda una propuesta abierta solo por fotos
 */
public record EditarNegocioPublicoRequest(

        @NotBlank(message = "El nombre del negocio es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 80, max = 2000, message = "La descripción debe tener al menos 80 caracteres")
        String descripcion,

        Long categoriaId) {
}
