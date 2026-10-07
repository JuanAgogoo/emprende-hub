package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Los correos de la aplicación, sin servidor de por medio.
 *
 * <p>Lo que importa comprobar es que el enlace lleva el token y que va a la
 * dirección de la cuenta: si eso se rompe, el recorrido entero deja de
 * funcionar y no hay prueba de servicio que lo vea.
 */
@ExtendWith(MockitoExtension.class)
class CorreoServiceTest {

    @Mock
    private JavaMailSender mailSender;

    /** Se construye a mano: dos de sus tres dependencias son propiedades. */
    private CorreoService service;

    private static final String REMITENTE = "no-responder@emprendehub.co";
    private static final String URL_BASE = "http://localhost:5173";

    @BeforeEach
    void prepararServicio() {
        service = new CorreoService(mailSender, REMITENTE, URL_BASE);
    }

    @Test
    @DisplayName("enviarRecuperacion: manda el enlace con el token a la cuenta")
    void enviarRecuperacion_componeElMensaje() {
        //arrange
        Usuario usuario = new Usuario("María García", "maria@gmail.com", "hash", Rol.CLIENTE);

        //act
        service.enviarRecuperacion(usuario, "TOKEN123", 30L);

        //assert
        ArgumentCaptor<SimpleMailMessage> enviado =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(enviado.capture());
        SimpleMailMessage mensaje = enviado.getValue();

        assertArrayEquals(new String[] {"maria@gmail.com"}, mensaje.getTo());
        assertEquals(REMITENTE, mensaje.getFrom());
        assertNotNull(mensaje.getText());
        assertTrue(mensaje.getText().contains("http://localhost:5173/recuperar/TOKEN123"),
                "el enlace del correo tiene que llevar el token");
        assertTrue(mensaje.getText().contains("30 minutos"));
        assertTrue(mensaje.getText().contains("María García"));
    }

    // ---------- Avisos de moderación (I1-ter) ----------

    private Negocio negocioDe(Usuario duena) {
        Negocio negocio = new Negocio(duena, "Panadería La Tradicional", "descripción",
                "3001234567", new CategoriaNegocio("Gastronomía", "🍴"), new Ciudad("Medellín"),
                null, NivelPrecio.MEDIO);
        negocio.setId(7L);
        return negocio;
    }

    private SimpleMailMessage mensajeEnviado() {
        ArgumentCaptor<SimpleMailMessage> enviado =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(enviado.capture());
        return enviado.getValue();
    }

    @Test
    @DisplayName("avisarNegocioAprobado: va al dueño con el enlace a su perfil público")
    void avisarNegocioAprobado_componeElMensaje() {
        //arrange
        Usuario duena = new Usuario("María García", "maria@gmail.com", "hash", Rol.EMPRENDEDOR);

        //act
        service.avisarNegocioAprobado(negocioDe(duena));

        //assert
        SimpleMailMessage mensaje = mensajeEnviado();
        assertArrayEquals(new String[] {"maria@gmail.com"}, mensaje.getTo());
        assertNotNull(mensaje.getText());
        assertTrue(mensaje.getText().contains("Panadería La Tradicional"));
        assertTrue(mensaje.getText().contains("http://localhost:5173/negocios/7"));
    }

    @Test
    @DisplayName("avisarNegocioRechazado: lleva el motivo y el camino al panel para corregir")
    void avisarNegocioRechazado_llevaElMotivo() {
        //arrange
        Usuario duena = new Usuario("María García", "maria@gmail.com", "hash", Rol.EMPRENDEDOR);

        //act
        service.avisarNegocioRechazado(negocioDe(duena), "Faltan fotos del local");

        //assert
        SimpleMailMessage mensaje = mensajeEnviado();
        assertNotNull(mensaje.getText());
        assertTrue(mensaje.getText().contains("Faltan fotos del local"));
        assertTrue(mensaje.getText().contains("http://localhost:5173/mi-negocio"));
    }

    @Test
    @DisplayName("Un aviso que no sale no tumba la decisión que lo provocó")
    void avisar_servidorCaido_noLanza() {
        //arrange
        Usuario duena = new Usuario("María García", "maria@gmail.com", "hash", Rol.EMPRENDEDOR);
        doThrow(new MailSendException("SMTP caído")).when(mailSender)
                .send(any(SimpleMailMessage.class));

        //act
        //assert
        assertDoesNotThrow(() -> service.avisarNegocioAprobado(negocioDe(duena)));
    }
}
