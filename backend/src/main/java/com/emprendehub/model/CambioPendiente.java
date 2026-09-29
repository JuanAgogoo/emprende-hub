package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Modificación de campos públicos esperando revisión (B2-bis).
 *
 * <p>Existe para resolver una tensión: los cambios visibles tienen que pasar por
 * el administrador (B2), pero el negocio <strong>no puede desaparecer del
 * directorio mientras tanto</strong>, porque castigaría a alguien por corregir
 * una errata.
 *
 * <p>La solución es guardar los valores propuestos aparte. El negocio conserva
 * publicados los suyos, el público no se entera de nada, y al aprobar se copian
 * y esta fila se borra.
 *
 * <p>Solo hay una por negocio: una propuesta nueva sustituye a la anterior.
 */
@Entity
@Table(name = "cambio_pendiente")
@Getter
@Setter
@NoArgsConstructor
public class CambioPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false, unique = true)
    private Negocio negocio;

    @Column(nullable = false, length = 120)
    private String nombrePropuesto;

    @Column(nullable = false, length = 2000)
    private String descripcionPropuesta;

    /**
     * La categoría propuesta, o {@code null} si la propuesta no la cambia.
     *
     * <p>Es el único campo que admite nulo, y el nulo significa algo: una
     * propuesta abierta solo por subir fotos no propone categoría ninguna. El
     * nombre y la descripción se copian de los actuales en ese caso porque ya
     * eran obligatorios cuando se entregó la tabla; esta columna nació después.
     */
    @ManyToOne
    @JoinColumn(name = "categoria_propuesta_id")
    private CategoriaNegocio categoriaPropuesta;

    @Column(nullable = false)
    private Instant fechaSolicitud;

    public CambioPendiente(Negocio negocio, String nombrePropuesto, String descripcionPropuesta) {
        this.negocio = negocio;
        this.nombrePropuesto = nombrePropuesto;
        this.descripcionPropuesta = descripcionPropuesta;
        this.fechaSolicitud = Instant.now();
    }
}
