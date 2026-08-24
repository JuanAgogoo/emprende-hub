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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nivel inferior de la jerarquía de localización (G3): cuelga de una
 * {@link Ciudad}.
 *
 * <p>Solo Medellín tiene barrios en el material de partida. Los demás municipios
 * del área metropolitana se quedan sin ninguno, por eso el barrio es opcional en
 * el negocio.
 */
@Entity
@Table(name = "barrio")
@Getter
@Setter
@NoArgsConstructor
public class Barrio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    public Barrio(String nombre) {
        this.nombre = nombre;
    }
}
