package com.emprendehub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepta cada petición, saca el token de la cabecera y, si es válido, deja
 * al usuario autenticado en el {@code SecurityContextHolder}.
 *
 * <p>Nunca rechaza por sí mismo: si no hay token o no sirve, deja pasar sin
 * autenticar y es la cadena de filtros la que decide si esa ruta lo permitía.
 * Eso es lo que hace posible la <em>autenticación opcional</em> que necesita el
 * perfil público para no contar la visita de su propio dueño (H1).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        String cabecera = peticion.getHeader(CABECERA);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        String token = cabecera.substring(PREFIJO.length());
        try {
            String correo = jwtService.extraerCorreo(token);
            if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = userDetailsService.loadUserByUsername(correo);
                if (jwtService.esValido(token, usuario)) {
                    var autenticacion = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
                    autenticacion.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(peticion));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                }
            }
        } catch (Exception ex) {
            // Token corrupto, caducado o de un usuario que ya no existe: se
            // sigue sin autenticar y la cadena decidirá si la ruta lo tolera.
            SecurityContextHolder.clearContext();
        }

        cadena.doFilter(peticion, respuesta);
    }
}
