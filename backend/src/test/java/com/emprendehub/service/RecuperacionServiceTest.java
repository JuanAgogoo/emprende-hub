package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.emprendehub.dto.RestablecerContrasenaRequest;
import com.emprendehub.dto.SolicitarRecuperacionRequest;
import com.emprendehub.exception.EnlaceCaducadoException;
import com.emprendehub.model.Rol;
import com.emprendehub.model.TokenRecuperacion;
import com.emprendehub.model.Usuario;
import com.emprendehub.repository.TokenRecuperacionRepository;
import com.emprendehub.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * La recuperación de contraseña, con todo lo de fuera simulado.
 *
 * <p>Las dos reglas que más se comprueban aquí son las de la decisión 3 del
 * plan: que un correo desconocido no delata nada y que el token no sale por
 * ninguna parte que no sea el correo.
 */
@ExtendWith(MockitoExtension.class)
class RecuperacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TokenRecuperacionRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CorreoService correoService;

    /**
     * Se construye a mano y no con {@code @InjectMocks} porque el constructor
     * lleva un {@code long} —los minutos de vida del enlace— y Mockito no sabe
     * qué poner en un primitivo.
     */
    private RecuperacionService service;

    private static final long MINUTOS = 30L;
    private static final String CORREO = "maria@gmail.com";

    @BeforeEach
    void prepararServicio() {
        service = new RecuperacionService(
                usuarioRepository, tokenRepository, passwordEncoder, correoService, MINUTOS);
    }

    private Usuario usuarioDePrueba() {
        Usuario usuario = new Usuario("María García", CORREO, "hash-viejo", Rol.CLIENTE);
        usuario.setId(1L);
        return usuario;
    }

    private TokenRecuperacion enlaceDe(Usuario usuario, Instant caducidad, boolean usado) {
        TokenRecuperacion enlace = new TokenRecuperacion(usuario, "token-de-prueba", caducidad);
        enlace.setUsado(usado);
        return enlace;
    }

    // ---------- Solicitar ----------

    @Test
    @DisplayName("solicitar: con un correo conocido guarda el enlace y lo manda")
    void solicitar_correoConocido_guardaYEnvia() {
        //arrange
        Usuario usuario = usuarioDePrueba();
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(tokenRepository.save(any(TokenRecuperacion.class))).thenAnswer(i -> i.getArgument(0));

        //act
        service.solicitar(new SolicitarRecuperacionRequest(CORREO));

        //assert
        ArgumentCaptor<TokenRecuperacion> guardado =
                ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).save(guardado.capture());
        assertEquals(usuario, guardado.getValue().getUsuario());
        assertFalse(guardado.getValue().isUsado());

        //verify
        verify(correoService).enviarRecuperacion(usuario, guardado.getValue().getToken(), MINUTOS);
    }

    @Test
    @DisplayName("solicitar: un correo desconocido no guarda nada, no manda nada y no falla")
    void solicitar_correoDesconocido_noHaceNadaYNoFalla() {
        //arrange
        when(usuarioRepository.findByCorreo("nadie@gmail.com")).thenReturn(Optional.empty());

        //act
        service.solicitar(new SolicitarRecuperacionRequest("nadie@gmail.com"));

        //assert
        // Responder 200 sin haber hecho nada es la decisión 3: un 404 aquí
        // convertiría el formulario en una lista de qué correos tienen cuenta.

        //verify
        verifyNoInteractions(tokenRepository);
        verifyNoInteractions(correoService);
    }

    @Test
    @DisplayName("solicitar: el correo se normaliza a minúsculas y sin espacios")
    void solicitar_normalizaElCorreo() {
        //arrange
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        //act
        service.solicitar(new SolicitarRecuperacionRequest("  MARIA@Gmail.com  "));

        //assert
        //verify
        verify(usuarioRepository).findByCorreo(CORREO);
    }

    @Test
    @DisplayName("solicitar: el enlace caduca a los treinta minutos")
    void solicitar_elEnlaceCaducaALaMediaHora() {
        //arrange
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioDePrueba()));
        when(tokenRepository.save(any(TokenRecuperacion.class))).thenAnswer(i -> i.getArgument(0));
        Instant antes = Instant.now();

        //act
        service.solicitar(new SolicitarRecuperacionRequest(CORREO));

        //assert
        ArgumentCaptor<TokenRecuperacion> guardado =
                ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).save(guardado.capture());
        Instant caducidad = guardado.getValue().getCaducidad();
        assertTrue(caducidad.isAfter(antes.plus(Duration.ofMinutes(MINUTOS - 1))));
        assertTrue(caducidad.isBefore(antes.plus(Duration.ofMinutes(MINUTOS + 1))));
    }

    @Test
    @DisplayName("solicitar: dos peticiones no generan el mismo token")
    void solicitar_dosVeces_generaTokensDistintos() {
        //arrange
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioDePrueba()));
        when(tokenRepository.save(any(TokenRecuperacion.class))).thenAnswer(i -> i.getArgument(0));

        //act
        service.solicitar(new SolicitarRecuperacionRequest(CORREO));
        service.solicitar(new SolicitarRecuperacionRequest(CORREO));

        //assert
        ArgumentCaptor<TokenRecuperacion> guardados =
                ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository, org.mockito.Mockito.times(2)).save(guardados.capture());
        String primero = guardados.getAllValues().get(0).getToken();
        String segundo = guardados.getAllValues().get(1).getToken();
        assertNotEquals(primero, segundo);
        assertEquals(TokenRecuperacion.LONGITUD_TOKEN, primero.length());
    }

    @Test
    @DisplayName("solicitar: si el servidor de correo no contesta, la petición no falla")
    void solicitar_correoQueNoSale_noPropaganElFallo() {
        //arrange
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(usuarioDePrueba()));
        when(tokenRepository.save(any(TokenRecuperacion.class))).thenAnswer(i -> i.getArgument(0));
        doThrow(new MailSendException("SMTP caído"))
                .when(correoService).enviarRecuperacion(any(), anyString(), anyLong());

        //act
        service.solicitar(new SolicitarRecuperacionRequest(CORREO));

        //assert
        // Un 500 aquí delataría que esa cuenta existe, porque una desconocida
        // responde 200. Queda en el registro y se puede volver a pedir.

        //verify
        verify(tokenRepository).save(any(TokenRecuperacion.class));
    }

    // ---------- Comprobar ----------

    @Test
    @DisplayName("comprobar: un enlace vivo y sin usar no lanza nada")
    void comprobar_enlaceValido_pasa() {
        //arrange
        TokenRecuperacion enlace = enlaceDe(
                usuarioDePrueba(), Instant.now().plus(Duration.ofMinutes(10)), false);
        when(tokenRepository.findByToken("token-de-prueba")).thenReturn(Optional.of(enlace));

        //act
        service.comprobar("token-de-prueba");

        //assert
        //verify
        verify(tokenRepository).findByToken("token-de-prueba");
    }

    @Test
    @DisplayName("comprobar: un enlace caducado responde que ya no vale")
    void comprobar_enlaceCaducado_lanza410() {
        //arrange
        TokenRecuperacion enlace = enlaceDe(
                usuarioDePrueba(), Instant.now().minus(Duration.ofMinutes(1)), false);
        when(tokenRepository.findByToken("token-de-prueba")).thenReturn(Optional.of(enlace));

        //act
        //assert
        assertThrows(EnlaceCaducadoException.class, () -> service.comprobar("token-de-prueba"));
    }

    @Test
    @DisplayName("comprobar: un enlace ya usado responde que ya no vale")
    void comprobar_enlaceUsado_lanza410() {
        //arrange
        TokenRecuperacion enlace = enlaceDe(
                usuarioDePrueba(), Instant.now().plus(Duration.ofMinutes(10)), true);
        when(tokenRepository.findByToken("token-de-prueba")).thenReturn(Optional.of(enlace));

        //act
        //assert
        assertThrows(EnlaceCaducadoException.class, () -> service.comprobar("token-de-prueba"));
    }

    @Test
    @DisplayName("comprobar: un token inventado responde lo mismo que uno caducado")
    void comprobar_tokenInventado_lanza410() {
        //arrange
        when(tokenRepository.findByToken("me-lo-invento")).thenReturn(Optional.empty());

        //act
        //assert
        // Mismo error que los otros dos casos: distinguirlos confirmaría qué
        // tokens llegaron a existir.
        assertThrows(EnlaceCaducadoException.class, () -> service.comprobar("me-lo-invento"));
    }

    // ---------- Restablecer ----------

    @Test
    @DisplayName("restablecer: guarda la contraseña nueva con hash y gasta el enlace")
    void restablecer_enlaceValido_cambiaLaContrasenaYMarcaElEnlace() {
        //arrange
        Usuario usuario = usuarioDePrueba();
        TokenRecuperacion enlace = enlaceDe(
                usuario, Instant.now().plus(Duration.ofMinutes(10)), false);
        when(tokenRepository.findByToken("token-de-prueba")).thenReturn(Optional.of(enlace));
        when(passwordEncoder.encode("contrasenaNueva")).thenReturn("hash-nuevo");

        //act
        service.restablecer("token-de-prueba", new RestablecerContrasenaRequest("contrasenaNueva"));

        //assert
        assertEquals("hash-nuevo", usuario.getContrasena());
        assertNotEquals("contrasenaNueva", usuario.getContrasena());
        assertTrue(enlace.isUsado());

        //verify
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).save(enlace);
    }

    @Test
    @DisplayName("restablecer: con un enlace caducado no toca la contraseña")
    void restablecer_enlaceCaducado_noCambiaNada() {
        //arrange
        Usuario usuario = usuarioDePrueba();
        TokenRecuperacion enlace = enlaceDe(
                usuario, Instant.now().minus(Duration.ofMinutes(1)), false);
        when(tokenRepository.findByToken("token-de-prueba")).thenReturn(Optional.of(enlace));

        //act
        //assert
        assertThrows(EnlaceCaducadoException.class, () -> service.restablecer(
                "token-de-prueba", new RestablecerContrasenaRequest("contrasenaNueva")));
        assertEquals("hash-viejo", usuario.getContrasena());

        //verify
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("restablecer: el mismo enlace no sirve dos veces")
    void restablecer_enlaceYaUsado_lanza410() {
        //arrange
        TokenRecuperacion enlace = enlaceDe(
                usuarioDePrueba(), Instant.now().plus(Duration.ofMinutes(10)), true);
        when(tokenRepository.findByToken("token-de-prueba")).thenReturn(Optional.of(enlace));

        //act
        //assert
        assertThrows(EnlaceCaducadoException.class, () -> service.restablecer(
                "token-de-prueba", new RestablecerContrasenaRequest("contrasenaNueva")));

        //verify
        verify(usuarioRepository, never()).save(any());
    }
}
