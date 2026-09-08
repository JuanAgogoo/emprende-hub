package com.emprendehub.dto;

/**
 * Valor de un conjunto cerrado que se ofrece como opción al cliente.
 *
 * <p>El {@code codigo} es el nombre de la constante del enum, que es lo que
 * viaja de vuelta en las peticiones; el {@code nombre} es lo que se enseña.
 */
public record OpcionResponse(String codigo, String nombre) {
}
