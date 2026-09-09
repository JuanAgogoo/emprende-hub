import artesanias from './assets/categorias/artesanias.png';
import belleza from './assets/categorias/belleza.png';
import deportes from './assets/categorias/deportes.png';
import educacion from './assets/categorias/educacion.png';
import eventos from './assets/categorias/eventos.png';
import finanzas from './assets/categorias/finanzas.png';
import gastronomia from './assets/categorias/gastronomia.png';
import hogar from './assets/categorias/hogar.png';
import mascotas from './assets/categorias/mascotas.png';
import moda from './assets/categorias/moda.png';
import saludYBienestar from './assets/categorias/salud-y-bienestar.png';
import tecnologia from './assets/categorias/tecnologia.png';

/**
 * La ilustración de cada una de las 12 categorías (G1).
 *
 * Se indexa por **nombre** y no por identificador: los ids los pone la base al
 * sembrarse y cambian si se recrea, así que atarse a ellos rompería las imágenes
 * en cuanto alguien hiciera `docker compose down -v`.
 *
 * Los ficheros se importan uno a uno en vez de componer la ruta al vuelo. Cuesta
 * doce líneas y a cambio **un nombre mal escrito es un error de compilación** y
 * no una imagen rota en la portada, que es lo que prohíbe `diseno.md`. Vite les
 * pone su hash y las sirve desde el paquete.
 */
const IMAGENES: Readonly<Record<string, string>> = {
  Artesanías: artesanias,
  Belleza: belleza,
  Deportes: deportes,
  Educación: educacion,
  Eventos: eventos,
  Finanzas: finanzas,
  Gastronomía: gastronomia,
  Hogar: hogar,
  Mascotas: mascotas,
  Moda: moda,
  'Salud y bienestar': saludYBienestar,
  Tecnología: tecnologia,
};

/**
 * La ilustración de una categoría, o `null` si no la tiene.
 *
 * Devuelve `null` en vez de una imagen por defecto para que quien la pinte
 * decida: en la portada se cae al emoji del catálogo, que siempre está.
 */
export function imagenDeCategoria(nombre: string): string | null {
  return IMAGENES[nombre] ?? null;
}
