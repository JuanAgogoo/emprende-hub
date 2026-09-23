import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { obtenerMiNegocio } from '../api/negocios';
import { listarOpiniones, obtenerMiOpinion, publicarOpinion } from '../api/opiniones';
import { useSesion } from '../estado/SesionContext';
import { EsqueletoOpiniones } from './Esqueleto';
import { Estrellas } from './Estrellas';
import { FormularioOpinion } from './FormularioOpinion';
import { Paginacion } from './Paginacion';
import { fecha, numero } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { DatosDeOpinion, Opinion } from '../types/opinion';
import type { Pagina } from '../types/pagina';
import type { Rol } from '../types/sesion';
import estilos from './Opiniones.module.css';

interface Props {
  readonly negocioId: number;
  /** Se llama al publicar: el perfil relee su promedio y su recuento (C4). */
  readonly alPublicar: () => void;
}

/**
 * Cinco por página. El perfil ya es largo, y con cinco el paginador aparece
 * pronto en vez de esconder las opiniones antiguas detrás de un listado
 * interminable.
 */
const POR_PAGINA = 5;

/**
 * Qué puede hacer quien mira, que es lo que decide si se dibuja el formulario.
 *
 * Se resuelve preguntando antes de dibujar nada, no dejando que el backend
 * responda `400`: quien ya opinó no entendería ese error ni podría arreglarlo.
 */
type Participacion =
  | { readonly estado: 'COMPROBANDO' }
  | { readonly estado: 'SIN_SESION' }
  | { readonly estado: 'ES_DUENO' }
  | { readonly estado: 'PUEDE_OPINAR' }
  | { readonly estado: 'YA_OPINO'; readonly mia: Opinion };

/** Un 404 aquí no es un error: es que esa cuenta no tiene negocio. */
async function negocioPropio(): Promise<number | null> {
  try {
    return (await obtenerMiNegocio()).id;
  } catch (error: unknown) {
    if (error instanceof ErrorApi && error.estado === 404) return null;
    throw error;
  }
}

/** Ni aquí: es que todavía no ha opinado sobre este negocio (C2). */
async function opinionPropia(negocioId: number): Promise<Opinion | null> {
  try {
    return await obtenerMiOpinion(negocioId);
  } catch (error: unknown) {
    if (error instanceof ErrorApi && error.estado === 404) return null;
    throw error;
  }
}

async function averiguarParticipacion(negocioId: number, rol: Rol): Promise<Participacion> {
  // Solo un emprendedor puede ser el dueño (A4), así que a nadie más se le
  // pregunta por su negocio.
  if (rol === 'EMPRENDEDOR' && (await negocioPropio()) === negocioId) {
    return { estado: 'ES_DUENO' };
  }

  const mia = await opinionPropia(negocioId);
  return mia === null ? { estado: 'PUEDE_OPINAR' } : { estado: 'YA_OPINO', mia };
}

/**
 * Las opiniones del perfil público: leerlas y, con sesión, escribir la tuya.
 *
 * Pide su propia página porque no viene dentro del perfil: son dos endpoints
 * distintos y paginar aquí no tiene por qué recargar el negocio entero.
 */
export function Opiniones({ negocioId, alPublicar }: Props) {
  const { sesion } = useSesion();
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Opinion>>>({ estado: 'CARGANDO' });
  const [pagina, setPagina] = useState(0);
  // Cambiarlo es lo que vuelve a disparar el efecto: lo usan «Reintentar» y la
  // publicación, que tiene que enseñar la nueva opinión sin recargar (C4).
  const [intento, setIntento] = useState(0);
  const [participacion, setParticipacion] = useState<Participacion>({ estado: 'COMPROBANDO' });
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    listarOpiniones(negocioId, pagina, POR_PAGINA)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        const mensaje =
          error instanceof Error ? error.message : 'No se pudieron cargar las opiniones';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [negocioId, pagina, intento]);

  useEffect(() => {
    let vigente = true;

    if (sesion === null) {
      setParticipacion({ estado: 'SIN_SESION' });
      return;
    }

    setParticipacion({ estado: 'COMPROBANDO' });

    averiguarParticipacion(negocioId, sesion.rol)
      .then((resuelta) => {
        if (vigente) setParticipacion(resuelta);
      })
      .catch(() => {
        // Si la comprobación falla no se esconde el formulario: se deja
        // intentarlo, y el backend dirá que no si es que no.
        if (vigente) setParticipacion({ estado: 'PUEDE_OPINAR' });
      });

    return () => {
      vigente = false;
    };
  }, [negocioId, sesion]);

  async function publicar(datos: DatosDeOpinion) {
    setEnviando(true);
    setFallo(null);

    try {
      const mia = await publicarOpinion(negocioId, datos);
      setParticipacion({ estado: 'YA_OPINO', mia });
      // La suya es la más reciente, así que entra arriba de la primera página.
      setPagina(0);
      setIntento((valor) => valor + 1);
      alPublicar();
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo publicar tu opinión';
      setFallo(mensaje);
    } finally {
      setEnviando(false);
    }
  }

  const total = carga.estado === 'EXITO' ? carga.datos.totalElements : 0;

  return (
    <section className={estilos.bloque}>
      <h2 className={estilos.titulo}>
        Opiniones
        {total > 0 && (
          <span className={estilos.contador}>
            {numero(total)} {total === 1 ? 'opinión' : 'opiniones'}
          </span>
        )}
      </h2>

      <TuParte
        participacion={participacion}
        enviando={enviando}
        fallo={fallo}
        alPublicar={publicar}
      />

      <Listado
        carga={carga}
        alPaginar={setPagina}
        alReintentar={() => setIntento((valor) => valor + 1)}
      />
    </section>
  );
}

