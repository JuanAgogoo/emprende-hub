import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { obtenerCategoriasNegocio } from '../api/catalogos';
import { buscarNegocios, obtenerDestacados } from '../api/directorio';
import { obtenerEstadisticasPortada } from '../api/estadisticas';
import { EsqueletoTarjetas } from '../componentes/Esqueleto';
import { TarjetaNegocio } from '../componentes/TarjetaNegocio';
import { imagenDeCategoria } from '../categorias';
import { calificacion, numero } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CategoriaNegocio } from '../types/catalogo';
import type { EstadisticasPortada } from '../types/estadisticas';
import type { TarjetaNegocio as Negocio } from '../types/negocio';
import type { Pagina } from '../types/pagina';
import estilos from './Inicio.module.css';
import { useTitulo } from '../titulo';

/** Todo lo que la portada necesita para pintarse. */
interface DatosPortada {
  readonly estadisticas: EstadisticasPortada;
  readonly destacados: readonly Negocio[];
  readonly categorias: readonly CategoriaNegocio[];
}

/** El resultado de una búsqueda, atado al texto que la pidió. */
interface Busqueda {
  readonly consulta: string;
  readonly carga: EstadoCarga<Pagina<Negocio>>;
}

/** Hasta aquí busca la portada; el resto se ve en el directorio. */
const MAXIMO_SUGERENCIAS = 6;

/**
 * Con una sola letra la lista sería todo el directorio y no ayuda a nadie.
 */
const MINIMO_LETRAS = 2;

/** Lo que se espera a que deje de escribir, para no lanzar una petición por tecla. */
const ESPERA_MS = 300;

const PASOS = [
  { titulo: 'Busca', texto: 'Filtra por categoría, ciudad, barrio y precio hasta dar con lo que necesitas.' },
  { titulo: 'Contacta', texto: 'Escribe al negocio o llama directamente. Sin intermediarios ni comisiones.' },
  { titulo: 'Opina', texto: 'Cuenta cómo te fue. Tu calificación ayuda a quien busca después.' },
] as const;

