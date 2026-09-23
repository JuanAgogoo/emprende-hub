package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.Rol;
import com.emprendehub.model.TokenRecuperacion;
import com.emprendehub.model.Usuario;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Los enlaces de recuperación contra PostgreSQL de verdad.
 *
 * <p>Lo que obliga a esta clase es lo que una prueba de servicio con el
 * repositorio simulado no puede ver: que el token es único en el esquema y que
 * la búsqueda trae resuelto el usuario, que es de quien hay que cambiar la
 * contraseña con {@code open-in-view: false}.
 */
class TokenRecuperacionRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TokenRecuperacionRepository repository;

    private Usuario maria;

    @BeforeEach
    void prepararCuenta() {
        maria = entityManager.persistAndFlush(
                new Usuario("María García", "maria@test.co", "hash", Rol.CLIENTE));
    }

    private TokenRecuperacion enlace(String token, Instant caducidad) {
        return new TokenRecuperacion(maria, token, caducidad);
    }

    @Test
    @DisplayName("findByToken: encuentra el enlace y trae resuelto a su dueño")
    void findByToken_existente_traeElUsuario() {
        //arrange
        entityManager.persistAndFlush(enlace("token-vivo", Instant.now().plus(Duration.ofMinutes(30))));
        entityManager.clear();

        //act
        Optional<TokenRecuperacion> encontrado = repository.findByToken("token-vivo");

        //assert
        assertTrue(encontrado.isPresent());
        assertEquals("maria@test.co", encontrado.get().getUsuario().getCorreo());
        assertTrue(encontrado.get().sirve(Instant.now()));
    }

    @Test
    @DisplayName("findByToken: un token que no existe no devuelve nada")
    void findByToken_inventado_vacio() {
        //arrange
        entityManager.persistAndFlush(enlace("token-vivo", Instant.now().plus(Duration.ofMinutes(30))));

        //act
        Optional<TokenRecuperacion> encontrado = repository.findByToken("me-lo-invento");

        //assert
        assertTrue(encontrado.isEmpty());
    }

    @Test
    @DisplayName("el token es único: la base no deja guardar dos iguales")
    void token_repetido_loRechazaElEsquema() {
        //arrange
        entityManager.persistAndFlush(enlace("token-repetido", Instant.now().plus(Duration.ofMinutes(30))));

        //act
        //assert
        assertThrows(PersistenceException.class, () -> entityManager.persistAndFlush(
                enlace("token-repetido", Instant.now().plus(Duration.ofMinutes(30)))));
    }

    @Test
    @DisplayName("un enlace caducado o usado se guarda igual: lo que cambia es que ya no sirve")
    void enlace_caducadoOUsado_seGuardaPeroNoSirve() {
        //arrange
        TokenRecuperacion caducado =
                enlace("token-caducado", Instant.now().minus(Duration.ofMinutes(1)));
        TokenRecuperacion usado =
                enlace("token-usado", Instant.now().plus(Duration.ofMinutes(30)));
        usado.setUsado(true);

        //act
        entityManager.persistAndFlush(caducado);
        entityManager.persistAndFlush(usado);
        entityManager.clear();

        //assert
        assertTrue(repository.findByToken("token-caducado").isPresent());
        assertTrue(repository.findByToken("token-usado").isPresent());
        assertEquals(false, repository.findByToken("token-caducado").get().sirve(Instant.now()));
        assertEquals(false, repository.findByToken("token-usado").get().sirve(Instant.now()));
    }
}
