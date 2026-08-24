package com.emprendehub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Alta de un producto del escaparate.
 *
 * <p>Solo se valida la forma. Que el negocio sea de quien lo crea es una regla
 * de negocio y se comprueba en el servicio.
 */
public record CrearProductoRequest(

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

        boolean disponible) {
}
