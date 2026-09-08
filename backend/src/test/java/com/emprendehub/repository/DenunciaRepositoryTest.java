package com.emprendehub.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.emprendehub.model.CategoriaNegocio;
import com.emprendehub.model.Ciudad;
import com.emprendehub.model.Denuncia;
import com.emprendehub.model.EstadoDenuncia;
import com.emprendehub.model.EstadoNegocio;
import com.emprendehub.model.MotivoDenuncia;
import com.emprendehub.model.Negocio;
import com.emprendehub.model.NivelPrecio;
import com.emprendehub.model.Opinion;
import com.emprendehub.model.Rol;
import com.emprendehub.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;

class DenunciaRepositoryTest extends PostgresTestBase {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DenunciaRepository repository;

    private Long idOpinion;
    private Long idDenunciante;
    private Long idDenunciaPendiente;

    @BeforeEach
    void prepararDenuncias() {
        Ciudad medellin = entityManager.persistAndFlush(new Ciudad("Medellín"));
        CategoriaNegocio categoria =
                entityManager.persistAndFlush(new CategoriaNegocio("Gastronomía", "🍴"));

        Usuario dueno = usuario("María", "maria@test.co");
        Usuario autor = usuario("Carlos", "carlos@test.co");
        Usuario tercero = usuario("Sofía", "sofia@test.co");

        Negocio negocio = new Negocio(dueno, "Panadería La Tradicional",
                "Descripción larga de prueba con más de ochenta caracteres para pasar la "
                        + "validación de longitud del registro.",
                "3001234567", categoria, medellin, null, NivelPrecio.MEDIO);
        negocio.setEstado(EstadoNegocio.APROBADO);
        entityManager.persistAndFlush(negocio);

        Opinion opinion = entityManager.persistAndFlush(
                new Opinion(negocio, autor, 1, "Texto denunciable"));

        Denuncia pendiente = entityManager.persistAndFlush(
                new Denuncia(opinion, dueno, MotivoDenuncia.LENGUAJE_INAPROPIADO));
        Denuncia resuelta = new Denuncia(opinion, tercero, MotivoDenuncia.SPAM);
        resuelta.setEstado(EstadoDenuncia.DESESTIMADA);
        entityManager.persistAndFlush(resuelta);

        idOpinion = opinion.getId();
        idDenunciante = dueno.getId();
        idDenunciaPendiente = pendiente.getId();
        entityManager.clear();
    }

    private Usuario usuario(String nombre, String correo) {
        return entityManager.persistAndFlush(new Usuario(nombre, correo, "hash", Rol.CLIENTE));
    }

    @Test
    @DisplayName("La cola del administrador solo trae las pendientes")
    void findByEstado_soloLasPendientes() {
        var cola = repository.findByEstadoOrderByFechaAsc(
                EstadoDenuncia.PENDIENTE, PageRequest.of(0, 10));

        assertEquals(1, cola.getTotalElements());
        assertEquals(MotivoDenuncia.LENGUAJE_INAPROPIADO,
                cola.getContent().getFirst().getMotivo());
    }

    @Test
    @DisplayName("La cola trae resueltos la opinión, su autor y su negocio")
    void findByEstado_traeLasRelacionesResueltas() {
        // Sin el @EntityGraph anidado serían tres consultas más por fila, y con
        // open-in-view desactivado el mapeo fallaría fuera de la transacción.
        Denuncia denuncia = repository.findByEstadoOrderByFechaAsc(
                EstadoDenuncia.PENDIENTE, PageRequest.of(0, 10)).getContent().getFirst();

        assertNotNull(denuncia.getOpinion().getComentario());
        assertNotNull(denuncia.getOpinion().getAutor().getNombre());
        assertNotNull(denuncia.getOpinion().getNegocio().getNombre());
        assertNotNull(denuncia.getDenunciante().getNombre());
    }

    @Test
    @DisplayName("findWithDetalleById trae la denuncia lista para resolverla")
    void findWithDetalle_traeLoNecesario() {
        var denuncia = repository.findWithDetalleById(idDenunciaPendiente);

        assertTrue(denuncia.isPresent());
        assertEquals("Panadería La Tradicional",
                denuncia.get().getOpinion().getNegocio().getNombre());
    }

    @Test
    @DisplayName("El esquema impide denunciar dos veces la misma opinión")
    void unicidad_segundaDenuncia_revienta() {
        Opinion opinion = entityManager.find(Opinion.class, idOpinion);
        Usuario dueno = entityManager.find(Usuario.class, idDenunciante);

        assertThrows(Exception.class, () -> entityManager.persistAndFlush(
                new Denuncia(opinion, dueno, MotivoDenuncia.SPAM)));
    }

    @Test
    @DisplayName("Las denuncias de una opinión se encuentran para irse con ella")
    void findByOpinionId_lasDevuelveTodas() {
        // Las busca el servicio antes de borrar la opinión: si sobrevivieran,
        // la cola se llenaría de avisos sobre textos que ya no existen.
        assertEquals(2, repository.findByOpinionId(idOpinion).size());
    }

    @Test
    @DisplayName("Denunciante y opinión juntos dicen si esa persona ya denunció")
    void existsByOpinionYDenunciante_distingue() {
        assertTrue(repository.existsByOpinionIdAndDenuncianteId(idOpinion, idDenunciante));
        assertFalse(repository.existsByOpinionIdAndDenuncianteId(idOpinion, 999999L));
    }
}
