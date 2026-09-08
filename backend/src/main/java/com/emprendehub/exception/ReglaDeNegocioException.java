package com.emprendehub.exception;

/**
 * Una regla del dominio impide completar la operación.
 *
 * <p>No es un fallo de formato de la petición, que eso lo detecta Bean
 * Validation, sino una condición que solo el servicio puede comprobar. Se
 * traduce a un 400.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
