package com.emprendehub.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidadorProduccionTest {

    // 32 bytes en Base64: valores inventados solo para la prueba.
    private static final String JWT = "dW5hLWNsYXZlLWRlLXBydWViYS1kZS0zMi1ieXRlcy14eA==";
    private static final String BD = "bd-de-prueba";
    private static final String CORREO = "alguien@ejemplo.test";
    private static final String ADMIN = "admin-de-prueba";
    private static final String DEMO = "demo-de-prueba";

    private List<String> con(String jwt, String bd, String correo, String admin,
                             boolean semilla, String demo) {
        return ValidadorProduccion.problemas(jwt, bd, correo, admin, semilla, demo);
    }

    @Test
    @DisplayName("Con credenciales propias no hay ningún problema")
    void credencialesPropias_noHayProblemas() {
        assertTrue(con(JWT, BD, CORREO, ADMIN, true, DEMO).isEmpty());
    }

    @Test
    @DisplayName("La clave JWT del repositorio se rechaza")
    void jwtPublico_seRechaza() {
        assertEquals(1, con(ValidadorProduccion.JWT_PUBLICO, BD, CORREO, ADMIN, true, DEMO).size());
    }

    @Test
    @DisplayName("Una clave JWT corta o que no es Base64 se rechaza")
    void jwtInvalido_seRechaza() {
        assertEquals(1, con("Y29ydGE=", BD, CORREO, ADMIN, true, DEMO).size());
        assertEquals(1, con("esto no es base64 !!", BD, CORREO, ADMIN, true, DEMO).size());
    }

    @Test
    @DisplayName("Las credenciales públicas de la base y del administrador se rechazan")
    void credencialesPublicas_seRechazan() {
        assertEquals(1, con(JWT, ValidadorProduccion.CONTRASENA_BD_PUBLICA, CORREO, ADMIN, true, DEMO).size());
        assertEquals(1, con(JWT, BD, ValidadorProduccion.CORREO_ADMIN_PUBLICO, ADMIN, true, DEMO).size());
        assertEquals(1, con(JWT, BD, CORREO, ValidadorProduccion.CONTRASENA_ADMIN_PUBLICA, true, DEMO).size());
    }

    @Test
    @DisplayName("Con la semilla activa, la contraseña de las cuentas de demo tiene que ser propia")
    void semillaActiva_exigeContrasenaPropia() {
        assertEquals(1, con(JWT, BD, CORREO, ADMIN, true, "").size());
        assertEquals(1, con(JWT, BD, CORREO, ADMIN, true, "demo12345").size());
    }

    @Test
    @DisplayName("Con la semilla apagada no hace falta contraseña de demo")
    void semillaApagada_noExigeContrasena() {
        assertTrue(con(JWT, BD, CORREO, ADMIN, false, "").isEmpty());
    }

    @Test
    @DisplayName("El mensaje de error dice qué falla sin repetir el secreto")
    void elMensaje_noRepiteElSecreto() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new ValidadorProduccion(JWT, BD, CORREO, ValidadorProduccion.CONTRASENA_ADMIN_PUBLICA, false, ""));

        assertTrue(error.getMessage().contains("ADMIN_CONTRASENA"));
        assertFalse(error.getMessage().contains(ValidadorProduccion.CONTRASENA_ADMIN_PUBLICA));
    }

    @Test
    @DisplayName("Con todo correcto el bean se crea")
    void todoCorrecto_elBeanSeCrea() {
        assertDoesNotThrow(() -> new ValidadorProduccion(JWT, BD, CORREO, ADMIN, true, DEMO));
    }
}
