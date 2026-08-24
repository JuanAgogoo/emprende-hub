package com.emprendehub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Emisión y validación de los tokens JWT.
 *
 * <p>Un JWT tiene tres partes: cabecera, cuerpo con los reclamos y firma. La
 * firma garantiza que nadie lo alteró, pero <strong>el cuerpo solo va
 * codificado en Base64, no cifrado</strong>: por eso aquí solo viajan el correo
 * y el rol, nunca la contraseña ni ningún dato sensible.
 */
@Service
public class JwtService {

    private final String claveSecreta;
    private final long duracionMillis;

    public JwtService(@Value("${jwt.secret-key}") String claveSecreta,
                      @Value("${jwt.expiration}") long duracionMillis) {
        this.claveSecreta = claveSecreta;
        this.duracionMillis = duracionMillis;
    }

    /** Genera un token para el usuario, con su rol como reclamo adicional. */
    public String generarToken(UserDetails usuario) {
        String rol = usuario.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("");

        Date ahora = new Date();
        return Jwts.builder()
                .claims(Map.of("rol", rol))
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracionMillis))
                .signWith(clave())
                .compact();
    }

    public String extraerCorreo(String token) {
        return extraerReclamo(token, Claims::getSubject);
    }

    /** Válido si es de este usuario y todavía no ha caducado. */
    public boolean esValido(String token, UserDetails usuario) {
        return extraerCorreo(token).equals(usuario.getUsername()) && !haCaducado(token);
    }

    public long getDuracionMillis() {
        return duracionMillis;
    }

    private boolean haCaducado(String token) {
        return extraerReclamo(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerReclamo(String token, Function<Claims, T> extractor) {
        Claims reclamos = Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) clave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return extractor.apply(reclamos);
    }

    private Key clave() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(claveSecreta));
    }
}
