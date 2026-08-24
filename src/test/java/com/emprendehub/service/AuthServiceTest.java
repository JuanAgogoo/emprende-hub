package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.LoginRequest;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.UsuarioRepository;
import com.emprendehub.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    private RegistroClienteRequest registroDePrueba() {
        return new RegistroClienteRequest("María García", "maria@gmail.com", "contrasena123");
    }

    private Usuario clienteDePrueba() {
        Usuario usuario = new Usuario("María García", "maria@gmail.com", "hash", Rol.CLIENTE);
        usuario.setId(1L);
        return usuario;
    }

    // ---------- Registro ----------

    @Test
    @DisplayName("registrarCliente: guarda al usuario y devuelve un token")
    void registrarCliente_valido_devuelveToken() {
        when(usuarioRepository.existsByCorreo("maria@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("contrasena123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(any())).thenReturn("token-generado");
        when(jwtService.getDuracionMillis()).thenReturn(3600000L);

        AuthResponse respuesta = service.registrarCliente(registroDePrueba());

        assertEquals("token-generado", respuesta.token());
        assertEquals("Bearer", respuesta.tipo());
        assertEquals("CLIENTE", respuesta.rol());
    }

    @Test
    @DisplayName("registrarCliente: la contraseña se guarda con hash, nunca en claro")
    void registrarCliente_guardaLaContrasenaConHash() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode("contrasena123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(any())).thenReturn("token");

        service.registrarCliente(registroDePrueba());

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals("hash-bcrypt", guardado.getValue().getContrasena());
        assertNotEquals("contrasena123", guardado.getValue().getContrasena());
    }

    @Test
    @DisplayName("registrarCliente: el correo se normaliza a minúsculas y sin espacios")
    void registrarCliente_normalizaElCorreo() {
        when(usuarioRepository.existsByCorreo("maria@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(any())).thenReturn("token");

        service.registrarCliente(new RegistroClienteRequest(
                "  María  ", "  MARIA@Gmail.com  ", "contrasena123"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals("maria@gmail.com", guardado.getValue().getCorreo());
        assertEquals("María", guardado.getValue().getNombre());
    }

    @Test
    @DisplayName("registrarCliente: nace siempre con rol CLIENTE, nunca ADMIN")
    void registrarCliente_naceComoCliente() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(any())).thenReturn("token");

        service.registrarCliente(registroDePrueba());

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals(Rol.CLIENTE, guardado.getValue().getRol());
        assertTrue(guardado.getValue().isActivo());
    }

    @Test
    @DisplayName("registrarCliente: un correo repetido se rechaza y no se guarda")
    void registrarCliente_correoRepetido_lanzaExcepcion() {
        when(usuarioRepository.existsByCorreo("maria@gmail.com")).thenReturn(true);

        var excepcion = assertThrows(ReglaDeNegocioException.class,
                () -> service.registrarCliente(registroDePrueba()));

        assertTrue(excepcion.getMessage().contains("Ya existe"));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    // ---------- Inicio de sesión ----------

    @Test
    @DisplayName("iniciarSesion: con credenciales correctas devuelve el token")
    void iniciarSesion_correcto_devuelveToken() {
        when(usuarioRepository.findByCorreo("maria@gmail.com"))
                .thenReturn(Optional.of(clienteDePrueba()));
        when(jwtService.generarToken(any())).thenReturn("token-generado");
        when(jwtService.getDuracionMillis()).thenReturn(3600000L);

        AuthResponse respuesta = service.iniciarSesion(
                new LoginRequest("maria@gmail.com", "contrasena123"));

        assertEquals("token-generado", respuesta.token());
        assertEquals("María García", respuesta.nombre());
    }

    @Test
    @DisplayName("iniciarSesion: normaliza el correo antes de autenticar")
    void iniciarSesion_normalizaElCorreo() {
        when(usuarioRepository.findByCorreo("maria@gmail.com"))
                .thenReturn(Optional.of(clienteDePrueba()));
        when(jwtService.generarToken(any())).thenReturn("token");

        service.iniciarSesion(new LoginRequest("  MARIA@Gmail.com ", "contrasena123"));

        verify(usuarioRepository).findByCorreo("maria@gmail.com");
    }

    @Test
    @DisplayName("iniciarSesion: si el gestor rechaza las credenciales, no emite token")
    void iniciarSesion_credencialesMalas_noEmiteToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        assertThrows(BadCredentialsException.class, () -> service.iniciarSesion(
                new LoginRequest("maria@gmail.com", "equivocada")));

        verify(jwtService, never()).generarToken(any());
    }

    @Test
    @DisplayName("iniciarSesion: usuario autenticado que ya no está en la base de datos")
    void iniciarSesion_usuarioDesaparecido_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("maria@gmail.com")).thenReturn(Optional.empty());

        assertThrows(ReglaDeNegocioException.class, () -> service.iniciarSesion(
                new LoginRequest("maria@gmail.com", "contrasena123")));
    }
}
