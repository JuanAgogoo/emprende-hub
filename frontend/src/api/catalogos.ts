import { obtener } from './cliente';
import type { CategoriaNegocio } from '../types/catalogo';

export function obtenerCategoriasNegocio(): Promise<CategoriaNegocio[]> {
  return obtener<CategoriaNegocio[]>('/catalogos/categorias-negocio');
}
