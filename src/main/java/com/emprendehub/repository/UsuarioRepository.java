package com.emprendehub.repository;

import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    /**
     * Usuarios registrados de la portada (H4).
     *
     * <p>Fuera los suspendidos y fuera el administrador: es una cuenta técnica
     * sembrada (A3), no gente que se haya registrado en la plataforma.
     */
    long countByActivoTrueAndRolNot(Rol rol);
}
