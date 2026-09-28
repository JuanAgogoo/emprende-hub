import { useEffect, useState } from 'react';
import {
  leerTodasLasNotificaciones,
  listarNotificaciones,
  marcarNotificacionLeida,
} from '../api/notificaciones';
import { EsqueletoLista } from './Esqueleto';
import { Paginacion } from './Paginacion';
import { fecha, numero, tiempoTranscurrido } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { Notificacion, TipoNotificacion } from '../types/notificacion';
import type { Pagina } from '../types/pagina';
import estilos from './Notificaciones.module.css';

/** Diez por página: es una columna dentro de una pantalla que ya es larga. */
const POR_PAGINA = 10;

/** Cómo se enseña cada hecho: su nombre, su señal y su color. */
interface Insignia {
  readonly etiqueta: string;
  /** Decorativa: va en `aria-hidden`, porque la etiqueta ya lo dice. */
  readonly senal: string;
  readonly clase: string;
}

/**
 * La insignia de cada tipo.
 *
 * Es una función con su `switch` y no un mapa de configuración, y es exhaustiva:
 * si mañana el backend añade un tipo, esto deja de compilar en vez de dibujar un
 * aviso sin nombre.
 */
function insignia(tipo: TipoNotificacion): Insignia {
  switch (tipo) {
    case 'OPINION_NUEVA':
      return { etiqueta: 'Nueva opinión', senal: '★', clase: estilos.opinion };
    case 'CONSULTA_NUEVA':
      return { etiqueta: 'Nueva consulta', senal: '✉', clase: estilos.consulta };
    case 'NEGOCIO_APROBADO':
      return { etiqueta: 'Negocio aprobado', senal: '✓', clase: estilos.aprobado };
    case 'NEGOCIO_RECHAZADO':
      return { etiqueta: 'Negocio rechazado', senal: '✕', clase: estilos.rechazado };
  }
}

/**
 * El panel de avisos de quien tiene la sesión abierta (H2).
 *
 * Aquí es donde el emprendedor se entera de que alguien le escribió o le opinó:
 * el buzón no avisa por sí solo. Los avisos cuelgan de la persona, así que esta
 * lista no se pide por negocio.
 */
export function Notificaciones() {
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Notificacion>>>({ estado: 'CARGANDO' });
  const [pagina, setPagina] = useState(0);
  // Cambiarlo vuelve a disparar el efecto: lo usan «Reintentar» y «marcar todas».
  const [intento, setIntento] = useState(0);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    listarNotificaciones(pagina, POR_PAGINA)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        const mensaje = error instanceof Error ? error.message : 'No se pudieron cargar los avisos';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [pagina, intento]);

  /**
   * Sustituye el aviso por el que devuelve el backend, sin volver a pedir la
   * página: es la misma mutación inmutable de siempre, y así la lista no se
   * reordena ni parpadea debajo del cursor.
   */
  async function marcar(aviso: Notificacion) {
    setFallo(null);
    try {
      const actualizado = await marcarNotificacionLeida(aviso.id, !aviso.leida);
      setCarga((previo) => {
        if (previo.estado !== 'EXITO') return previo;
        const content = previo.datos.content.map((n) =>
          n.id === actualizado.id ? actualizado : n,
        );
        return { estado: 'EXITO', datos: { ...previo.datos, content } };
      });
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo marcar el aviso';
      setFallo(mensaje);
    }
  }

  async function leerTodas() {
    setFallo(null);
    try {
      await leerTodasLasNotificaciones();
      // Cambian muchos a la vez: aquí sí compensa volver a pedir la página.
      setIntento((valor) => valor + 1);
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudieron marcar los avisos';
      setFallo(mensaje);
    }
  }

  const sinLeer =
    carga.estado === 'EXITO' ? carga.datos.content.filter((aviso) => !aviso.leida).length : 0;

  return (
    <section className={estilos.bloque}>
      <h2 className={estilos.titulo}>
        Avisos
        {sinLeer > 0 && <span className={estilos.pendientes}>{numero(sinLeer)} sin leer</span>}
      </h2>

      {fallo !== null && (
        <p className={estilos.fallo} role="alert">
          {fallo}
        </p>
      )}

      {sinLeer > 0 && (
        <button type="button" className={estilos.secundario} onClick={leerTodas}>
          Marcar todos como leídos
        </button>
      )}

      <Listado
        carga={carga}
        alPaginar={setPagina}
        alMarcar={marcar}
        alReintentar={() => setIntento((valor) => valor + 1)}
      />
    </section>
  );
}

interface PropsListado {
  readonly carga: EstadoCarga<Pagina<Notificacion>>;
  readonly alPaginar: (pagina: number) => void;
  readonly alMarcar: (aviso: Notificacion) => void;
  readonly alReintentar: () => void;
}

function Listado({ carga, alPaginar, alMarcar, alReintentar }: PropsListado) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoLista cuantas={3} />
        </div>
      );

    case 'ERROR':
      return (
        <div className={estilos.error} role="alert">
          <p>No se pudieron cargar los avisos: {carga.mensaje}.</p>
          <button type="button" className={estilos.secundario} onClick={alReintentar}>
            Reintentar
          </button>
        </div>
      );

    case 'EXITO': {
      const pagina = carga.datos;

      if (pagina.content.length === 0) {
        return <p className={estilos.vacio}>Todavía no tienes avisos.</p>;
      }

      return (
        <>
          <ul className={estilos.lista}>
            {pagina.content.map((aviso) => (
              <AvisoRecibido key={aviso.id} aviso={aviso} alMarcar={alMarcar} />
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

interface PropsAviso {
  readonly aviso: Notificacion;
  readonly alMarcar: (aviso: Notificacion) => void;
}

function AvisoRecibido({ aviso, alMarcar }: PropsAviso) {
  const { etiqueta, senal, clase } = insignia(aviso.tipo);

  return (
    <li className={aviso.leida ? estilos.aviso : `${estilos.aviso} ${estilos.sinLeer}`}>
      <div className={estilos.cabecera}>
        <span className={`${estilos.tipo} ${clase}`}>
          <span aria-hidden="true">{senal}</span> {etiqueta}
        </span>

        {/* Lo que se lee es cuánto hace; la fecha exacta queda a un palmo, en el
            título, para quien la necesite. */}
        <time className={estilos.fecha} dateTime={aviso.fecha} title={fecha(aviso.fecha)}>
          {tiempoTranscurrido(aviso.fecha)}
        </time>
      </div>

      <p className={estilos.texto}>{aviso.texto}</p>

      <button type="button" className={estilos.marcar} onClick={() => alMarcar(aviso)}>
        {aviso.leida ? 'Marcar como pendiente' : 'Marcar como leído'}
      </button>
    </li>
  );
}
