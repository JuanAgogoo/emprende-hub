package com.emprendehub.dto;

import java.math.BigDecimal;

/**
 * Negocio tal como lo ve su dueño o el administrador.
 *
 * <p>Incluye el estado y el motivo del rechazo, que el dueño necesita leer en su
 * panel porque la plataforma no envía correos (I1).
 *
 * <p>El correo de la cuenta no aparece: es el identificador de acceso y no se
 * publica nunca.
 */
public record NegocioResponse(
        Long id,
        String nombre,
        String descripcion,
        String telefono,
        String categoria,
        String ciudad,
        String barrio,
        String nivelPrecio,
        String estado,
        String motivoRechazo,
        BigDecimal calificacionPromedio,
        int numeroOpiniones,
        String instagram,
        String linkedin) {
}
