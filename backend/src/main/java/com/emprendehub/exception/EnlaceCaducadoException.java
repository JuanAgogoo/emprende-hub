package com.emprendehub.exception;

/**
 * Un enlace de un solo uso que ya no sirve: caducado, usado o inventado.
 *
 * <p>Se traduce a <strong>410 Gone</strong> y no a 404 a propósito: el recorrido
 * existe, lo que ya no vale es este enlace concreto. Los tres casos responden lo
 * mismo para no confirmar qué tokens existieron alguna vez.
 */
public class EnlaceCaducadoException extends RuntimeException {

    public EnlaceCaducadoException(String mensaje) {
        super(mensaje);
    }
}
