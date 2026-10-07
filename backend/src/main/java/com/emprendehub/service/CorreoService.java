package com.emprendehub.service;

import com.emprendehub.model.Negocio;
import com.emprendehub.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Los correos que manda la aplicación: el de recuperación (I1-bis) y los avisos
 * de las decisiones del administrador (I1-ter).
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

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

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

    // ---------- Avisos de moderación (I1-ter) ----------

    /** Avisa al dueño de que su negocio ya se ve en el directorio. */
    public void avisarNegocioAprobado(Negocio negocio) {
        enviarAviso(negocio.getUsuario(), "Tu negocio ya está publicado en EmprendeHub", """
                Hola, %s:

                Tu negocio «%s» ya está publicado en el directorio. Así lo ve
                todo el mundo:

                %s/negocios/%d
                """.formatted(negocio.getUsuario().getNombre(), negocio.getNombre(),
                urlBase, negocio.getId()));
    }

    /**
     * Avisa al dueño de que su negocio necesita cambios, con el motivo.
     *
     * <p>El motivo sigue además en su panel, que es donde corrige y reenvía: el
     * correo avisa, el panel es donde se actúa.
     */
    public void avisarNegocioRechazado(Negocio negocio, String motivo) {
        enviarAviso(negocio.getUsuario(), "Tu negocio necesita cambios antes de publicarse", """
                Hola, %s:

                Revisamos tu negocio «%s» y necesita cambios antes de publicarse.
                El motivo:

                %s

                Corrígelo y vuelve a enviarlo desde tu panel; no hay límite de
                intentos:

                %s/mi-negocio
                """.formatted(negocio.getUsuario().getNombre(), negocio.getNombre(), motivo,
                urlBase));
    }

    /**
     * Avisa de que la cuenta está suspendida (B4).
     *
     * <p>De todos los avisos es el que más falta hace por correo: una cuenta
     * suspendida no puede entrar, así que no vería nada en su panel.
     */
    public void avisarCuentaSuspendida(Usuario usuario) {
        enviarAviso(usuario, "Tu cuenta de EmprendeHub está suspendida", """
                Hola, %s:

                La administración de EmprendeHub suspendió tu cuenta. Mientras
                siga así no podrás entrar, y si tienes un negocio no se verá en
                el directorio. Las opiniones que escribiste se mantienen.

                Si crees que es un error, ponte en contacto con la administración
                de la plataforma.
                """.formatted(usuario.getNombre()));
    }

    /** Avisa de que la cuenta vuelve a funcionar. */
    public void avisarCuentaReactivada(Usuario usuario) {
        enviarAviso(usuario, "Tu cuenta de EmprendeHub está activa otra vez", """
                Hola, %s:

                Tu cuenta vuelve a estar activa. Ya puedes entrar con tu correo y
                tu contraseña de siempre:

                %s/entrar
                """.formatted(usuario.getNombre(), urlBase));
    }

    /**
     * Manda un aviso sin que su fallo tumbe la decisión que lo provoca.
     *
     * <p>El aviso informa de algo que ya pasó: si el servidor de correo no
     * responde, el negocio sigue aprobado y el dueño lo ve igual en su panel
     * (H2). Deshacer la decisión del administrador porque no salió un correo
     * sería peor que el correo perdido, así que se anota y se sigue.
     */
    private void enviarAviso(Usuario destinatario, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario.getCorreo());
        mensaje.setSubject(asunto);
        mensaje.setText(texto);

        try {
            mailSender.send(mensaje);
        } catch (MailException ex) {
            log.error("No se pudo enviar el aviso «{}» a {}", asunto, destinatario.getCorreo(), ex);
        }
    }
}
