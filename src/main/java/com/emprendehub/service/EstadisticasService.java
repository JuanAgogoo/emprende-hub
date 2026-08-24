package com.emprendehub.service;

import com.emprendehub.dto.EstadisticasPortadaResponse;
import com.emprendehub.model.Rol;
import com.emprendehub.repository.CategoriaNegocioRepository;
import com.emprendehub.repository.NegocioRepository;
import com.emprendehub.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las cifras de la barra de la portada, calculadas de verdad (H4).
 *
 * <p>El prototipo llevaba números fijos —«+500 negocios», «4.7★»—. Tras decidir
 * que las visitas se cuentan de verdad (H1), dejar cifras inventadas en la
 * portada sería incoherente.
 *
 * <p>Las cuatro se cuentan con el mismo criterio que usa el directorio para
 * enseñar: si un negocio no se ve, tampoco suma.
 */
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

    /** Un decimal, que es como la portada enseña la nota: «4.7★». */
    private static final int DECIMALES_DE_LA_PORTADA = 1;

    private final NegocioRepository negocioRepository;
    private final CategoriaNegocioRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public EstadisticasService(NegocioRepository negocioRepository,
                               CategoriaNegocioRepository categoriaRepository,
                               UsuarioRepository usuarioRepository) {
        this.negocioRepository = negocioRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public EstadisticasPortadaResponse obtenerPortada() {
        return new EstadisticasPortadaResponse(
                negocioRepository.contarVisiblesEnDirectorio(),
                categoriaRepository.count(),
                usuarioRepository.countByActivoTrueAndRolNot(Rol.ADMIN),
                redondear(negocioRepository.promedioDeCalificaciones()));
    }

    /**
     * Redondea la media a un decimal.
     *
     * <p>Una plataforma recién sembrada, sin ninguna opinión, no tiene media: se
     * devuelve nula y no cero, por el mismo motivo que un negocio sin opiniones
     * no se califica con un cero (C5).
     */
    private BigDecimal redondear(Double promedio) {
        if (promedio == null) {
            return null;
        }
        return BigDecimal.valueOf(promedio)
                .setScale(DECIMALES_DE_LA_PORTADA, RoundingMode.HALF_UP);
    }
}
