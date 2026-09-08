package com.emprendehub.config;

import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Crea la cuenta del administrador la primera vez que arranca.
 *
 * <p>El administrador no puede registrarse por su cuenta (A3): no hay ningún
 * endpoint que cree cuentas con rol ADMIN, así que tiene que nacer aquí.
 *
 * <p>La contraseña pasa por el {@code PasswordEncoder} como cualquier otra;
 * nunca se guarda en claro.
 */
@Configuration
public class CargaInicialAdmin {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialAdmin.class);

    /**
     * @implNote El administrador no depende de nada.
     */
    @Order(2)
    @Bean
    public CommandLineRunner cargarAdmin(UsuarioRepository usuarioRepository,
                                         PasswordEncoder passwordEncoder,
                                         @Value("${admin.correo:admin@emprendehub.co}") String correo,
                                         @Value("${admin.contrasena:admin12345}") String contrasena,
                                         @Value("${admin.nombre:Administrador}") String nombre) {
        return args -> {
            if (usuarioRepository.existsByCorreo(correo)) {
                log.info("El administrador ya existía, no se toca nada.");
                return;
            }
            usuarioRepository.save(new Usuario(
                    nombre, correo, passwordEncoder.encode(contrasena), Rol.ADMIN));
            log.info("Creada la cuenta de administrador con el correo {}.", correo);
        };
    }
}
