package com.emprendehub.model;

/**
 * Estado de una foto dentro del ciclo de revisión.
 *
 * <p>Existe porque B2 manda las fotos a revisión igual que el nombre y la
 * descripción, pero B2-bis prohíbe que el negocio desaparezca del directorio
 * mientras espera. La solución es la misma que para el texto, aplicada a cada
 * foto: la nueva nace pendiente y el público sigue viendo las que ya estaban.
 */
public enum EstadoFoto {

    /** Subida pero todavía sin revisar: solo la ve su dueño. */
    PENDIENTE,

    /** Aprobada por el administrador y visible en el perfil público. */
    APROBADA
}
