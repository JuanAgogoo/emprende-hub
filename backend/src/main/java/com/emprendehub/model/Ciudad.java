package com.emprendehub.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Municipio del área metropolitana del Valle de Aburrá (G2).
 *
 * <p>Es el nivel superior de la jerarquía de la decisión G3. Un negocio de El
 * Poblado aparece al filtrar por Medellín porque El Poblado cuelga de ella, en
 * vez de convivir a su mismo nivel como hacía el prototipo.
 */
@Entity
@Table(name = "ciudad")
@Getter
@Setter
@NoArgsConstructor
public class Ciudad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @OneToMany(mappedBy = "ciudad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Barrio> barrios = new ArrayList<>();

    public Ciudad(String nombre) {
        this.nombre = nombre;
    }

    /** Da de alta un barrio manteniendo los dos lados de la relación. */
    public void agregarBarrio(Barrio barrio) {
        barrios.add(barrio);
        barrio.setCiudad(this);
    }
}
