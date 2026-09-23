package com.emprendehub.service;

import com.emprendehub.dto.RestablecerContrasenaRequest;
import com.emprendehub.dto.SolicitarRecuperacionRequest;
import com.emprendehub.exception.EnlaceCaducadoException;
import com.emprendehub.model.TokenRecuperacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.TokenRecuperacionRepository;
import com.emprendehub.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperar la contraseña sin saber la anterior.
 *
 * <p>Reabre I1, que había cerrado el dominio sin correos. Las dos reglas que
 * hacen que esto sea una recuperación y no un agujero:
 *
 * <ul>
 *   <li><strong>El token sale solo por correo.</strong> Devolverlo en la
 *       respuesta encadenaría bien las pantallas, y de paso dejaría cambiar la
 *       contraseña de cualquiera sabiendo únicamente su dirección.</li>
 *   <li><strong>Pedirlo responde igual exista la cuenta o no.</strong> Un 404
 *       para un correo desconocido convertiría el formulario en una forma de
 *       averiguar qué direcciones están registradas.</li>
 * </ul>
 */
@Service
public class RecuperacionService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);

    /** 32 bytes de aleatoriedad, que es lo que recomienda cualquier guía seria. */
    private static final int BYTES_DEL_TOKEN = 32;

    private static final SecureRandom AZAR = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CorreoService correoService;

    /** Cuánto vive un enlace. Treinta minutos, como dice el plan. */
    private final long minutosDeVida;

    public RecuperacionService(UsuarioRepository usuarioRepository,
                               TokenRecuperacionRepository tokenRepository,
                               PasswordEncoder passwordEncoder,
                               CorreoService correoService,
                               @Value("${emprendehub.recuperacion.minutos}") long minutosDeVida) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoService = correoService;
        this.minutosDeVida = minutosDeVida;
    }

    /**
     * Crea un enlace y lo manda por correo. <strong>Siempre termina bien.</strong>
     *
     * <p>Un correo sin cuenta no genera nada y no manda nada, pero responde lo
     * mismo que uno que sí la tiene. Y si el servidor de correo no contesta, la
     * petición tampoco falla: queda en el registro y quien lo pidió puede
     * volver a intentarlo, que es mejor que un 500 que además delataría que esa
     * cuenta existe.
     */
    @Transactional
    public void solicitar(SolicitarRecuperacionRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();
        Optional<Usuario> cuenta = usuarioRepository.findByCorreo(correo);

        if (cuenta.isEmpty()) {
            return;
        }

        Usuario usuario = cuenta.get();
        TokenRecuperacion enlace = tokenRepository.save(new TokenRecuperacion(
                usuario, generarToken(), Instant.now().plus(Duration.ofMinutes(minutosDeVida))));

        try {
            correoService.enviarRecuperacion(usuario, enlace.getToken(), minutosDeVida);
        } catch (MailException ex) {
            log.error("No se pudo enviar el correo de recuperación a {}", correo, ex);
        }
    }

    /**
     * Dice si un enlace todavía vale, antes de enseñar el formulario.
     *
     * <p>Existe para que la pantalla pueda decir «este enlace ya no sirve» antes
     * de que alguien escriba una contraseña nueva dos veces.
     */
    public void comprobar(String token) {
        buscarEnlaceUtil(token);
    }

    /**
     * Cambia la contraseña y gasta el enlace, en la misma transacción.
     *
     * <p>El mínimo de ocho caracteres lo comprueba la validación del DTO, la
     * misma regla de A6 que el alta.
     */
    @Transactional
    public void restablecer(String token, RestablecerContrasenaRequest peticion) {
        TokenRecuperacion enlace = buscarEnlaceUtil(token);

        Usuario usuario = enlace.getUsuario();
        usuario.setContrasena(passwordEncoder.encode(peticion.contrasena()));
        usuarioRepository.save(usuario);

        // Marcado y no borrado: el segundo intento con el mismo enlace tiene que
        // poder responder que ya no vale.
        enlace.setUsado(true);
        tokenRepository.save(enlace);
    }

    /**
     * El enlace, si sirve. Caducado, gastado e inventado dan lo mismo: un 410,
     * para no confirmar cuáles existieron.
     */
    private TokenRecuperacion buscarEnlaceUtil(String token) {
        return tokenRepository.findByToken(token)
                .filter(enlace -> enlace.sirve(Instant.now()))
                .orElseThrow(() -> new EnlaceCaducadoException(
                        "Este enlace de recuperación ya no es válido. Pide uno nuevo."));
    }

    private String generarToken() {
        byte[] bytes = new byte[BYTES_DEL_TOKEN];
        AZAR.nextBytes(bytes);
        // Sin relleno y en el alfabeto de URL: el token viaja dentro de un enlace.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
