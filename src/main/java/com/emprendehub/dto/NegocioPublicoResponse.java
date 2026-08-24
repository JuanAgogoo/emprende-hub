package com.emprendehub.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Negocio tal como lo ve cualquiera, con o sin sesión.
 *
 * <p>Se diferencia de {@link NegocioResponse} en lo que <strong>no</strong>
 * lleva: ni estado ni motivo de rechazo. Aquí solo llegan negocios aprobados
 * (B6), así que ese par de campos no diría nada, y el motivo del rechazo es una
 * conversación entre el dueño y el administrador.
 *
 * <p>El teléfono sí es público —es el canal de contacto que anuncia el perfil—;
 * el correo no aparece nunca, porque es el identificador de acceso a la cuenta.
 *
 * <p>La misma forma sirve para la tarjeta del directorio y para el perfil: hoy
 * no hay nada que enseñar en uno y ocultar en el otro.
 *
 * @param calificacionPromedio nulo mientras no haya ninguna opinión, no cero (C5)
 * @param fechaAprobacion cuándo entró en el directorio; ordena «Más recientes» (G7)
 */
public record NegocioPublicoResponse(
        Long id,
        String nombre,
        String descripcion,
        String telefono,
        String categoria,
        String ciudad,
        String barrio,
        String nivelPrecio,
        BigDecimal calificacionPromedio,
        int numeroOpiniones,
        Instant fechaAprobacion) {
}
