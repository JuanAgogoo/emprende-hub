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
 * Imagen de la galería de un negocio (B9).
 *
 * <p>En la base de datos vive solo el nombre del fichero; los bytes están en el
 * directorio que gestiona {@code AlmacenamientoFotos}. Guardar la imagen en una
 * columna habría hecho pesada cada consulta del directorio.
 *
 * <p><strong>No hay campo que marque la principal</strong>: es la primera por
 * orden, tal como decide B9. Un campo aparte permitiría estados imposibles —dos
 * principales, o ninguna— que habría que impedir por código.
 */
@Entity
@Table(name = "foto")
@Getter
@Setter
@NoArgsConstructor
public class Foto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    /** Nombre del fichero en el directorio de fotos, no la ruta completa. */
    @Column(nullable = false, length = 200)
    private String nombreArchivo;

    /** Posición en la galería. La de orden más bajo es la principal (B9). */
    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFoto estado;

    @Column(nullable = false)
    private Instant fechaSubida;

    public Foto(Negocio negocio, String nombreArchivo, int orden) {
        this.negocio = negocio;
        this.nombreArchivo = nombreArchivo;
        this.orden = orden;
        this.estado = EstadoFoto.PENDIENTE;
        this.fechaSubida = Instant.now();
    }

    public boolean estaAprobada() {
        return estado == EstadoFoto.APROBADA;
    }
}
