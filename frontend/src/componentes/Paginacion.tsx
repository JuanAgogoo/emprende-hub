import estilos from './Paginacion.module.css';

interface Props {
  /** Índice de la página actual, empezando en 0 como en Spring Data. */
  readonly pagina: number;
  readonly totalPaginas: number;
  readonly alCambiar: (pagina: number) => void;
}

export function Paginacion({ pagina, totalPaginas, alCambiar }: Props) {
  if (totalPaginas <= 1) return null;

  return (
    <nav className={estilos.paginacion} aria-label="Paginación de resultados">
      <button
        type="button"
        className={estilos.boton}
        onClick={() => alCambiar(pagina - 1)}
        disabled={pagina === 0}
      >
        Anterior
      </button>

      {/* Para la gente la primera página es la 1, no la 0. */}
      <span className={estilos.posicion} aria-live="polite">
        Página {pagina + 1} de {totalPaginas}
      </span>

      <button
        type="button"
        className={estilos.boton}
        onClick={() => alCambiar(pagina + 1)}
        disabled={pagina >= totalPaginas - 1}
      >
        Siguiente
      </button>
    </nav>
  );
}
