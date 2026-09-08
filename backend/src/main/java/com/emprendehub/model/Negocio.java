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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Emprendimiento publicado en el directorio.
 *
 * <p>Cada cuenta tiene como mucho uno (A2), de ahí la relación uno a uno con
 * {@link Usuario} y la restricción de unicidad sobre su columna.
 *
 * <p>La calificación media y el número de opiniones se guardan aquí en vez de
 * calcularse en cada consulta: el directorio ordena y filtra por ellas. Las
 * recalcula el servicio de opiniones cada vez que una cambia, incluida su
 * moderación.
 */
@Entity
@Table(name = "negocio")
@Getter
@Setter
@NoArgsConstructor
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dueño del negocio. Uno por cuenta (A2). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    /** Fijo o móvil, en el formato que admite G8. */
    @Column(nullable = false, length = 20)
    private String telefono;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaNegocio categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    /** Opcional: solo Medellín tiene barrios en el catálogo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barrio_id")
    private Barrio barrio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelPrecio nivelPrecio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNegocio estado;

    /** Motivo del rechazo, que el dueño lee en su panel al no haber correos (I1). */
    @Column(length = 1000)
    private String motivoRechazo;

    /**
     * Perfil de Instagram, opcional (B8).
     *
     * <p>El prototipo enseñaba los iconos de redes en el perfil pero no pedía
     * las direcciones en ningún formulario, así que nunca podían llenarse.
     */
    @Column(length = 200)
    private String instagram;

    /** Perfil de LinkedIn, opcional (B8). */
    @Column(length = 200)
    private String linkedin;

    /** Sin ninguna opinión se queda a null, no a cero (C5). */
    @Column(precision = 3, scale = 2)
    private java.math.BigDecimal calificacionPromedio;

    @Column(nullable = false)
    private int numeroOpiniones;

    @Column(nullable = false)
    private Instant fechaCreacion;

    /** Cuándo apareció de verdad en el directorio. Ordena «Más recientes» (G7). */
    private Instant fechaAprobacion;

    public Negocio(Usuario usuario, String nombre, String descripcion, String telefono,
                   CategoriaNegocio categoria, Ciudad ciudad, Barrio barrio,
                   NivelPrecio nivelPrecio) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.telefono = telefono;
        this.categoria = categoria;
        this.ciudad = ciudad;
        this.barrio = barrio;
        this.nivelPrecio = nivelPrecio;
        this.estado = EstadoNegocio.PENDIENTE;
        this.numeroOpiniones = 0;
        this.fechaCreacion = Instant.now();
    }

    public boolean estaAprobado() {
        return estado == EstadoNegocio.APROBADO;
    }

    /** Cierto si lo puede ver quien no es ni su dueño ni el administrador (B6). */
    public boolean esVisiblePublicamente() {
        return estaAprobado() && usuario.isActivo();
    }
}
