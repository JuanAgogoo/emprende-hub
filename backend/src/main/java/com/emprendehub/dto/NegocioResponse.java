package com.emprendehub.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Negocio tal como lo ve su dueño o el administrador.
 *
 * <p>Incluye el estado y el motivo del rechazo, que el dueño necesita leer en su
 * panel porque la plataforma no envía correos (I1).
 *
 * <p>El correo de la cuenta no aparece: es el identificador de acceso y no se
 * publica nunca.
 *
 * @param fechaCreacion cuándo se registró. La cola del administrador la enseña
 *     y ordena por ella: lo que lleva más tiempo esperando va primero (B5)
 * @param cambioPendiente la propuesta que espera revisión, o {@code null} si no
 *     hay ninguna. Solo se rellena para el dueño, que es quien necesita saber
 *     que editó y que su cambio está en cola (B2-bis)
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
        String linkedin,
        Instant fechaCreacion,
        CambioPropuestoResponse cambioPendiente) {
}
