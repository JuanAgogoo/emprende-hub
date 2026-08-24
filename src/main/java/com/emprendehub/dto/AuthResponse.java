package com.emprendehub.dto;

/**
 * Respuesta de un registro o un inicio de sesión.
 *
 * <p>El {@code token} se envía después en la cabecera
 * {@code Authorization: Bearer <token>}. Van también el nombre y el rol para
 * que el cliente no tenga que descodificar el token solo para saber a quién ha
 * autenticado.
 */
public record AuthResponse(
        String token,
        String tipo,
        long expiraEnMillis,
        String nombre,
        String correo,
        String rol) {

    public static AuthResponse de(String token, long expiraEnMillis,
                                  String nombre, String correo, String rol) {
        return new AuthResponse(token, "Bearer", expiraEnMillis, nombre, correo, rol);
    }
}
