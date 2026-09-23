import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { obtenerMiNegocio } from '../api/negocios';
import {
  borrarOpinion,
  editarOpinion,
  listarOpiniones,
  obtenerMiOpinion,
  publicarOpinion,
} from '../api/opiniones';
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
  /**
   * Se llama cuando las opiniones cambian —al publicar, al editar y al
   * borrar—: el perfil relee su promedio y su recuento (C4, C5).
   */
  readonly alCambiarOpiniones: () => void;
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

/**
 * Qué se está haciendo con la opinión propia. El borrado pide confirmación
 * porque es destructivo y no se deshace.
 */
type ModoPropia = 'LECTURA' | 'EDITANDO' | 'CONFIRMANDO_BORRADO';

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
export function Opiniones({ negocioId, alCambiarOpiniones }: Props) {
  const { sesion } = useSesion();
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Opinion>>>({ estado: 'CARGANDO' });
  const [pagina, setPagina] = useState(0);
  // Cambiarlo es lo que vuelve a disparar el efecto: lo usan «Reintentar» y los
  // tres cambios de la opinión propia, que tienen que verse en la lista sin
  // recargar la página (C4).
  const [intento, setIntento] = useState(0);
  const [participacion, setParticipacion] = useState<Participacion>({ estado: 'COMPROBANDO' });
  const [modo, setModo] = useState<ModoPropia>('LECTURA');
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

  /** Vuelve a pedir la lista y deja que el perfil ponga al día su ficha. */
  function refrescar(volverAlPrincipio: boolean) {
    if (volverAlPrincipio) setPagina(0);
    setIntento((valor) => valor + 1);
    alCambiarOpiniones();
  }

  async function publicar(datos: DatosDeOpinion) {
    setEnviando(true);
    setFallo(null);

    try {
      const mia = await publicarOpinion(negocioId, datos);
      setParticipacion({ estado: 'YA_OPINO', mia });
      // La suya es la más reciente, así que entra arriba de la primera página.
      refrescar(true);
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo publicar tu opinión';
      setFallo(mensaje);
    } finally {
      setEnviando(false);
    }
  }

  async function editar(datos: DatosDeOpinion) {
    setEnviando(true);
    setFallo(null);

    try {
      const mia = await editarOpinion(negocioId, datos);
      setParticipacion({ estado: 'YA_OPINO', mia });
      setModo('LECTURA');
      // Editar no cambia el orden —la fecha de creación no se toca—, así que se
      // rehace la página que se está viendo y no la primera.
      refrescar(false);
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo guardar tu opinión';
      setFallo(mensaje);
    } finally {
      setEnviando(false);
    }
  }

  async function borrar() {
    setEnviando(true);
    setFallo(null);

    try {
      await borrarOpinion(negocioId);
      // Volver a opinar pasa a ser posible, así que vuelve el formulario.
      setParticipacion({ estado: 'PUEDE_OPINAR' });
      setModo('LECTURA');
      refrescar(true);
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo borrar tu opinión';
      setFallo(mensaje);
    } finally {
      setEnviando(false);
    }
  }

  /** Cambiar de modo descarta el fallo del intento anterior. */
  function cambiarModo(siguiente: ModoPropia) {
    setFallo(null);
    setModo(siguiente);
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
        modo={modo}
        enviando={enviando}
        fallo={fallo}
        alPublicar={publicar}
        alEditar={editar}
        alBorrar={borrar}
        alCambiarModo={cambiarModo}
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
  readonly modo: ModoPropia;
  readonly enviando: boolean;
  readonly fallo: string | null;
  readonly alPublicar: (datos: DatosDeOpinion) => void;
  readonly alEditar: (datos: DatosDeOpinion) => void;
  readonly alBorrar: () => void;
  readonly alCambiarModo: (modo: ModoPropia) => void;
}

/** Lo que se le ofrece a quien mira, según quién sea. */
function TuParte({
  participacion,
  modo,
  enviando,
  fallo,
  alPublicar,
  alEditar,
  alBorrar,
  alCambiarModo,
}: PropsTuParte) {
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
        <TuOpinion
          mia={participacion.mia}
          modo={modo}
          enviando={enviando}
          fallo={fallo}
          alEditar={alEditar}
          alBorrar={alBorrar}
          alCambiarModo={alCambiarModo}
        />
      );

    default:
      return casoImposible(participacion);
  }
}

interface PropsTuOpinion {
  readonly mia: Opinion;
  readonly modo: ModoPropia;
  readonly enviando: boolean;
  readonly fallo: string | null;
  readonly alEditar: (datos: DatosDeOpinion) => void;
  readonly alBorrar: () => void;
  readonly alCambiarModo: (modo: ModoPropia) => void;
}

/** La opinión propia: leerla, cambiarla o borrarla. */
function TuOpinion({
  mia,
  modo,
  enviando,
  fallo,
  alEditar,
  alBorrar,
  alCambiarModo,
}: PropsTuOpinion) {
  const titulo = useRef<HTMLHeadingElement>(null);
  const modoAnterior = useRef<ModoPropia>(modo);

  // El botón que se acaba de pulsar desaparece con el cambio de modo, y con él
  // se iría el foco al principio de la página. Se lleva al título del bloque,
  // que además dice en voz alta qué acaba de aparecer. En el primer dibujo no
  // se toca el foco: ahí nadie ha pulsado nada.
  useEffect(() => {
    if (modoAnterior.current !== modo) {
      modoAnterior.current = modo;
      titulo.current?.focus();
    }
  }, [modo]);

  return (
    <div className={estilos.tuya}>
      <h3 className={estilos.subtitulo} tabIndex={-1} ref={titulo}>
        {modo === 'EDITANDO' ? 'Cambiar tu opinión' : 'Tu opinión'}
      </h3>

      {modo === 'EDITANDO' ? (
        /* El mismo formulario que publica, con los valores puestos. */
        <FormularioOpinion
          inicial={{ calificacion: mia.calificacion, comentario: mia.comentario ?? undefined }}
          enviando={enviando}
          fallo={fallo}
          alEnviar={alEditar}
          alCancelar={() => alCambiarModo('LECTURA')}
        />
      ) : (
        <>
          <ul className={estilos.lista}>
            <OpinionPublicada opinion={mia} />
          </ul>

          {modo === 'CONFIRMANDO_BORRADO' ? (
            <div className={estilos.confirmacion}>
              <p id="aviso-borrado">
                Se borra para siempre y el negocio deja de contarla en su nota.
              </p>
              {fallo !== null && (
                <p className={estilos.falloBorrado} role="alert">
                  {fallo}
                </p>
              )}
              <div className={estilos.acciones}>
                <button
                  type="button"
                  className={estilos.peligro}
                  onClick={alBorrar}
                  disabled={enviando}
                  aria-describedby="aviso-borrado"
                >
                  {enviando ? 'Borrando…' : 'Sí, borrarla'}
                </button>
                {/* Cancelar no borra nada: la deja como estaba. */}
                <button
                  type="button"
                  className={estilos.secundario}
                  onClick={() => alCambiarModo('LECTURA')}
                  disabled={enviando}
                >
                  Cancelar
                </button>
              </div>
            </div>
          ) : (
            <div className={estilos.acciones}>
              <button
                type="button"
                className={estilos.secundario}
                onClick={() => alCambiarModo('EDITANDO')}
              >
                Editar
              </button>
              <button
                type="button"
                className={estilos.secundario}
                onClick={() => alCambiarModo('CONFIRMANDO_BORRADO')}
              >
                Borrar
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
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
