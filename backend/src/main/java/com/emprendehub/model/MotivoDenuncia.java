package com.emprendehub.model;

import lombok.Getter;

/**
 * Por qué se denuncia una opinión (C6).
 *
 * <p>Es una <strong>lista cerrada, no texto libre</strong>. Un campo abierto
 * obligaría al administrador a leer e interpretar cada denuncia antes de poder
 * agruparlas, y convierte el formulario en otro sitio donde escribir lo mismo
 * que se está denunciando.
 *
 * <p>Se ofrece al cliente en {@code GET /api/v1/catalogos/motivos-denuncia},
 * como el resto de conjuntos cerrados.
 */
@Getter
public enum MotivoDenuncia {

    /** El único que nombra el prototipo, en su tabla de reseñas reportadas. */
    LENGUAJE_INAPROPIADO("Lenguaje inapropiado"),
    INFORMACION_FALSA("Información falsa"),
    SPAM("Spam o publicidad"),
    NO_ES_SOBRE_EL_NEGOCIO("No habla del negocio"),
    DATOS_PERSONALES("Expone datos personales");

    private final String nombre;

    MotivoDenuncia(String nombre) {
        this.nombre = nombre;
    }
}
