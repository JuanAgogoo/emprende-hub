package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.AuthResponse;
import com.emprendehub.dto.CrearProductoRequest;
import com.emprendehub.dto.EditarRedesRequest;
import com.emprendehub.dto.LoginRequest;
import com.emprendehub.dto.NegocioResponse;
import com.emprendehub.dto.RegistroClienteRequest;
import com.emprendehub.dto.RegistroEmprendedorRequest;
import com.emprendehub.dto.RegistroEmprendedorResponse;
import com.emprendehub.dto.RegistrarNegocioRequest;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.exception.ReglaDeNegocioException;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.UsuarioRepository;
import com.emprendehub.security.JwtService;
import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private NegocioService negocioService;

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

    // ---------- Registro de emprendedor ----------

    private RegistrarNegocioRequest negocioDePrueba() {
        return new RegistrarNegocioRequest("Panadería La Espiga",
                "Pan de masa madre horneado cada mañana en horno de leña, con harinas "
                        + "molidas a la piedra y fermentación lenta de 24 horas.",
                "3105551234", 1L, 1L, null, NivelPrecio.BAJO);
    }

    private RegistroEmprendedorRequest registroEmprendedorDePrueba() {
        return new RegistroEmprendedorRequest("Lucía Restrepo", "lucia@emprendehub.co",
                "contrasena123", negocioDePrueba(), null);
    }

    private NegocioResponse negocioCreado() {
        return new NegocioResponse(7L, "Panadería La Espiga", "descripción", "3105551234",
                "Gastronomía", "Medellín", null, "BAJO", "PENDIENTE",
                null, null, 0, null, null);
    }

    @Test
    @DisplayName("registrarEmprendedor: la cuenta nace EMPRENDEDOR, sin pasar por cliente")
    void registrarEmprendedor_valido_naceEmprendedor() {
        when(usuarioRepository.existsByCorreo("lucia@emprendehub.co")).thenReturn(false);
        when(passwordEncoder.encode("contrasena123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(negocioService.registrar(any(), any())).thenReturn(negocioCreado());
        when(jwtService.generarToken(any())).thenReturn("token-generado");
        when(jwtService.getDuracionMillis()).thenReturn(3600000L);

        RegistroEmprendedorResponse respuesta =
                service.registrarEmprendedor(registroEmprendedorDePrueba());

        assertEquals("token-generado", respuesta.token());
        assertEquals("EMPRENDEDOR", respuesta.rol());
        assertEquals(7L, respuesta.negocioId());

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertEquals(Rol.EMPRENDEDOR, capturado.getValue().getRol());
    }

    @Test
    @DisplayName("registrarEmprendedor: la contraseña se guarda con hash, nunca en claro")
    void registrarEmprendedor_valido_guardaLaContrasenaConHash() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode("contrasena123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(negocioService.registrar(any(), any())).thenReturn(negocioCreado());
        when(jwtService.generarToken(any())).thenReturn("token");

        service.registrarEmprendedor(registroEmprendedorDePrueba());

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertEquals("hash-bcrypt", capturado.getValue().getContrasena());
        assertNotEquals("contrasena123", capturado.getValue().getContrasena());
    }

    @Test
    @DisplayName("registrarEmprendedor: el correo repetido corta antes de crear nada")
    void registrarEmprendedor_correoRepetido_lanzaYNoCreaNada() {
        when(usuarioRepository.existsByCorreo("lucia@emprendehub.co")).thenReturn(true);

        assertThrows(ReglaDeNegocioException.class,
                () -> service.registrarEmprendedor(registroEmprendedorDePrueba()));

        verify(usuarioRepository, never()).save(any());
        verify(negocioService, never()).registrar(any(), any());
    }

    @Test
    @DisplayName("registrarEmprendedor: si el negocio falla, la excepción sube y deshace la transacción")
    void registrarEmprendedor_negocioInvalido_propagaLaExcepcion() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(negocioService.registrar(any(), any()))
                .thenThrow(new ReglaDeNegocioException("El barrio no pertenece a la ciudad"));

        assertThrows(ReglaDeNegocioException.class,
                () -> service.registrarEmprendedor(registroEmprendedorDePrueba()));

        // El usuario llegó a guardarse, pero la excepción sale del método
        // @Transactional: es Spring quien revierte, y por eso no puede quedar
        // una cuenta creada sin su negocio.
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("registrarEmprendedor: el correo se normaliza a minúsculas")
    void registrarEmprendedor_correoConMayusculas_seNormaliza() {
        when(usuarioRepository.existsByCorreo("lucia@emprendehub.co")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(negocioService.registrar(any(), any())).thenReturn(negocioCreado());
        when(jwtService.generarToken(any())).thenReturn("token");

        RegistroEmprendedorRequest peticion = new RegistroEmprendedorRequest(
                "Lucía Restrepo", "  LUCIA@EmprendeHub.CO  ", "contrasena123",
                negocioDePrueba(), null);

        service.registrarEmprendedor(peticion);

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        assertEquals("lucia@emprendehub.co", capturado.getValue().getCorreo());
    }

    @Test
    @DisplayName("registrarEmprendedor: las redes se aplican si vienen, y se omiten si no")
    void registrarEmprendedor_conRedes_lasAplica() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(negocioService.registrar(any(), any())).thenReturn(negocioCreado());
        when(jwtService.generarToken(any())).thenReturn("token");

        EditarRedesRequest redes = new EditarRedesRequest(
                "https://instagram.com/laespiga", "https://linkedin.com/company/laespiga");
        RegistroEmprendedorRequest peticion = new RegistroEmprendedorRequest(
                "Lucía Restrepo", "lucia@emprendehub.co", "contrasena123",
                negocioDePrueba(), redes);

        service.registrarEmprendedor(peticion);

        verify(negocioService).actualizarRedes(any(), eq(redes));
    }

    @Test
    @DisplayName("registrarEmprendedor: sin redes no se llama a su servicio")
    void registrarEmprendedor_sinRedes_noLasToca() {
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(negocioService.registrar(any(), any())).thenReturn(negocioCreado());
        when(jwtService.generarToken(any())).thenReturn("token");

        service.registrarEmprendedor(registroEmprendedorDePrueba());

        verify(negocioService, never()).actualizarRedes(any(), any());
    }
}
