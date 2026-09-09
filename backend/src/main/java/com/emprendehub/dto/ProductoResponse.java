package com.emprendehub.dto;

import java.math.BigDecimal;

/**
 * Un producto o servicio del escaparate.
 *
 * <p>Lleva precio porque la ficha lo enseña, pero no hay forma de comprarlo
 * desde aquí (F1).
 *
 * @param descripcion puede venir a nulo: es opcional (F2)
 * @param disponible el único estado que existe; no hay inventario (F3)
 */
public record ProductoResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        String descripcion,
        boolean disponible,
        /** URL pública de su imagen. Nunca nula: sin foto no hay producto. */
        String foto) {
}
