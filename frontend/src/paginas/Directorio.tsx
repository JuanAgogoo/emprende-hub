import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { obtenerCategoriasNegocio, obtenerCiudades } from '../api/catalogos';
import { buscarNegocios } from '../api/directorio';
import { EsqueletoTarjetas } from '../componentes/Esqueleto';
import { FiltrosDirectorio as PanelFiltros } from '../componentes/FiltrosDirectorio';
import { Paginacion } from '../componentes/Paginacion';
import { TarjetaNegocio } from '../componentes/TarjetaNegocio';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CategoriaNegocio, Ciudad } from '../types/catalogo';
import type { FiltrosDirectorio, NivelPrecio, OrdenDirectorio } from '../types/negocio';
import type { Pagina } from '../types/pagina';
import type { TarjetaNegocio as Negocio } from '../types/negocio';
import estilos from './Directorio.module.css';
import { useTitulo } from '../titulo';

const POR_PAGINA = 12;

interface Catalogos {
  readonly categorias: readonly CategoriaNegocio[];
  readonly ciudades: readonly Ciudad[];
}

/**
 * Los filtros viven en la barra de direcciones, no en un estado interno: así el
 * enlace de la portada —`/directorio?categoriaId=3`— funciona sin nada más, y
 * una búsqueda se puede compartir o recargar sin perderla.
 */
function leerFiltros(parametros: URLSearchParams): FiltrosDirectorio {
  const numero = (clave: string): number | undefined => {
    const valor = parametros.get(clave);
    if (valor === null || valor === '') return undefined;
    const convertido = Number(valor);
    return Number.isNaN(convertido) ? undefined : convertido;
  };

  const texto = parametros.get('texto') ?? undefined;
  const nivel = parametros.get('nivelPrecio');
  const orden = parametros.get('orden');

  return {
    texto: texto === '' ? undefined : texto,
    categoriaId: numero('categoriaId'),
    ciudadId: numero('ciudadId'),
    barrioId: numero('barrioId'),
    calificacionMinima: numero('calificacionMinima'),
    nivelPrecio: nivel === null ? undefined : (nivel as NivelPrecio),
    orden: orden === null ? undefined : (orden as OrdenDirectorio),
    page: numero('page'),
  };
}

export function Directorio() {
  useTitulo('Directorio');
  const [parametros, setParametros] = useSearchParams();
  const filtros = leerFiltros(parametros);

  const [catalogos, setCatalogos] = useState<Catalogos>({ categorias: [], ciudades: [] });
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Negocio>>>({ estado: 'CARGANDO' });
  const [texto, setTexto] = useState(filtros.texto ?? '');

  // Los catálogos no cambian: se piden una vez.
  useEffect(() => {
    let vigente = true;
    Promise.all([obtenerCategoriasNegocio(), obtenerCiudades()])
      .then(([categorias, ciudades]) => {
        if (vigente) setCatalogos({ categorias, ciudades });
      })
      .catch(() => {
        // Sin catálogos los desplegables salen vacíos, pero el listado funciona:
        // no es motivo para tumbar la página entera.
      });
    return () => {
      vigente = false;
    };
  }, []);

  // La búsqueda depende de la barra de direcciones, así que se rehace con ella.
  const consultaActual = parametros.toString();
  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    buscarNegocios({ ...leerFiltros(new URLSearchParams(consultaActual)), size: POR_PAGINA })
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
  }, [consultaActual]);

  /** Aplica un cambio de filtro y vuelve a la primera página. */
  function cambiar(cambio: Partial<FiltrosDirectorio>) {
    const siguientes = new URLSearchParams(parametros);
    for (const [clave, valor] of Object.entries(cambio)) {
      if (valor === undefined || valor === '') siguientes.delete(clave);
      else siguientes.set(clave, String(valor));
    }
    // Cambiar un filtro y quedarse en la página 4 enseña resultados vacíos.
    siguientes.delete('page');
    setParametros(siguientes);
  }

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    cambiar({ texto: texto.trim() });
  }

  function limpiar() {
    setTexto('');
    setParametros(new URLSearchParams());
  }

  function irA(pagina: number) {
    const siguientes = new URLSearchParams(parametros);
    siguientes.set('page', String(pagina));
    setParametros(siguientes);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  const resultados = carga.estado === 'EXITO' ? carga.datos.totalElements : 0;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.encabezado}>
        <h1>Directorio</h1>
        <form className={estilos.buscador} onSubmit={buscar} role="search">
          <label className={estilos.etiquetaOculta} htmlFor="buscar-directorio">
            Buscar por nombre o descripción
          </label>
          <input
            id="buscar-directorio"
            className={estilos.campo}
            type="search"
            placeholder="Panadería, peluquería, taller…"
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
          />
          <button className={estilos.primario} type="submit">
            Buscar
          </button>
        </form>
      </header>

      <div className={estilos.columnas}>
        <aside className={estilos.lateral}>
          <PanelFiltros
            categorias={catalogos.categorias}
            ciudades={catalogos.ciudades}
            filtros={filtros}
            resultados={resultados}
            alCambiar={cambiar}
            alLimpiar={limpiar}
          />
        </aside>

        <section className={estilos.resultados}>
          <Resultados carga={carga} alLimpiar={limpiar} alPaginar={irA} />
        </section>
      </div>
    </div>
  );
}

interface PropsResultados {
  readonly carga: EstadoCarga<Pagina<Negocio>>;
  readonly alLimpiar: () => void;
  readonly alPaginar: (pagina: number) => void;
}

function Resultados({ carga, alLimpiar, alPaginar }: PropsResultados) {
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
          No se pudo buscar: {carga.mensaje}. Comprueba que la API esté funcionando y vuelve a
          intentarlo.
        </p>
      );

    case 'EXITO': {
      const pagina = carga.datos;

      // Una lista vacía no es una lista sin nada: se dice qué pasó y qué hacer.
      if (pagina.content.length === 0) {
        return (
          <div className={estilos.vacio}>
            <h2 className={estilos.tituloVacio}>Ningún negocio coincide</h2>
            <p>
              Prueba con menos filtros o con otras palabras. También puede que ese negocio
              todavía no esté publicado.
            </p>
            <button type="button" className={estilos.limpiar} onClick={alLimpiar}>
              Quitar todos los filtros
            </button>
          </div>
        );
      }

      return (
        <>
          <div className={estilos.rejilla}>
            {pagina.content.map((negocio) => (
              <TarjetaNegocio key={negocio.id} negocio={negocio} />
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
