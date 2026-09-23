package com.emprendehub.repository;

import com.emprendehub.model.TokenRecuperacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    /**
     * Busca un enlace por su token.
     *
     * <p>Trae resuelto el usuario porque restablecer la contraseña es cambiarle
     * la suya, y la configuración usa {@code open-in-view: false}.
     */
    @EntityGraph(attributePaths = "usuario")
    Optional<TokenRecuperacion> findByToken(String token);
}
