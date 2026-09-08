package com.emprendehub.model;

/**
 * En qué punto está una denuncia.
 *
 * <p>Solo hay dos estados porque solo hay dos finales posibles: o el
 * administrador borra la opinión —y entonces la denuncia se va con ella, que ya
 * no denuncia nada— o la desestima y queda constancia de que se miró.
 */
public enum EstadoDenuncia {

    /** Esperando al administrador. */
    PENDIENTE,

    /** Revisada y sin motivo: la opinión se queda publicada. */
    DESESTIMADA
}
