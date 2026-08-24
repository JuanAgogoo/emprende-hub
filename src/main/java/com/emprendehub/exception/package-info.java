/**
 * Excepciones de negocio y su traducción a respuestas HTTP.
 *
 * <p>El servicio lanza excepciones propias; el {@code @RestControllerAdvice} de
 * este paquete es el único sitio que decide códigos de estado. Ninguna otra
 * capa conoce HTTP.
 */
package com.emprendehub.exception;
