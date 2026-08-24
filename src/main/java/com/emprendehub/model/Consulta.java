package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Mensaje de un cliente al dueño de un negocio (D1).
 *
 * <p>Vive en un <strong>buzón dentro de la plataforma</strong>, no en un correo:
 * la aplicación no envía ninguno (I1). El dueño lo lee en su panel y responde
 * por fuera, con la dirección que el buzón le enseña (D2).
 *
 * <p>No hay campo de respuesta ni hilo de conversación. Es un buzón de entrada,
 * y esa limitación es deliberada: montar mensajería de ida y vuelta sin correos
 * habría exigido que las dos partes entraran a mirar.
 *
 * <p><strong>Esta pantalla no existe en el prototipo</strong>, que se limitaba a
 * enseñar un aviso de «mensaje enviado» y no guardaba nada.
 */
@Entity
@Table(name = "consulta")
@Getter
@Setter
@NoArgsConstructor
public class Consulta {

    /** Lo que cuenta el textarea del prototipo. */
    public static final int MAXIMO_MENSAJE = 500;

    /**
     * El asunto es una línea de la lista del buzón, no un segundo mensaje.
     *
     * <p>Las decisiones de dominio agrupan asunto y mensaje bajo el mismo límite
     * de 500, pero el prototipo solo cuenta el del mensaje y un asunto de 500
     * caracteres rompería la tabla donde se lee.
     */
    public static final int MAXIMO_ASUNTO = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /** Quién pregunta. Contactar exige sesión, así que siempre hay alguien. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(nullable = false, length = MAXIMO_ASUNTO)
    private String asunto;

    @Column(nullable = false, length = MAXIMO_MENSAJE)
    private String mensaje;

    /** Leída o no leída (D3). Nace sin leer, que es de lo que va el buzón. */
    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private Instant fechaEnvio;

    /** Cuándo la abrió el dueño. Vuelve a nulo si la marca como no leída. */
    private Instant fechaLectura;

    public Consulta(Negocio negocio, Usuario cliente, String asunto, String mensaje) {
        this.negocio = negocio;
        this.cliente = cliente;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.leida = false;
        this.fechaEnvio = Instant.now();
    }

    /**
     * Marca la consulta como leída o la devuelve al montón (D3).
     *
     * <p>La fecha de lectura acompaña al estado: dejarla puesta en una consulta
     * marcada como no leída diría que se abrió y se ignoró, que no es lo que el
     * dueño quiere decir al desmarcarla.
     */
    public void marcarLectura(boolean leida) {
        this.leida = leida;
        this.fechaLectura = leida ? Instant.now() : null;
    }
}
