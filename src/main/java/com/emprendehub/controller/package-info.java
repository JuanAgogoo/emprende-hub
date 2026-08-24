/**
 * Capa de entrada HTTP. Clases {@code @RestController} bajo la ruta
 * {@code /api/<recurso>}.
 *
 * <p>Traduce peticiones HTTP a llamadas al servicio y devuelve records de
 * {@code dto}. <strong>Nunca llama al repositorio directamente</strong> ni
 * devuelve entidades de {@code model}.
 */
package com.emprendehub.controller;
