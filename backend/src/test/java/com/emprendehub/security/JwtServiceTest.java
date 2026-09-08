package com.emprendehub.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del servicio de tokens, sin Spring de por medio: se construye a mano
 * con una clave y una duración.
 */
class JwtServiceTest {

    private static final String CLAVE =
            "ZW1wcmVuZGVodWItY2xhdmUtZGUtcHJ1ZWJhcy1kZS0yNTYtYml0cy1sYXJnYQ==";

    private final JwtService service = new JwtService(CLAVE, 3_600_000L);

    private Usuario usuario(String correo, Rol rol) {
        return new Usuario("Nombre", correo, "hash", rol);
    }

    @Test
    @DisplayName("generarToken: produce un JWT de tres partes separadas por puntos")
    void generarToken_produceTresPartes() {
        String token = service.generarToken(usuario("maria@test.co", Rol.CLIENTE));

        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("extraerCorreo: recupera el correo que se puso como sujeto")
    void extraerCorreo_devuelveElCorreo() {
        String token = service.generarToken(usuario("maria@test.co", Rol.CLIENTE));

        assertEquals("maria@test.co", service.extraerCorreo(token));
    }

    @Test
    @DisplayName("esValido: un token recién emitido para su dueño es válido")
    void esValido_tokenPropio_devuelveTrue() {
        Usuario maria = usuario("maria@test.co", Rol.CLIENTE);

        assertTrue(service.esValido(service.generarToken(maria), maria));
    }

    @Test
    @DisplayName("esValido: el token de una persona no sirve para otra")
    void esValido_tokenDeOtro_devuelveFalse() {
        String tokenDeMaria = service.generarToken(usuario("maria@test.co", Rol.CLIENTE));

        assertFalse(service.esValido(tokenDeMaria, usuario("carlos@test.co", Rol.CLIENTE)));
    }

    @Test
    @DisplayName("esValido: un token caducado deja de valer")
    void esValido_tokenCaducado_devuelveFalse() {
        // Duración negativa: nace ya caducado.
        JwtService caducado = new JwtService(CLAVE, -1000L);
        Usuario maria = usuario("maria@test.co", Rol.CLIENTE);
        String token = caducado.generarToken(maria);

        // jjwt rechaza los tokens caducados al analizarlos.
        assertThrows(Exception.class, () -> caducado.esValido(token, maria));
    }

    @Test
    @DisplayName("Un token firmado con otra clave no se acepta")
    void tokenFirmadoConOtraClave_seRechaza() {
        JwtService otroEmisor = new JwtService(
                "b3RyYS1jbGF2ZS1kaXN0aW50YS1kZS0yNTYtYml0cy1wYXJhLXBydWViYXM=", 3_600_000L);
        String tokenAjeno = otroEmisor.generarToken(usuario("maria@test.co", Rol.ADMIN));

        assertThrows(Exception.class, () -> service.extraerCorreo(tokenAjeno));
    }

    @Test
    @DisplayName("Un token manipulado no pasa la verificación de la firma")
    void tokenManipulado_seRechaza() {
        String token = service.generarToken(usuario("maria@test.co", Rol.CLIENTE));
        String manipulado = token.substring(0, token.length() - 4) + "AAAA";

        assertThrows(Exception.class, () -> service.extraerCorreo(manipulado));
    }

    @Test
    @DisplayName("getDuracionMillis: devuelve la duración configurada")
    void getDuracionMillis_devuelveLaConfigurada() {
        assertEquals(3_600_000L, service.getDuracionMillis());
    }
}
