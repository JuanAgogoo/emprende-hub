package com.emprendehub.config;

import com.emprendehub.model.CategoriaCurso;
import com.emprendehub.model.Curso;
import com.emprendehub.model.EstadoCurso;
import com.emprendehub.model.NivelCurso;
import com.emprendehub.repository.CursoRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los ocho cursos del prototipo, publicados.
 *
 * <p>Van aparte de los datos de negocios porque no dependen de nada: un curso es
 * una ficha de catálogo con enlace externo y no se relaciona con ninguna otra
 * entidad (sección E).
 *
 * <p>Idempotente: si ya hay cursos no hace nada.
 */
@Configuration
public class CargaInicialCursos {

    private static final Logger log = LoggerFactory.getLogger(CargaInicialCursos.class);

    /** Ficha del prototipo, ya traducida a los conjuntos cerrados del dominio. */
    private record Ficha(String titulo, String descripcion, String duracion,
                         CategoriaCurso categoria, NivelCurso nivel, String precio,
                         String emoji) {
    }

    private static final List<Ficha> CURSOS = List.of(
            new Ficha("Marketing Digital Básico",
                    "Aprende los fundamentos del marketing en redes sociales para tu negocio.",
                    "4 horas", CategoriaCurso.MARKETING, NivelCurso.BASICO, null, "📱"),
            new Ficha("Finanzas para Emprendedores",
                    "Controla tus ingresos, egresos y flujo de caja desde cero.",
                    "6 horas", CategoriaCurso.FINANZAS, NivelCurso.BASICO, null, "💰"),
            new Ficha("SEO para Principiantes",
                    "Posiciona tu negocio en Google sin pagar publicidad.",
                    "3 horas", CategoriaCurso.DIGITAL, NivelCurso.BASICO, null, "🔍"),
            new Ficha("Estrategias de Ventas",
                    "Técnicas avanzadas de cierre de ventas y fidelización de clientes.",
                    "8 horas", CategoriaCurso.VENTAS, NivelCurso.INTERMEDIO, "49000", "🎯"),
            new Ficha("E-commerce con Instagram",
                    "Vende directamente desde Instagram Shop y Stories.",
                    "5 horas", CategoriaCurso.MARKETING, NivelCurso.INTERMEDIO, "35000", "🛍️"),
            new Ficha("Contabilidad Básica",
                    "Lleva los libros de tu negocio sin ser contador.",
                    "4 horas", CategoriaCurso.FINANZAS, NivelCurso.BASICO, null, "📊"),
            new Ficha("Gestión de Equipos",
                    "Lidera tu equipo, delega y escala con confianza.",
                    "6 horas", CategoriaCurso.GESTION, NivelCurso.INTERMEDIO, "55000", "👥"),
            new Ficha("Automatización con IA",
                    "Usa herramientas de IA para automatizar tareas de tu negocio.",
                    "10 horas", CategoriaCurso.DIGITAL, NivelCurso.AVANZADO, "79000", "🤖"));

    /**
     * @implNote Los cursos no dependen de nada, pero van antes que la demostración para
     * que el registro del arranque se lea en el orden en que se sembró.
     */
    @Order(3)
    @Bean
    @Transactional
    public CommandLineRunner sembrarCursos(CursoRepository cursoRepository) {
        return argumentos -> {
            if (cursoRepository.count() > 0) {
                log.info("Los cursos ya estaban cargados, no se toca nada.");
                return;
            }

            CURSOS.forEach(ficha -> {
                Curso curso = new Curso(ficha.titulo(), ficha.descripcion(), ficha.duracion(),
                        ficha.categoria(), ficha.nivel(), ficha.precio() == null,
                        ficha.precio() == null ? null : new BigDecimal(ficha.precio()),
                        "https://emprendehub.co/cursos/" + enlace(ficha.titulo()), ficha.emoji());
                // El catálogo público solo enseña los publicados (E4), y estos
                // están para verse.
                curso.setEstado(EstadoCurso.PUBLICADO);
                cursoRepository.save(curso);
            });

            log.info("Sembrados {} cursos publicados.", CURSOS.size());
        };
    }

    private String enlace(String titulo) {
        return java.text.Normalizer.normalize(titulo, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-");
    }
}
