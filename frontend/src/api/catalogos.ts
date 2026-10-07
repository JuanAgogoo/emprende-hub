import { obtener } from './cliente';
import type { CategoriaNegocio, Ciudad, Opcion } from '../types/catalogo';
import type { CategoriaCurso, NivelCurso } from '../types/curso';

export function obtenerCategoriasNegocio(): Promise<CategoriaNegocio[]> {
  return obtener<CategoriaNegocio[]>('/catalogos/categorias-negocio');
}

export function obtenerCiudades(): Promise<Ciudad[]> {
  return obtener<Ciudad[]>('/catalogos/ciudades');
}

/** Las 5 categorías de formación, con el nombre que se enseña. */
export function obtenerCategoriasCurso(): Promise<Opcion<CategoriaCurso>[]> {
  return obtener<Opcion<CategoriaCurso>[]>('/catalogos/categorias-curso');
}

export function obtenerNivelesCurso(): Promise<Opcion<NivelCurso>[]> {
  return obtener<Opcion<NivelCurso>[]>('/catalogos/niveles-curso');
}
