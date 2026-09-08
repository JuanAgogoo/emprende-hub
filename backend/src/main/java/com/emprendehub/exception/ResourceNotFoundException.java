package com.emprendehub.exception;

/** El recurso pedido no existe. El manejador global la traduce a un 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, Object identificador) {
        super("No se encontró %s con identificador %s".formatted(recurso, identificador));
    }
}
