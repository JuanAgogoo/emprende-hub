package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entrada del log de moderación (sección L).
 *
 * <p>Tabla de solo escritura: nunca se edita ni se borra. Es el argumento de
 * trazabilidad de las acciones administrativas.
 *
 * <p>Guarda el nombre del afectado como texto, no como relación: si el negocio
 * o la cuenta desaparecen, el registro tiene que seguir contando qué pasó.
 */
@Entity
@Table(name = "registro_moderacion")
@Getter
@NoArgsConstructor
public class RegistroModeracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoEventoModeracion tipo;

    /** Sobre qué se actuó, en texto legible. */
    @Column(nullable = false, length = 200)
    private String afectado;

    /** Motivo o detalle, cuando la acción lo lleva. */
    @Column(length = 1000)
    private String detalle;

    @Column(nullable = false, length = 180)
    private String administrador;

    @Column(nullable = false)
    private Instant fecha;

    public RegistroModeracion(TipoEventoModeracion tipo, String afectado, String detalle,
                              String administrador) {
        this.tipo = tipo;
        this.afectado = afectado;
        this.detalle = detalle;
        this.administrador = administrador;
        this.fecha = Instant.now();
    }
}
