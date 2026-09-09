import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { obtenerCategoriasNegocio } from '../api/catalogos';
import { obtenerDestacados } from '../api/directorio';
import { obtenerEstadisticasPortada } from '../api/estadisticas';
import { EsqueletoTarjetas } from '../componentes/Esqueleto';
import { TarjetaNegocio } from '../componentes/TarjetaNegocio';
import { calificacion, numero } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CategoriaNegocio } from '../types/catalogo';
import type { EstadisticasPortada } from '../types/estadisticas';
import type { TarjetaNegocio as Negocio } from '../types/negocio';
import estilos from './Inicio.module.css';

/** Todo lo que la portada necesita para pintarse. */
interface DatosPortada {
  readonly estadisticas: EstadisticasPortada;
  readonly destacados: readonly Negocio[];
  readonly categorias: readonly CategoriaNegocio[];
}

const PASOS = [
  { titulo: 'Busca', texto: 'Filtra por categoría, ciudad, barrio y precio hasta dar con lo que necesitas.' },
  { titulo: 'Contacta', texto: 'Escribe al negocio o llama directamente. Sin intermediarios ni comisiones.' },
  { titulo: 'Opina', texto: 'Cuenta cómo te fue. Tu calificación ayuda a quien busca después.' },
] as const;

export function Inicio() {
  const [carga, setCarga] = useState<EstadoCarga<DatosPortada>>({ estado: 'CARGANDO' });
  const [texto, setTexto] = useState('');
  const navegar = useNavigate();

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

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    const limpio = texto.trim();
    navegar(limpio === '' ? '/directorio' : `/directorio?texto=${encodeURIComponent(limpio)}`);
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
              {categorias.map((categoria) => (
                <li key={categoria.id}>
                  <Link
                    to={`/directorio?categoriaId=${categoria.id}`}
                    className={estilos.categoria}
                  >
                    <span aria-hidden="true">{categoria.icono}</span>
                    {categoria.nombre}
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        </>
      );
    }

    default:
      return casoImposible(carga);
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
