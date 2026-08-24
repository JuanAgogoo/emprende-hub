package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Aviso para el emprendedor, derivado de un hecho (H2).
 *
 * <p>El texto se compone y se guarda al crearla, en vez de rehacerlo al leerla.
 * Así una notificación de «nueva opinión de María» sigue diciendo lo mismo
 * aunque después el administrador borre esa opinión: lo que se avisó ocurrió, y
 * reconstruirlo más tarde daría un aviso mudo o directamente falso.
 *
 * <p>Se marca como leída o no leída (H3), igual que el buzón de consultas.
 */
@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    public static final int MAXIMO_TEXTO = 250;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** A quién va dirigida. Hoy siempre es el dueño de un negocio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = MAXIMO_TEXTO)
    private String texto;

    @Column(nullable = false)
    private boolean leida;

    @Column(nullable = false)
    private Instant fecha;

    public Notificacion(Usuario destinatario, TipoNotificacion tipo, String texto) {
        this.destinatario = destinatario;
        this.tipo = tipo;
        this.texto = texto;
        this.leida = false;
        this.fecha = Instant.now();
    }
}
