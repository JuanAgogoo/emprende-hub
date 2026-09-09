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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto o servicio del escaparate de un negocio (F1).
 *
 * <p><strong>No se vende</strong>: sin carrito, sin pago y sin envíos. La ficha
 * enseña qué ofrece el negocio y el trato se cierra fuera de la plataforma, por
 * teléfono o por el formulario de consultas.
 *
 * <p>Tampoco hay inventario (F3): un interruptor de disponibilidad y nada más.
 * Contar existencias habría exigido descontarlas en alguna venta que no existe.
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 120)
    private String nombre;

    /** En pesos colombianos, la moneda del proyecto. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Opcional (F2): el formulario del prototipo ni siquiera la pedía. */
    @Column(length = 500)
    private String descripcion;

    /**
     * El nombre del fichero de su imagen, obligatorio.
     *
     * <p>Un escaparate con huecos no es un escaparate: cada artículo entra con
     * su foto o no entra. Se guarda el nombre y no la URL, igual que en
     * {@link Foto}, para que mover el sitio donde se sirven no obligue a
     * reescribir la tabla.
     */
    @Column(nullable = false, length = 255)
    private String foto;

    @Column(nullable = false)
    private boolean disponible;

    public Producto(Negocio negocio, String nombre, BigDecimal precio, String descripcion,
                    boolean disponible, String foto) {
        this.negocio = negocio;
        this.nombre = nombre;
        this.precio = precio;
        this.descripcion = descripcion;
        this.disponible = disponible;
        this.foto = foto;
    }
}
