import { useEffect } from 'react';

/** La marca encabeza todos los títulos. */
const MARCA = 'EmprendeHub';

/**
 * Pone el título de la pestaña como «EmprendeHub | Contexto».
 *
 * **Sin contexto queda solo la marca**, que es lo que le toca a la portada: no
 * es una sección del sitio, es el sitio.
 *
 * Lo llama cada página en vez de resolverse en un mapa central junto a las
 * rutas. Un mapa aparte se desincroniza —ya pasó con los enlaces del pie, que
 * apuntaron cuatro incrementos a rutas que no existían—; el título puesto donde
 * se pinta la página no puede quedarse atrás.
 */
export function useTitulo(contexto?: string): void {
  useEffect(() => {
    document.title = contexto === undefined ? MARCA : `${MARCA} | ${contexto}`;
  }, [contexto]);
}
