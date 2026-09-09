/**
 * El estado de una vista que carga datos.
 *
 * Es una unión discriminada: al consumirla con un `switch`, si mañana se añade
 * una variante y se olvida tratarla, el compilador avisa en el `default`. Esa
 * es la técnica del curso y la razón de que esto no sea un par de booleanos.
 */
export type EstadoCarga<T> =
  | { readonly estado: 'CARGANDO' }
  | { readonly estado: 'EXITO'; readonly datos: T }
  | { readonly estado: 'ERROR'; readonly mensaje: string };

/**
 * Se llama en el `default` de un `switch` sobre `EstadoCarga`. Si alguna
 * variante quedó sin tratar, el argumento deja de ser `never` y no compila.
 */
export function casoImposible(valor: never): never {
  throw new Error(`Caso sin tratar: ${JSON.stringify(valor)}`);
}
