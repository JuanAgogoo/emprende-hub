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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lo que una persona opina de un negocio.
 *
 * <p>La restricción de unicidad sobre (negocio, autor) es C2 escrita en el
 * esquema: <strong>una opinión por persona y negocio</strong>. El servicio la
 * comprueba antes para dar un 400 con su motivo, pero tenerla también aquí
 * significa que dos peticiones simultáneas no pueden colarse las dos.
 *
 * <p>Se publica al instante (C4). Solo pasa por el administrador si alguien la
 * denuncia, y entonces él la borra o desestima la denuncia.
 *
 * <p>El comentario es opcional: calificar con estrellas y no escribir nada es
 * una opinión perfectamente válida, y el prototipo lo permite.
 */
@Entity
@Table(name = "opinion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_opinion_negocio_autor",
                columnNames = {"negocio_id", "autor_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Opinion {

    /** Lo que admite el selector de estrellas del prototipo. */
    public static final int CALIFICACION_MINIMA = 1;
    public static final int CALIFICACION_MAXIMA = 5;

    /** Límite heredado del prototipo, que corta el textarea en 300. */
    public static final int MAXIMO_COMENTARIO = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /** Quién opina. No hay opiniones anónimas (C1). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false)
    private int calificacion;

    @Column(length = MAXIMO_COMENTARIO)
    private String comentario;

    @Column(nullable = false)
    private Instant fechaCreacion;

    /** Se rellena solo si su autor la editó después (C2). */
    private Instant fechaEdicion;

    public Opinion(Negocio negocio, Usuario autor, int calificacion, String comentario) {
        this.negocio = negocio;
        this.autor = autor;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fechaCreacion = Instant.now();
    }

    /** Cierto si su autor la cambió después de publicarla. */
    public boolean fueEditada() {
        return fechaEdicion != null;
    }
}
