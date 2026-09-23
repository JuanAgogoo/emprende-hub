package com.emprendehub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * El correo de recuperación, sin servidor de por medio.
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
}
