package com.emprendehub.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de filtros de la API.
 *
 * <p>Traduce a reglas la matriz de acceso de {@code docs/arquitectura.md}. Dos
 * cosas del taller que conviene tener claras al sustentar:
 * <ul>
 *   <li><strong>CSRF deshabilitado</strong>: es un ataque dirigido a cookies, y
 *       esta API no usa ninguna.</li>
 *   <li><strong>Sesiones desactivadas</strong>: el estado viaja en el token del
 *       cliente, el servidor no guarda nada.</li>
 * </ul>
 *
 * <p>Dos reglas no caben aquí y viven en los servicios: la propiedad del recurso
 * (tener rol EMPRENDEDOR no da acceso al negocio de otro) y la visibilidad de
 * los negocios pendientes o rechazados (B6).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthenticationProvider authenticationProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // El reenvío interno a /error tiene que ser público. Si no,
                        // el error de una ruta protegida se vuelve a filtrar ya sin
                        // token, cae en anyRequest().authenticated() y el 401 acaba
                        // pisando al 403 que había devuelto la autorización.
                        .requestMatchers("/error").permitAll()
                        // Registro e inicio de sesión: público por definición.
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // El directorio se explora sin registrarse, así que él,
                        // los catálogos, el catálogo de cursos y las cifras de la
                        // portada son públicos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalogos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/cursos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/directorio/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/estadisticas/**").permitAll()
                        // Las imágenes de los perfiles son parte del directorio
                        // público: se sirven como recurso estático, fuera de
                        // /api, y no llevan token en la etiqueta <img>.
                        .requestMatchers(HttpMethod.GET, "/fotos/**").permitAll()
                        // Toda la gestión cuelga de /admin y es solo del ADMIN.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Sin esto, una petición sin token a una ruta protegida
                // devolvería 403. Un 401 es lo correcto: falta autenticarse,
                // no es que falte permiso.
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (peticion, respuesta, excepcion) ->
                                respuesta.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "Se requiere autenticación")))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