interface PropsTuParte {
  readonly participacion: Participacion;
  readonly enviando: boolean;
  readonly fallo: string | null;
  readonly alPublicar: (datos: DatosDeOpinion) => void;
}

/** Lo que se le ofrece a quien mira, según quién sea. */
function TuParte({ participacion, enviando, fallo, alPublicar }: PropsTuParte) {
  // Volver aquí después de entrar: el login lo lee de `state`.
  const { pathname } = useLocation();

  switch (participacion.estado) {
    // Mientras se comprueba no se enseña nada: dibujar el formulario y
    // quitarlo medio segundo después es peor que esperar a saberlo.
    case 'COMPROBANDO':
      return null;

    case 'SIN_SESION':
      return (
        <p className={estilos.invitacion}>
          <Link to="/entrar" state={{ volverA: pathname }}>
            Entra con tu cuenta
          </Link>{' '}
          para calificar este negocio y dejar tu reseña.
        </p>
      );

    // A4: el dueño no opina sobre lo suyo. Se dice, en vez de dejar un hueco.
    case 'ES_DUENO':
      return (
        <p className={estilos.invitacion}>
          Este negocio es tuyo. Las opiniones las escriben quienes te compran.
        </p>
      );

    case 'PUEDE_OPINAR':
      return <FormularioOpinion enviando={enviando} fallo={fallo} alEnviar={alPublicar} />;

    // C2: una por persona y negocio. La suya se enseña arriba porque con varias
    // páginas podría estar en cualquiera.
    case 'YA_OPINO':
      return (
        <div className={estilos.tuya}>
          <h3 className={estilos.subtitulo}>Tu opinión</h3>
          <ul className={estilos.lista}>
            <OpinionPublicada opinion={participacion.mia} />
          </ul>
        </div>
      );

    default:
      return casoImposible(participacion);
  }
}

interface PropsListado {
  readonly carga: EstadoCarga<Pagina<Opinion>>;
  readonly alPaginar: (pagina: number) => void;
  readonly alReintentar: () => void;
}

function Listado({ carga, alPaginar, alReintentar }: PropsListado) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoOpiniones cuantas={3} />
        </div>
      );

    case 'ERROR':
      return (
        <div className={estilos.error} role="alert">
          <p>
            No se pudieron cargar las opiniones: {carga.mensaje}. El resto del perfil sí se ve.
          </p>
          <button type="button" className={estilos.reintentar} onClick={alReintentar}>
            Reintentar
          </button>
        </div>
      );

    case 'EXITO': {
      const pagina = carga.datos;

      // Un negocio recién publicado no tiene ninguna, y eso es lo normal: no es
      // un hueco ni un error.
      if (pagina.content.length === 0) {
        return <p className={estilos.vacio}>Todavía nadie ha opinado sobre este negocio.</p>;
      }

      return (
        <>
          <ul className={estilos.lista}>
            {pagina.content.map((opinion) => (
              <OpinionPublicada key={opinion.id} opinion={opinion} />
            ))}
          </ul>
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

function OpinionPublicada({ opinion }: { readonly opinion: Opinion }) {
  return (
    <li className={estilos.opinion}>
      <div className={estilos.cabecera}>
        <span className={estilos.autor}>{opinion.autor}</span>
        <time className={estilos.fecha} dateTime={opinion.fechaCreacion}>
          {fecha(opinion.fechaCreacion)}
        </time>
      </div>

      <p className={estilos.nota}>
        <Estrellas valor={opinion.calificacion} />
        {/* Las estrellas son decorativas: la nota que se lee es esta. */}
        <span className={estilos.cifra}>{opinion.calificacion} de 5</span>
        {opinion.editada && <span className={estilos.editada}>Editada</span>}
      </p>

      {opinion.comentario !== null && opinion.comentario !== '' && (
        <p className={estilos.comentario}>{opinion.comentario}</p>
      )}
    </li>
  );
}
