package com.emprendehub.dto;

import java.time.Instant;

/**
 * Una consulta como la ve el dueño en su buzón.
 *
 * <p><strong>Es la única respuesta de la API que enseña el correo de otra
 * persona</strong>, y es a propósito (D2): la plataforma no envía correos ni
 * permite responder desde dentro, así que sin esa dirección el buzón sería un
 * montón de preguntas sin forma de contestarlas. (El del inicio de sesión no
 * cuenta: ahí cada cual recibe el suyo.)
 *
 * <p>La excepción está acotada a este sitio. El correo sigue sin aparecer en el
 * perfil público, en el directorio ni en las opiniones, y solo lo recibe el
 * dueño del negocio al que va dirigida la consulta.
 *
 * @param fechaLectura nula mientras siga sin leerse (D3)
 */
public record ConsultaResponse(
        Long id,
        String asunto,
        String mensaje,
        String nombreCliente,
        String correoCliente,
        boolean leida,
        Instant fechaEnvio,
        Instant fechaLectura) {
}
