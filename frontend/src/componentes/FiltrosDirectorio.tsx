import type { CategoriaNegocio, Ciudad } from '../types/catalogo';
import type { FiltrosDirectorio, NivelPrecio, OrdenDirectorio } from '../types/negocio';
import estilos from './FiltrosDirectorio.module.css';

interface Props {
  readonly categorias: readonly CategoriaNegocio[];
  readonly ciudades: readonly Ciudad[];
  readonly filtros: FiltrosDirectorio;
  readonly resultados: number;
  readonly alCambiar: (cambio: Partial<FiltrosDirectorio>) => void;
  readonly alLimpiar: () => void;
}

const NIVELES: readonly { readonly valor: NivelPrecio; readonly texto: string }[] = [
  { valor: 'BAJO', texto: 'Económico ($)' },
  { valor: 'MEDIO', texto: 'Medio ($$)' },
  { valor: 'ALTO', texto: 'Alto ($$$)' },
];

const ORDENES: readonly { readonly valor: OrdenDirectorio; readonly texto: string }[] = [
  { valor: 'CALIFICACION', texto: 'Mejor calificados' },
  { valor: 'NOMBRE', texto: 'Nombre (A-Z)' },
  { valor: 'RECIENTES', texto: 'Más recientes' },
];

const CALIFICACIONES = [4.5, 4, 3] as const;

/** Convierte el valor de un `select` en número, o en `undefined` si está vacío. */
function aNumero(valor: string): number | undefined {
  return valor === '' ? undefined : Number(valor);
}

export function FiltrosDirectorio({
  categorias,
  ciudades,
  filtros,
  resultados,
  alCambiar,
  alLimpiar,
}: Props) {
  const ciudadElegida = ciudades.find((ciudad) => ciudad.id === filtros.ciudadId);
  const barrios = ciudadElegida?.barrios ?? [];
  const hayFiltros =
    filtros.categoriaId !== undefined ||
    filtros.ciudadId !== undefined ||
    filtros.barrioId !== undefined ||
    filtros.calificacionMinima !== undefined ||
    filtros.nivelPrecio !== undefined;

  return (
    /* En móvil se pliega; a partir de tablet el CSS lo deja siempre abierto.
       <details> lo hace sin una línea de JavaScript y sin perder el teclado. */
    <details className={estilos.panel} open>
      <summary className={estilos.resumen}>
        <span>Filtros</span>
        {hayFiltros && <span className={estilos.marca}>activos</span>}
      </summary>

      <div className={estilos.campos}>
        <p className={estilos.recuento} aria-live="polite">
          {resultados === 1 ? '1 negocio' : `${resultados} negocios`}
        </p>

        <div className={estilos.campo}>
          <label htmlFor="filtro-categoria">Categoría</label>
          <select
            id="filtro-categoria"
            value={filtros.categoriaId ?? ''}
            onChange={(evento) => alCambiar({ categoriaId: aNumero(evento.target.value) })}
          >
            <option value="">Todas</option>
            {categorias.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-ciudad">Ciudad</label>
          <select
            id="filtro-ciudad"
            value={filtros.ciudadId ?? ''}
            onChange={(evento) =>
              // Cambiar de ciudad invalida el barrio elegido: se limpia a la vez.
              alCambiar({ ciudadId: aNumero(evento.target.value), barrioId: undefined })
            }
          >
            <option value="">Todas</option>
            {ciudades.map((ciudad) => (
              <option key={ciudad.id} value={ciudad.id}>
                {ciudad.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-barrio">Barrio</label>
          <select
            id="filtro-barrio"
            value={filtros.barrioId ?? ''}
            disabled={barrios.length === 0}
            onChange={(evento) => alCambiar({ barrioId: aNumero(evento.target.value) })}
          >
            <option value="">Todos</option>
            {barrios.map((barrio) => (
              <option key={barrio.id} value={barrio.id}>
                {barrio.nombre}
              </option>
            ))}
          </select>
          {/* Deshabilitado en silencio no se entiende: se dice por qué. */}
          {barrios.length === 0 && (
            <small className={estilos.ayuda}>
              {ciudadElegida === undefined
                ? 'Elige antes una ciudad'
                : `${ciudadElegida.nombre} no tiene barrios cargados`}
            </small>
          )}
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-precio">Nivel de precio</label>
          <select
            id="filtro-precio"
            value={filtros.nivelPrecio ?? ''}
            onChange={(evento) =>
              alCambiar({
                nivelPrecio:
                  evento.target.value === '' ? undefined : (evento.target.value as NivelPrecio),
              })
            }
          >
            <option value="">Cualquiera</option>
            {NIVELES.map((nivel) => (
              <option key={nivel.valor} value={nivel.valor}>
                {nivel.texto}
              </option>
            ))}
          </select>
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-calificacion">Calificación mínima</label>
          <select
            id="filtro-calificacion"
            value={filtros.calificacionMinima ?? ''}
            onChange={(evento) => alCambiar({ calificacionMinima: aNumero(evento.target.value) })}
          >
            <option value="">Cualquiera</option>
            {CALIFICACIONES.map((nota) => (
              <option key={nota} value={nota}>
                {nota} o más
              </option>
            ))}
          </select>
          {filtros.calificacionMinima !== undefined && (
            <small className={estilos.ayuda}>Deja fuera a quien todavía no tiene opiniones</small>
          )}
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-orden">Ordenar por</label>
          <select
            id="filtro-orden"
            value={filtros.orden ?? 'CALIFICACION'}
            onChange={(evento) => alCambiar({ orden: evento.target.value as OrdenDirectorio })}
          >
            {ORDENES.map((orden) => (
              <option key={orden.valor} value={orden.valor}>
                {orden.texto}
              </option>
            ))}
          </select>
        </div>

        {hayFiltros && (
          <button type="button" className={estilos.limpiar} onClick={alLimpiar}>
            Quitar los filtros
          </button>
        )}
      </div>
    </details>
  );
}
