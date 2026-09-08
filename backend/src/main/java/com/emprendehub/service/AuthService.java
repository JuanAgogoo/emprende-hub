package com.emprendehub.service;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.LoginRequest;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.UsuarioRepository;
import com.emprendehub.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro e inicio de sesión.
 *
 * <p>Sin correos de por medio (I1): no hay verificación de dirección ni
 * recuperación de contraseña. Quien la olvide tiene que pedírsela al
 * administrador.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Da de alta un cliente y lo deja ya autenticado.
     *
     * <p>El correo es único en todo el sistema: es el identificador de la
     * cuenta. Un cliente que más adelante registre un negocio conservará esta
     * misma cuenta y solo cambiará de rol (A1-bis).
     */
    @Transactional
    public AuthResponse registrarCliente(RegistroClienteRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ReglaDeNegocioException("Ya existe una cuenta con ese correo");
        }

        Usuario usuario = new Usuario(
                peticion.nombre().trim(),
                correo,
                passwordEncoder.encode(peticion.contrasena()),
                Rol.CLIENTE);

        return respuestaPara(usuarioRepository.save(usuario));
    }

    /**
     * Inicia sesión.
     *
     * <p>El {@code AuthenticationManager} es quien compara el hash y quien
     * rechaza a las cuentas suspendidas, porque {@code Usuario.isEnabled()}
     * devuelve el campo {@code activo} (B4).
     */
    public AuthResponse iniciarSesion(LoginRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(correo, peticion.contrasena()));

        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ReglaDeNegocioException("Credenciales incorrectas"));

        return respuestaPara(usuario);
    }

    private AuthResponse respuestaPara(Usuario usuario) {
        return AuthResponse.de(
                jwtService.generarToken(usuario),
                jwtService.getDuracionMillis(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol().name());
    }
}