export function Inicio() {
  useTitulo();
  const [carga, setCarga] = useState<EstadoCarga<DatosPortada>>({ estado: 'CARGANDO' });
  const [texto, setTexto] = useState('');
  /**
   * Lo último que se buscó, **con el texto que lo produjo**.
   *
   * Guardar la consulta al lado del resultado es lo que impide que, al seguir
   * escribiendo, sigan en pantalla los resultados de lo anterior durante la
   * espera: si no coincide con lo que hay escrito, no se pinta.
   */
  const [busqueda, setBusqueda] = useState<Busqueda | null>(null);
  const resultados = useRef<HTMLDivElement>(null);

  const consulta = texto.trim();
  /** Solo se pinta lo que corresponde a lo que hay escrito ahora mismo. */
  const enPantalla =
    busqueda !== null && busqueda.consulta === consulta ? busqueda.carga : null;

  useEffect(() => {
    // Las tres llegan juntas: son del mismo backend y la portada no sirve de
    // nada a medias. Un solo estado en vez de tres cargas independientes.
    let vigente = true;

    Promise.all([obtenerEstadisticasPortada(), obtenerDestacados(), obtenerCategoriasNegocio()])
      .then(([estadisticas, destacados, categorias]) => {
        if (vigente) setCarga({ estado: 'EXITO', datos: { estadisticas, destacados, categorias } });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudo cargar la portada';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, []);

  /**
   * La búsqueda de la portada, **sin salir de la portada**.
   *
   * Espera a que se deje de escribir antes de preguntar: sin eso saldría una
   * petición por tecla. El guardia `vigente` evita que una respuesta lenta de
   * hace tres letras pise a la de ahora, que es el fallo clásico de un buscador
   * que va escribiendo.
   */
  useEffect(() => {
    if (consulta.length < MINIMO_LETRAS) return;

    let vigente = true;
    const espera = setTimeout(() => {
      setBusqueda({ consulta, carga: { estado: 'CARGANDO' } });
      buscarNegocios({ texto: consulta, size: MAXIMO_SUGERENCIAS })
        .then((pagina) => {
          if (vigente) setBusqueda({ consulta, carga: { estado: 'EXITO', datos: pagina } });
        })
        .catch((error: unknown) => {
          const mensaje = error instanceof Error ? error.message : 'No se pudo buscar';
          if (vigente) setBusqueda({ consulta, carga: { estado: 'ERROR', mensaje } });
        });
    }, ESPERA_MS);

    return () => {
      vigente = false;
      clearTimeout(espera);
    };
  }, [consulta]);

  /**
   * El botón ya no lleva a ninguna parte: los resultados están debajo.
   *
   * Lo que hace es mover el foco hasta ellos, que es lo que necesita quien
   * navega con teclado y no ve que la lista ha aparecido sola.
   */
  function buscar(evento: FormEvent) {
    evento.preventDefault();
    resultados.current?.focus();
  }

  return (
    <>
      {/* 1 · Titular, con el buscador debajo */}
      <section className={`contenedor ${estilos.hero}`}>
        <h1 className={estilos.titulo}>
          Los emprendimientos de tu barrio,{' '}
          <span className={estilos.destacado}>en un solo sitio</span>
        </h1>
        <p className={estilos.entrada}>
          Explora negocios del Valle de Aburrá sin necesidad de registrarte. Y si el negocio es
          tuyo, publícalo y deja que te encuentren.
        </p>

        <form className={estilos.buscador} onSubmit={buscar} role="search">
          <label className={estilos.etiquetaOculta} htmlFor="buscar-portada">
            Buscar negocios
          </label>
          <input
            id="buscar-portada"
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

        {enPantalla !== null && (
          <div className={estilos.resultados} ref={resultados} tabIndex={-1}>
            <ResultadosDeBusqueda busqueda={enPantalla} texto={consulta} />
          </div>
        )}
      </section>

      <ContenidoPortada carga={carga} />

      {/* 5 · Cómo funciona */}
      <section className={`contenedor ${estilos.seccion}`}>
        <h2>Cómo funciona</h2>
        <ol className={estilos.pasos}>
          {PASOS.map((paso, posicion) => (
            <li key={paso.titulo} className={estilos.paso}>
              <span className={estilos.numeroPaso} aria-hidden="true">
                {posicion + 1}
              </span>
              <h3 className={estilos.tituloPaso}>{paso.titulo}</h3>
              <p>{paso.texto}</p>
            </li>
          ))}
        </ol>
      </section>

      {/* 6 · Garantía · 7 · Llamada a la acción */}
      <section className={`contenedor ${estilos.cierre}`}>
        <h2 className={estilos.tituloCierre}>Cada negocio pasa por revisión</h2>
        <p className={estilos.entradaCierre}>
          Ninguno aparece en el directorio sin que alguien lo haya revisado antes. Lo que ves
          publicado está comprobado.
        </p>
        <div className={estilos.acciones}>
          <Link to="/directorio" className={estilos.primario}>
            Explorar el directorio
          </Link>
          <Link to="/registro-emprendedor" className={estilos.secundario}>
            Publicar mi negocio
          </Link>
        </div>
      </section>
    </>
  );
}

/** La parte que depende del backend: cifras, destacados y categorías. */
function ContenidoPortada({ carga }: { readonly carga: EstadoCarga<DatosPortada> }) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <section className={`contenedor ${estilos.seccion}`} aria-busy="true">
          <h2>Negocios destacados</h2>
          <EsqueletoTarjetas cuantas={3} />
        </section>
      );

    case 'ERROR':
      return (
        <section className={`contenedor ${estilos.seccion}`}>
          <p className={estilos.error} role="alert">
            No se pudieron cargar los datos: {carga.mensaje}. Comprueba que la API esté
            funcionando y vuelve a intentarlo.
          </p>
        </section>
      );

    case 'EXITO': {
      const { estadisticas, destacados, categorias } = carga.datos;
      const nota = calificacion(estadisticas.calificacionPromedio);

      return (
        <>
          {/* 4 · Prueba social: cifras calculadas, nunca inventadas (H4) */}
          <section className={estilos.franjaCifras}>
            <dl className={`contenedor ${estilos.cifras}`}>
              <Cifra valor={numero(estadisticas.negociosActivos)} etiqueta="Negocios activos" />
              <Cifra valor={numero(estadisticas.categorias)} etiqueta="Categorías" />
              <Cifra valor={numero(estadisticas.usuariosRegistrados)} etiqueta="Personas registradas" />
              <Cifra valor={nota ?? '—'} etiqueta="Calificación media" />
            </dl>
          </section>

          {/* 2 · Foco visual */}
          <section className={`contenedor ${estilos.seccion}`}>
            <h2>Negocios destacados</h2>
            {destacados.length === 0 ? (
              <p className={estilos.vacio}>
                Todavía no hay destacados. Un negocio entra aquí cuando reúne al menos cinco
                opiniones, para que una sola reseña no decida la portada.
              </p>
            ) : (
              <div className={estilos.rejilla}>
                {destacados.map((negocio) => (
                  <TarjetaNegocio key={negocio.id} negocio={negocio} />
                ))}
              </div>
            )}
          </section>

          {/* 3 · Explora por categoría */}
          <section className={`contenedor ${estilos.seccion}`}>
            <h2>Explora por categoría</h2>
            <ul className={estilos.categorias}>
              {categorias.map((categoria) => {
                const imagen = imagenDeCategoria(categoria.nombre);

                return (
                  <li key={categoria.id}>
                    <Link
                      to={`/directorio?categoriaId=${categoria.id}`}
                      className={estilos.categoria}
                    >
                      {/* Decorativa: el nombre va justo debajo. Si alguna
                          categoría se quedara sin ilustración, cae al emoji del
                          catálogo en vez de dejar una imagen rota. */}
                      {imagen === null ? (
                        <span className={estilos.emojiCategoria} aria-hidden="true">
                          {categoria.icono}
                        </span>
                      ) : (
                        <img className={estilos.imagenCategoria} src={imagen} alt="" />
                      )}
                      <span className={estilos.nombreCategoria}>{categoria.nombre}</span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          </section>
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}

/**
 * Lo que sale debajo del buscador mientras se escribe.
 *
 * Es una lista compacta, no el directorio: enseña las primeras y ofrece el
 * enlace para verlas todas allí. La portada no tiene que convertirse en otra
 * pantalla de listado.
 */
function ResultadosDeBusqueda({
  busqueda,
  texto,
}: {
  readonly busqueda: EstadoCarga<Pagina<Negocio>>;
  readonly texto: string;
}) {
  switch (busqueda.estado) {
    case 'CARGANDO':
      return (
        <p className={estilos.buscando} aria-live="polite">
          Buscando «{texto}»…
        </p>
      );

    case 'ERROR':
      return (
        <p className={estilos.errorBusqueda} role="alert">
          No se pudo buscar: {busqueda.mensaje}.
        </p>
      );

    case 'EXITO': {
      const { content, totalElements } = busqueda.datos;

      if (content.length === 0) {
        return (
          <p className={estilos.sinResultados} aria-live="polite">
            Ningún negocio coincide con «{texto}». Prueba con otra palabra o explora el{' '}
            <Link to="/directorio">directorio completo</Link>.
          </p>
        );
      }

      return (
        <>
          <p className={estilos.recuento} aria-live="polite">
            {totalElements === 1 ? '1 negocio' : `${totalElements} negocios`} para «{texto}»
          </p>

          <ul className={estilos.listaResultados}>
            {content.map((negocio) => (
              <li key={negocio.id}>
                <Link to={`/negocios/${negocio.id}`} className={estilos.resultado}>
                  {negocio.fotoPrincipal === null ? (
                    <span className={estilos.inicial} aria-hidden="true">
                      {negocio.nombre.charAt(0)}
                    </span>
                  ) : (
                    <img className={estilos.fotoResultado} src={negocio.fotoPrincipal} alt="" />
                  )}

                  <span className={estilos.datosResultado}>
                    <span className={estilos.nombreResultado}>{negocio.nombre}</span>
                    <span className={estilos.metaResultado}>
                      {negocio.categoria} <span aria-hidden="true">·</span> {negocio.ciudad}
                    </span>
                  </span>

                  {/* Sin opiniones no es lo mismo que cero (C5). */}
                  <span className={estilos.notaResultado}>
                    {calificacion(negocio.calificacionPromedio) ?? 'Sin opiniones'}
                  </span>
                </Link>
              </li>
            ))}
          </ul>

          {totalElements > content.length && (
            <p className={estilos.verTodos}>
              <Link to={`/directorio?texto=${encodeURIComponent(texto)}`}>
                Ver los {totalElements} resultados en el directorio
              </Link>
            </p>
          )}
        </>
      );
    }

    default:
      return casoImposible(busqueda);
  }
}

function Cifra({ valor, etiqueta }: { readonly valor: string; readonly etiqueta: string }) {
  return (
    <div className={estilos.cifra}>
      <dt className={estilos.cifraEtiqueta}>{etiqueta}</dt>
      <dd className={estilos.cifraValor}>{valor}</dd>
    </div>
  );
}
