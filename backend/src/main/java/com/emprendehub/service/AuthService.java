package com.emprendehub.service;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.EditarRedesRequest;
import com.emprendehub.dto.LoginRequest;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.dto.RegistroEmprendedorRequest;
import com.emprendehub.dto.RegistroEmprendedorResponse;
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

import java.util.List;

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
    private final NegocioService negocioService;
    private final ProductoService productoService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       NegocioService negocioService, ProductoService productoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.negocioService = negocioService;
        this.productoService = productoService;
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
     * Da de alta un emprendedor con su negocio y su escaparate, todo en una
     * transacción.
     *
     * <p>La cuenta nace ya como {@code EMPRENDEDOR}, sin pasar por cliente: no
     * hay ascenso que hacer porque el negocio llega en la misma petición. El
     * camino de A1-bis sigue existiendo para quien se anima después, y es
     * {@code POST /negocios}.
     *
     * <p><strong>Que sea una sola transacción es el punto.</strong> Con tres
     * llamadas encadenadas, un fallo a mitad dejaría la cuenta creada y el
     * negocio sin registrar, y quien lo intentara de nuevo se encontraría su
     * propio correo cogido. O entra todo, o no entra nada.
     */
    @Transactional
    public RegistroEmprendedorResponse registrarEmprendedor(RegistroEmprendedorRequest peticion) {
        String correo = peticion.correo().trim().toLowerCase();

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ReglaDeNegocioException("Ya existe una cuenta con ese correo");
        }

        Usuario usuario = usuarioRepository.save(new Usuario(
                peticion.nombre().trim(),
                correo,
                passwordEncoder.encode(peticion.contrasena()),
                Rol.EMPRENDEDOR));

        // El negocio nace PENDIENTE y las reglas del alta —barrio que pertenece
        // a la ciudad, un negocio por cuenta— las sigue comprobando su servicio.
        NegocioResponse negocio = negocioService.registrar(usuario, peticion.negocio());

        // Las redes son opcionales y se aplican al momento, igual que en su
        // endpoint: no esperan revisión porque no cambian lo que se publicó.
        EditarRedesRequest redes = peticion.redes();
        if (redes != null) {
            negocioService.actualizarRedes(usuario, redes);
        }

        List<CrearProductoRequest> productos = peticion.productos();
        if (productos != null) {
            for (CrearProductoRequest producto : productos) {
                productoService.crear(usuario, producto);
            }
        }

        return RegistroEmprendedorResponse.de(respuestaPara(usuario), negocio.id());
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
