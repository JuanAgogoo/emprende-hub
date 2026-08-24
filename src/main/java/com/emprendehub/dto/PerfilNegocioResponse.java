package com.emprendehub.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * El perfil público completo de un negocio.
 *
 * <p>Hasta el PR 9 la tarjeta del directorio y el perfil devolvían lo mismo,
 * porque no había nada que enseñar en uno y ocultar en el otro. Las fotos y los
 * productos rompen ese empate: la tarjeta necesita <em>una</em> imagen y el
 * perfil la galería entera con el escaparate, así que son dos respuestas.
 *
 * <p>Sigue sin llevar estado ni motivo de rechazo —aquí solo llegan negocios
 * aprobados (B6)— y el correo no aparece nunca.
 *
 * @param fotos solo las aprobadas, en orden; la primera es la principal (B9)
 * @param productos el escaparate, que no se vende (F1)
 */
public record PerfilNegocioResponse(
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
        Instant fechaAprobacion,
        String instagram,
        String linkedin,
        List<FotoResponse> fotos,
        List<ProductoResponse> productos) {
}
