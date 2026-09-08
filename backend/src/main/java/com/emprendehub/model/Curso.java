package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recurso formativo del catálogo.
 *
 * <p>Es una ficha con un enlace externo, nada más (E1): la plataforma no cobra
 * ni gestiona inscripciones (E2). Solo el administrador los crea (E3).
 *
 * <p>No se relaciona con ninguna otra entidad del sistema.
 */
@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    /** Duración estimada en texto libre, tal como la escribe el administrador. */
    @Column(nullable = false, length = 40)
    private String duracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaCurso categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NivelCurso nivel;

    @Column(nullable = false)
    private boolean gratuito;

    /** Solo tiene valor cuando el curso es de pago. Ver la regla en el servicio. */
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    /** Enlace externo donde se consume el curso. La plataforma solo apunta. */
    @Column(nullable = false, length = 500)
    private String urlRecurso;

    @Column(nullable = false, length = 8)
    private String emoji;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCurso estado;

    @Column(nullable = false)
    private Instant fechaCreacion;

    /** Un curso nace siempre en borrador: publicarlo es un acto deliberado. */
    public Curso(String titulo, String descripcion, String duracion, CategoriaCurso categoria,
                 NivelCurso nivel, boolean gratuito, BigDecimal precio, String urlRecurso,
                 String emoji) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.duracion = duracion;
        this.categoria = categoria;
        this.nivel = nivel;
        this.gratuito = gratuito;
        this.precio = precio;
        this.urlRecurso = urlRecurso;
        this.emoji = emoji;
        this.estado = EstadoCurso.BORRADOR;
        this.fechaCreacion = Instant.now();
    }

    public boolean estaPublicado() {
        return estado == EstadoCurso.PUBLICADO;
    }
}
