package com.emprendehub.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * El nuevo orden de la galería, de la primera a la última.
 *
 * <p>Llega la lista entera y no un movimiento suelto —«sube esta una posición»—
 * porque el orden es el estado, no la acción: con la lista completa el servidor
 * no tiene que reconstruir de qué posición venía nada, y dos pestañas abiertas no
 * pueden dejarlo a medias.
 *
 * <p><strong>La primera es la portada</strong> (B9). No hay un campo que la
 * marque: elegir portada es poner esa foto la primera.
 */
public record ReordenarFotosRequest(

        @NotEmpty(message = "Hay que enviar el orden de las fotos")
        List<Long> orden) {
}
