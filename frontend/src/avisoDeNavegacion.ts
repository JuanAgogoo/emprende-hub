/**
 * El aviso que deja una acción al volver a la lista: qué se decidió y sobre
 * quién. Viaja en el `state` de la navegación, como el «cuenta creada» del login.
 *
 * `location.state` es dato externo —sobrevive a una recarga y lo pone quien
 * navega—, así que se comprueba la forma antes de enseñarlo.
 */
export function avisoDeNavegacion(estado: unknown): string | null {
  if (typeof estado !== 'object' || estado === null) return null;
  const traspaso = estado as Record<string, unknown>;
  return typeof traspaso.aviso === 'string' ? traspaso.aviso : null;
}
