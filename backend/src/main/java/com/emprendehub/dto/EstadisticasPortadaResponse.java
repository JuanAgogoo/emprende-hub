package com.emprendehub.dto;

import java.math.BigDecimal;

/**
 * La barra de cifras de la portada, calculada de verdad (H4).
 *
 * <p>El prototipo llevaba números fijos («+500 negocios»). Tras decidir que las
 * visitas se cuentan de verdad (H1), dejar cifras inventadas en la portada sería
 * incoherente.
 *
 * @param negociosActivos los que se ven en el directorio: aprobados y de cuenta
 *     no suspendida
 * @param categorias las 12 de G1
 * @param usuariosRegistrados cuentas activas sin contar la del administrador
 * @param calificacionPromedio media de las calificaciones que existen, nula
 *     mientras ningún negocio tenga opiniones (C5)
 */
public record EstadisticasPortadaResponse(
        long negociosActivos,
        long categorias,
        long usuariosRegistrados,
        BigDecimal calificacionPromedio) {
}
