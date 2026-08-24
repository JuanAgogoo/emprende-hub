package com.emprendehub.dto;

/**
 * Una imagen de la galería.
 *
 * @param url ruta pública desde la que se descarga la imagen
 * @param principal cierto solo en la primera por orden, que es la que hace de
 *     portada del negocio (B9). No sale de una columna: se calcula al mapear,
 *     porque el dato es «ser la primera», no una marca aparte
 * @param estado {@code PENDIENTE} mientras el administrador no la haya revisado.
 *     El público solo recibe aprobadas, así que solo tiene interés en el panel
 */
public record FotoResponse(
        Long id,
        String url,
        int orden,
        boolean principal,
        String estado) {
}
