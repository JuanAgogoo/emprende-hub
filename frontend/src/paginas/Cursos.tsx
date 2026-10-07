import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { obtenerCategoriasCurso, obtenerNivelesCurso } from '../api/catalogos';
import { buscarCursos } from '../api/cursos';
import { EsqueletoTarjetas } from '../componentes/Esqueleto';
import { Paginacion } from '../componentes/Paginacion';
import { TarjetaCurso } from '../componentes/TarjetaCurso';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { Opcion } from '../types/catalogo';
import type { CategoriaCurso, Curso, FiltrosCursos, NivelCurso } from '../types/curso';
import type { Pagina } from '../types/pagina';
import estilos from './Cursos.module.css';
import { useTitulo } from '../titulo';

const POR_PAGINA = 12;

/** Lo que se espera a que se deje de escribir antes de buscar. */
const ESPERA_AL_ESCRIBIR = 300;

interface Catalogos {
  readonly categorias: readonly Opcion<CategoriaCurso>[];
  readonly niveles: readonly Opcion<NivelCurso>[];
}

/**
 * Los filtros viven en la barra de direcciones, igual que en el directorio: una
 * búsqueda se comparte o se recarga sin perderla.
 *
 * Lo que llega de la URL es texto que cualquiera puede escribir, así que solo
 * se acepta un código que esté en el catálogo; otro cualquiera se ignora en vez
 * de mandarlo al backend a que responda 400.
 */
function leerFiltros(parametros: URLSearchParams, catalogos: Catalogos): FiltrosCursos {
  const texto = parametros.get('texto') ?? '';
  const categoria = catalogos.categorias.find((c) => c.codigo === parametros.get('categoria'));
  const nivel = catalogos.niveles.find((n) => n.codigo === parametros.get('nivel'));
  const pagina = Number(parametros.get('page') ?? '0');

  return {
    texto: texto === '' ? undefined : texto,
    categoria: categoria?.codigo,
    nivel: nivel?.codigo,
    gratuito: parametros.get('gratuito') === 'true' ? true : undefined,
    page: Number.isInteger(pagina) && pagina > 0 ? pagina : undefined,
  };
}

/** El nombre que se enseña, o el código si el catálogo no llegó a cargar. */
function nombreDe<C extends string>(opciones: readonly Opcion<C>[], codigo: C): string {
  return opciones.find((opcion) => opcion.codigo === codigo)?.nombre ?? codigo;
}

