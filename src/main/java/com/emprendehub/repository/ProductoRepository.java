package com.emprendehub.repository;

import com.emprendehub.model.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByNegocioIdOrderByNombreAsc(Long negocioId);

    /**
     * Un producto concreto, pero solo si es de ese negocio.
     *
     * <p>Lleva el negocio en la consulta a propósito: buscar por identificador y
     * comprobar el dueño después deja la puerta abierta a que alguien se olvide
     * de comprobarlo. Aquí el que no es tuyo sencillamente no aparece.
     */
    Optional<Producto> findByIdAndNegocioId(Long id, Long negocioId);
}
