package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una visita al perfil público de un negocio (H1).
 *
 * <p>Se guarda una fila por <strong>perfil, sesión y día</strong>: recargar la
 * página no suma. La fecha se calcula en la zona del proyecto, no en UTC, porque
 * el corte de «un día» depende de dónde cae la medianoche y el negocio está en
 * Medellín.
 *
 * <p>No hay restricción de unicidad en el esquema, y es deliberado. La
 * deduplicación es una <em>heurística de conteo</em>, no un invariante como el de
 * una opinión por persona: dos peticiones simultáneas del mismo visitante
 * contarían dos veces, y eso es preferible a que el perfil público responda 500
 * por una violación de clave.
 */
@Entity
@Table(name = "visita",
        indexes = @Index(name = "idx_visita_negocio_fecha", columnList = "negocio_id, fecha"))
@Getter
@Setter
@NoArgsConstructor
public class Visita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /**
     * Quién visita, sin guardar quién es.
     *
     * <p>Con sesión iniciada es la cuenta; sin ella, una huella de la petición.
     * Lo único que hace falta es poder decir «esto ya lo conté hoy», no
     * identificar a nadie.
     */
    @Column(nullable = false, length = 64)
    private String huellaSesion;

    /** El día en la zona del proyecto, que es lo que agrupa el conteo. */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Instant instante;

    public Visita(Negocio negocio, String huellaSesion, LocalDate fecha, Instant instante) {
        this.negocio = negocio;
        this.huellaSesion = huellaSesion;
        this.fecha = fecha;
        this.instante = instante;
    }
}
