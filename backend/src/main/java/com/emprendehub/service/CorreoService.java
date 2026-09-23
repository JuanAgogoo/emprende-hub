package com.emprendehub.service;

import com.emprendehub.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Los correos que manda la aplicación. Hoy solo hay uno: el de recuperación.
 *
 * <p>Va contra un servidor SMTP local —Mailpit, un servicio más de
 * {@code docker-compose.yml}—, así que funciona sin internet y sin cuenta de
 * correo de nadie, y el mensaje se ve llegar en su interfaz web. Para salir al
 * mundo real bastaría con cambiar las tres propiedades de {@code spring.mail}.
 *
 * <p>El mensaje es texto plano: un correo con maquetación no lo pidió nadie y
 * el enlace se lee igual de bien.
 */
@Service
public class CorreoService {

    private final JavaMailSender mailSender;
    private final String remitente;
    private final String urlBase;

    public CorreoService(JavaMailSender mailSender,
                         @Value("${emprendehub.correo.remitente}") String remitente,
                         @Value("${emprendehub.correo.url-base}") String urlBase) {
        this.mailSender = mailSender;
        this.remitente = remitente;
        this.urlBase = urlBase;
    }

    /**
     * Manda el enlace de recuperación a la dirección de la cuenta.
     *
     * <p>El token **solo viaja por aquí**: devolverlo en la respuesta de la
     * solicitud dejaría cambiar la contraseña de cualquiera sabiendo únicamente
     * su correo.
     */
    public void enviarRecuperacion(Usuario usuario, String token, long minutosDeVida) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(usuario.getCorreo());
        mensaje.setSubject("Recupera tu contraseña de EmprendeHub");
        mensaje.setText("""
                Hola, %s:

                Alguien pidió recuperar la contraseña de esta cuenta. Si fuiste tú,
                abre este enlace y elige una nueva:

                %s/recuperar/%s

                El enlace sirve una sola vez y caduca en %d minutos.

                Si no lo pediste, no hace falta que hagas nada: tu contraseña
                sigue siendo la de siempre.
                """.formatted(usuario.getNombre(), urlBase, token, minutosDeVida));

        mailSender.send(mensaje);
    }
}
