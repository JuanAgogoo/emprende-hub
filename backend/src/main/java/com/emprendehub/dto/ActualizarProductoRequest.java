package com.emprendehub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Edición de un producto. Mismos campos y mismas reglas que el alta. */
public record ActualizarProductoRequest(

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String nombre,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2,
                message = "El precio admite como mucho dos decimales")
        BigDecimal precio,

        @Size(max = 500, message = "La descripción no puede pasar de 500 caracteres")
        String descripcion,

        // Obligatorio y sin valor por defecto: un producto que falta marcar no
        // se da por disponible ni por agotado. Es Boolean para que llegue nulo y
        // lo diga la validación, en vez de un error de conversión en inglés.
        @NotNull(message = "Hay que indicar si el producto está disponible")
        Boolean disponible) {
}
