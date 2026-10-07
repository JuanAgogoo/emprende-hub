import { enviar } from './cliente';
import type { MotivoDenuncia } from '../types/denuncia';

/**
 * Denuncia una opinión (C3). Responde `204`.
 *
 * No la oculta: sigue publicada mientras el administrador decide. El backend
 * responde `400` a quien denuncia la suya o la misma dos veces.
 */
export function denunciarOpinion(opinionId: number, motivo: MotivoDenuncia): Promise<void> {
  return enviar<void>(`/opiniones/${opinionId}/denuncias`, { motivo }, true);
}