export function Cursos() {
  useTitulo('Cursos');
  const [parametros, setParametros] = useSearchParams();

  const [catalogos, setCatalogos] = useState<Catalogos | null>(null);
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Curso>>>({ estado: 'CARGANDO' });
  const [texto, setTexto] = useState(parametros.get('texto') ?? '');
  const temporizador = useRef<number | undefined>(undefined);

  // Los catálogos no cambian: se piden una vez. Sin ellos no se puede validar
  // lo que trae la URL, así que la búsqueda espera a que lleguen.
  useEffect(() => {
    let vigente = true;
    Promise.all([obtenerCategoriasCurso(), obtenerNivelesCurso()])
      .then(([categorias, niveles]) => {
        if (vigente) setCatalogos({ categorias, niveles });
      })
      .catch(() => {
        // Sin catálogos los desplegables salen vacíos, pero el catálogo de
        // cursos se sigue pudiendo recorrer: no es motivo para tumbar la página.
        if (vigente) setCatalogos({ categorias: [], niveles: [] });
      });
    return () => {
      vigente = false;
    };
  }, []);

  const filtros = catalogos === null ? null : leerFiltros(parametros, catalogos);

  // La búsqueda depende de la barra de direcciones, así que se rehace con ella.
  const consultaActual = parametros.toString();
  useEffect(() => {
    if (catalogos === null) return;
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    buscarCursos({
      ...leerFiltros(new URLSearchParams(consultaActual), catalogos),
      size: POR_PAGINA,
    })
      .then((pagina) => {
        if (vigente) setCarga({ estado: 'EXITO', datos: pagina });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudo buscar';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [consultaActual, catalogos]);

  // Si se sale de la página con una búsqueda en espera, no debe dispararse.
  useEffect(() => () => window.clearTimeout(temporizador.current), []);

  /**
   * Aplica un cambio de filtro y vuelve a la primera página.
   *
   * Parte de los parámetros vigentes **en el momento de aplicarse**, no de los
   * de cuando se pidió: el texto se aplica con retraso, y entre medias puede
   * haberse cambiado un desplegable que no se quiere perder.
   */
  function cambiar(cambio: Readonly<Record<string, string | undefined>>) {
    setParametros((actuales) => {
      const siguientes = new URLSearchParams(actuales);
      for (const [clave, valor] of Object.entries(cambio)) {
        if (valor === undefined || valor === '') siguientes.delete(clave);
        else siguientes.set(clave, valor);
      }
      siguientes.delete('page');
      return siguientes;
    });
  }

  /**
   * El buscador filtra según se escribe (HU-022), pero no en cada tecla: espera
   * a que se deje de escribir. Sin la espera, «marketing» serían nueve
   * peticiones, y la barra de direcciones guardaría nueve entradas.
   */
  function escribir(valor: string) {
    setTexto(valor);
    window.clearTimeout(temporizador.current);
    temporizador.current = window.setTimeout(
      () => cambiar({ texto: valor.trim() }),
      ESPERA_AL_ESCRIBIR,
    );
  }

  function limpiar() {
    window.clearTimeout(temporizador.current);
    setTexto('');
    setParametros(new URLSearchParams());
  }

  function irA(pagina: number) {
    setParametros((actuales) => {
      const siguientes = new URLSearchParams(actuales);
      siguientes.set('page', String(pagina));
      return siguientes;
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  const categorias = catalogos?.categorias ?? [];
  const niveles = catalogos?.niveles ?? [];
  const hayFiltros =
    filtros !== null &&
    (filtros.texto !== undefined ||
      filtros.categoria !== undefined ||
      filtros.nivel !== undefined ||
      filtros.gratuito !== undefined);
  const resultados = carga.estado === 'EXITO' ? carga.datos.totalElements : null;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.encabezado}>
        <h1>Cursos</h1>
        <p className={estilos.entrada}>
          Formación para hacer crecer tu negocio. Cada curso se hace en la página de quien lo
          imparte: los gratuitos se abren directamente, sin pasar por ningún pago.
        </p>
      </header>

      {/* Sin <form>: no hay nada que enviar, cada control aplica lo suyo. */}
      <section className={estilos.filtros} aria-label="Filtros del catálogo">
        <div className={`${estilos.campo} ${estilos.campoTexto}`}>
          <label htmlFor="buscar-curso">Buscar</label>
          <input
            id="buscar-curso"
            type="search"
            placeholder="Ventas, redes sociales, contabilidad…"
            value={texto}
            onChange={(evento) => escribir(evento.target.value)}
          />
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-categoria-curso">Categoría</label>
          <select
            id="filtro-categoria-curso"
            value={filtros?.categoria ?? ''}
            onChange={(evento) => cambiar({ categoria: evento.target.value })}
          >
            <option value="">Todas</option>
            {categorias.map((categoria) => (
              <option key={categoria.codigo} value={categoria.codigo}>
                {categoria.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className={estilos.campo}>
          <label htmlFor="filtro-nivel-curso">Nivel</label>
          <select
            id="filtro-nivel-curso"
            value={filtros?.nivel ?? ''}
            onChange={(evento) => cambiar({ nivel: evento.target.value })}
          >
            <option value="">Todos</option>
            {niveles.map((nivel) => (
              <option key={nivel.codigo} value={nivel.codigo}>
                {nivel.nombre}
              </option>
            ))}
          </select>
        </div>

        <label className={estilos.casilla}>
          <input
            type="checkbox"
            checked={filtros?.gratuito === true}
            onChange={(evento) =>
              cambiar({ gratuito: evento.target.checked ? 'true' : undefined })
            }
          />
          Solo gratuitos
        </label>
      </section>

      <div className={estilos.resumen}>
        <p aria-live="polite">
          {resultados === null ? '' : resultados === 1 ? '1 curso' : `${resultados} cursos`}
        </p>
        {hayFiltros && (
          <button type="button" className={estilos.limpiar} onClick={limpiar}>
            Quitar los filtros
          </button>
        )}
      </div>

      <Resultados
        carga={carga}
        catalogos={{ categorias, niveles }}
        alLimpiar={limpiar}
        alPaginar={irA}
      />
    </div>
  );
}

interface PropsResultados {
  readonly carga: EstadoCarga<Pagina<Curso>>;
  readonly catalogos: Catalogos;
  readonly alLimpiar: () => void;
  readonly alPaginar: (pagina: number) => void;
}

function Resultados({ carga, catalogos, alLimpiar, alPaginar }: PropsResultados) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoTarjetas cuantas={6} />
        </div>
      );

    case 'ERROR':
      return (
        <p className={estilos.error} role="alert">
          No se pudo cargar el catálogo: {carga.mensaje}. Comprueba que la API esté funcionando
          y vuelve a intentarlo.
        </p>
      );

    case 'EXITO': {
      const pagina = carga.datos;

      if (pagina.content.length === 0) {
        return (
          <div className={estilos.vacio}>
            <h2 className={estilos.tituloVacio}>Ningún curso coincide</h2>
            <p>Prueba con menos filtros o con otras palabras.</p>
            <button type="button" className={estilos.limpiar} onClick={alLimpiar}>
              Quitar todos los filtros
            </button>
          </div>
        );
      }

      return (
        <>
          <div className={estilos.rejilla}>
            {pagina.content.map((curso) => (
              <TarjetaCurso
                key={curso.id}
                curso={curso}
                categoria={nombreDe(catalogos.categorias, curso.categoria)}
                nivel={nombreDe(catalogos.niveles, curso.nivel)}
              />
            ))}
          </div>
          <Paginacion
            pagina={pagina.number}
            totalPaginas={pagina.totalPages}
            alCambiar={alPaginar}
          />
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}
