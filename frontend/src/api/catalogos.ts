import { obtener } from './cliente';
import type { CategoriaNegocio, Ciudad } from '../types/catalogo';

export function obtenerCategoriasNegocio(): Promise<CategoriaNegocio[]> {
  return obtener<CategoriaNegocio[]>('/catalogos/categorias-negocio');
}

export function obtenerCiudades(): Promise<Ciudad[]> {
  return obtener<Ciudad[]>('/catalogos/ciudades');
}
