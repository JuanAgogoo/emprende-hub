package com.emprendehub.dto;

import com.emprendehub.model.NivelPrecio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta de un negocio en el directorio.
 *
 * <p>Llega en <strong>una sola petición</strong>. Los cuatro pasos del prototipo
 * son una división visual del formulario, no cuatro envíos: el backend crea el
 * negocio en una única transacción.
 *
 * <p>Los mínimos de longitud vienen del prototipo: 3 caracteres de nombre y 80
 * de descripción.
 */
public record RegistrarNegocioRequest(

        @NotBlank(message = "El nombre del negocio es obligatorio")
        @Size(min = 3, max = 120, message = "El nombre debe tener entre 3 y 120 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 80, max = 2000,
                message = "La descripción debe tener al menos 80 caracteres")
        String descripcion,

        /*
         * Móvil: 10 dígitos empezando por 3. Fijo: 10 empezando por 60.
         * El patrón del prototipo solo admitía móviles, lo que dejaba fuera a
         * cualquier local con línea fija (G8).
         */
        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = "^(3\\d{9}|60\\d{8})$",
                message = "El teléfono debe ser un móvil (3XXXXXXXXX) o un fijo (60XXXXXXXX)")
        String telefono,

        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId,

        @NotNull(message = "La ciudad es obligatoria")
        Long ciudadId,

        /** Opcional: solo Medellín tiene barrios en el catálogo. */
        Long barrioId,

        @NotNull(message = "El nivel de precio es obligatorio")
        NivelPrecio nivelPrecio) {
}
