package com.emprendehub.model;

/**
 * Criterios de ordenación del directorio, los tres que ofrece la interfaz.
 *
 * <p>Es un conjunto cerrado que llega como parámetro de consulta, así que la
 * ordenación nunca se construye con nombres de columna enviados por el cliente:
 * quien pida algo fuera de esta lista recibe un 400 y no una consulta arbitraria.
 *
 * <p>Se queda como {@code enum} y no como interfaz sellada porque ninguna de las
 * tres opciones lleva datos propios; la traducción a {@code Sort} vive en
 * {@code DirectorioService}, para que {@code model} no dependa de Spring Data.
 */
public enum OrdenDirectorio {

    /** Mejor calificados. Es el orden por defecto, igual que en el prototipo. */
    CALIFICACION,

    /** Nombre de la A a la Z. */
    NOMBRE,

    /** Más recientes: por fecha de aprobación, no de creación (G7). */
    RECIENTES
}
