/**
 * Configuración de infraestructura: los {@code @Bean} que no son ni regla de
 * negocio ni acceso a datos.
 *
 * <p>Aquí viven el {@code Clock} inyectable, la publicación de las fotos como
 * recurso web, la escritura de imágenes en disco y los cuatro cargadores que
 * siembran la base al arrancar. <strong>El orden de los cargadores lo fija
 * {@code @Order}</strong> (catálogos, admin, cursos, demo); sin él la siembra
 * falla buscando una categoría que todavía no existe.
 *
 * <p>La autenticación y el JWT <strong>no</strong> están aquí, sino en
 * {@code com.emprendehub.security}.
 */
package com.emprendehub.config;
