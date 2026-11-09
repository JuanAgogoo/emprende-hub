package com.emprendehub.config;

import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Impide arrancar en producción con las credenciales públicas del repositorio.
 *
 * <p>Se comprueba al crear el bean, antes de que corra ningún cargador de datos:
 * un administrador con la contraseña del README no llega a existir en la base.
 * Solo vive en el perfil {@code prod}; en desarrollo esos valores son los
 * esperados.
 */
@Component
@Profile("prod")
public class ValidadorProduccion {

    static final String JWT_PUBLICO = "ZW1wcmVuZGVodWItY2xhdmUtZGUtZGVzYXJyb2xsby1zb2xvLTI1Ni1iaXRz";
    static final String CORREO_ADMIN_PUBLICO = "admin@emprendehub.co";
    static final String CONTRASENA_ADMIN_PUBLICA = "admin12345";
    static final String CONTRASENA_BD_PUBLICA = "emprendehub";
    static final Set<String> CONTRASENAS_DEMO_PUBLICAS =
            Set.of("cliente12345", "emprende12345", "demo12345");
    private static final int BYTES_MINIMOS_JWT = 32;

    public ValidadorProduccion(@Value("${jwt.secret-key}") String jwt,
                               @Value("${spring.datasource.password}") String contrasenaBd,
                               @Value("${admin.correo:admin@emprendehub.co}") String correoAdmin,
                               @Value("${admin.contrasena:admin12345}") String contrasenaAdmin,
                               @Value("${emprendehub.semilla-demo:true}") boolean semillaActiva,
                               @Value("${emprendehub.semilla-demo-contrasena:}") String contrasenaDemo) {
        List<String> problemas =
                problemas(jwt, contrasenaBd, correoAdmin, contrasenaAdmin, semillaActiva, contrasenaDemo);
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("Configuración insegura para producción: "
                    + String.join("; ", problemas));
        }
    }

    /** Devuelve qué falla, sin repetir nunca el valor de un secreto. */
    static List<String> problemas(String jwt, String contrasenaBd, String correoAdmin,
                                  String contrasenaAdmin, boolean semillaActiva,
                                  String contrasenaDemo) {
        List<String> problemas = new ArrayList<>();
        if (JWT_PUBLICO.equals(jwt)) {
            problemas.add("JWT_SECRET es la clave pública del repositorio");
        } else if (!claveValida(jwt)) {
            problemas.add("JWT_SECRET debe ser Base64 válido de al menos 256 bits");
        }
        if (contrasenaBd.isBlank() || CONTRASENA_BD_PUBLICA.equals(contrasenaBd)) {
            problemas.add("DB_PASSWORD está vacía o es la pública");
        }
        if (CORREO_ADMIN_PUBLICO.equalsIgnoreCase(correoAdmin)) {
            problemas.add("ADMIN_CORREO es el público del repositorio");
        }
        if (contrasenaAdmin.isBlank() || CONTRASENA_ADMIN_PUBLICA.equals(contrasenaAdmin)) {
            problemas.add("ADMIN_CONTRASENA está vacía o es la pública");
        }
        if (semillaActiva
                && (contrasenaDemo.isBlank() || CONTRASENAS_DEMO_PUBLICAS.contains(contrasenaDemo))) {
            problemas.add("SEMILLA_DEMO está activa sin una SEMILLA_DEMO_CONTRASENA propia");
        }
        return problemas;
    }

    private static boolean claveValida(String jwt) {
        try {
            return Decoders.BASE64.decode(jwt).length >= BYTES_MINIMOS_JWT;
        } catch (DecodingException ex) {
            return false;
        }
    }
}
