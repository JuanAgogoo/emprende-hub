package com.emprendehub.dto;

/**
 * Respuesta del alta de emprendedor: la misma que la de cualquier acceso, más
 * el identificador del negocio recién creado.
 *
 * <p>Se devuelve {@code negocioId} porque quien acaba de registrarse va derecho
 * a su negocio —a subir las fotos, por ejemplo— y hacerle pedir después
 * {@code GET /negocios/mio} para averiguar el identificador sería un viaje de
 * más por nada.
 *
 * @param negocioId el negocio, que nace {@code PENDIENTE} de revisión (B6)
 */
public record RegistroEmprendedorResponse(
        String token,
        String tipo,
        long expiraEnMillis,
        String nombre,
        String correo,
        String rol,
        Long negocioId) {

    public static RegistroEmprendedorResponse de(AuthResponse acceso, Long negocioId) {
        return new RegistroEmprendedorResponse(
                acceso.token(),
                acceso.tipo(),
                acceso.expiraEnMillis(),
                acceso.nombre(),
                acceso.correo(),
                acceso.rol(),
                negocioId);
    }
}
