package com.emprendehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Categoría bajo la que se clasifica un emprendimiento.
 *
 * <p>Son las 12 de la decisión G1 y no las edita nadie desde la aplicación (G5):
 * las carga {@code CargaInicialCatalogos} al arrancar.
 */
@Entity
@Table(name = "categoria_negocio")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    /** Emoji con el que la pinta el directorio. */
    @Column(nullable = false, length = 8)
    private String icono;

    public CategoriaNegocio(String nombre, String icono) {
        this.nombre = nombre;
        this.icono = icono;
    }
}
