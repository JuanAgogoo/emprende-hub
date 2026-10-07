import { obtener } from './cliente';
import type { CategoriaNegocio, Ciudad, Opcion } from '../types/catalogo';
import type { CategoriaCurso, NivelCurso } from '../types/curso';
import type { MotivoDenuncia } from '../types/denuncia';

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

/** Los motivos para denunciar una opinión, en el orden en que se ofrecen (C6). */
export function obtenerMotivosDenuncia(): Promise<Opcion<MotivoDenuncia>[]> {
  return obtener<Opcion<MotivoDenuncia>[]>('/catalogos/motivos-denuncia');
}
