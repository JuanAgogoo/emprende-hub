import { consulta, enviar, obtener, parchear } from './cliente';
import type { Consulta, DatosDeConsulta } from '../types/consulta';
import type { Pagina } from '../types/pagina';

/**
 * Escribe al buzón de un negocio (D1). Exige sesión.
 *
 * La petición **no lleva nombre ni correo**: salen de la cuenta, y no de un
 * formulario donde cualquiera podría escribir el correo de otra persona.
 *
 * Responde `400` a quien escriba a su propio buzón, y `404` a un negocio sin
 * publicar, igual que su perfil (B6).
 */
export function enviarConsulta(negocioId: number, datos: DatosDeConsulta): Promise<Consulta> {
  return enviar<Consulta>(`/negocios/${negocioId}/consultas`, datos, true);
}

/**
 * El buzón del negocio propio, las más recientes primero.
 *
 * Solo lo lee su dueño, y es la única respuesta de la API que trae el correo de
 * otra persona (D2).
 */
/**
 * Lo que la persona con sesión le ha escrito a ese negocio, de lo más antiguo a
 * lo más reciente: el historial de la vista de chat.
 */
export function listarConsultasEnviadas(negocioId: number): Promise<Consulta[]> {
  return obtener<Consulta[]>(`/negocios/${negocioId}/consultas/mias`, true);
}

export function listarMisConsultas(pagina: number, porPagina: number): Promise<Pagina<Consulta>> {
  return obtener<Pagina<Consulta>>(
    `/negocios/mio/consultas${consulta({ page: pagina, size: porPagina })}`,
    true,
  );
}

/** Marca una consulta como leída, o la devuelve a pendiente. */
/** El dueño responde una consulta de su buzón, o corrige su respuesta (D2). */
export function responderConsulta(id: number, respuesta: string): Promise<Consulta> {
  return parchear<Consulta>(`/negocios/mio/consultas/${id}/respuesta`, { respuesta }, true);
}

export function marcarConsultaLeida(id: number, leida: boolean): Promise<Consulta> {
  return parchear<Consulta>(
    `/negocios/mio/consultas/${id}/lectura${consulta({ leida: String(leida) })}`,
    undefined,
    true,
  );
}
