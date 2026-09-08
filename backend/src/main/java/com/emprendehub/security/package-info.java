/**
 * Autenticación y autorización. Llega en el PR 5; el paquete se declara aquí para
 * que la estructura de capas esté completa desde el principio.
 *
 * <p>Sigue el taller guiado de autenticación del curso, con sus mismos nombres:
 * {@code SecurityConfig} (el bean {@code SecurityFilterChain}), {@code JwtService},
 * {@code JwtAuthenticationFilter} y {@code ApplicationConfig}.
 *
 * <p>La API es stateless: sin sesión, sin CSRF, y el token viaja en la cabecera
 * {@code Authorization: Bearer}.
 */
package com.emprendehub.security;
