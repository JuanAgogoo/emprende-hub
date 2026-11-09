import { useEffect, useState, type FormEvent } from 'react';
import { listarMisConsultas, marcarConsultaLeida, responderConsulta } from '../api/consultas';
import { EsqueletoLista } from './Esqueleto';
import { Paginacion } from './Paginacion';
import { fecha, numero } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import { MAXIMO_MENSAJE, type Consulta } from '../types/consulta';
import type { Pagina } from '../types/pagina';
import estilos from './BuzonDeConsultas.module.css';

/** Cinco por página: cada consulta trae su mensaje entero, y ocupan. */
const POR_PAGINA = 5;

/**
 * El buzón del negocio propio (D1).
 *
 * Es donde se lee lo que el aviso solo anuncia: la notificación dice quién
 * escribió y sobre qué, y el mensaje está aquí.
 *
 * Cada consulta se **responde desde aquí** (D2) y el cliente lo lee en su chat
 * con el negocio. El correo de quien escribe sigue a la vista, por si hace
 * falta seguir la conversación por fuera.
 */
export function BuzonDeConsultas() {
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Consulta>>>({ estado: 'CARGANDO' });
  const [pagina, setPagina] = useState(0);
  const [intento, setIntento] = useState(0);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    listarMisConsultas(pagina, POR_PAGINA)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        const mensaje =
          error instanceof Error ? error.message : 'No se pudieron cargar las consultas';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [pagina, intento]);

  /** Sustituye la consulta por la que devuelve el backend, sin repedir la página. */
  async function marcar(consulta: Consulta) {
    setFallo(null);
    try {
      const actualizada = await marcarConsultaLeida(consulta.id, !consulta.leida);
      setCarga((previo) => {
        if (previo.estado !== 'EXITO') return previo;
        const content = previo.datos.content.map((c) =>
          c.id === actualizada.id ? actualizada : c,
        );
        return { estado: 'EXITO', datos: { ...previo.datos, content } };
      });
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo marcar la consulta';
      setFallo(mensaje);
    }
  }

  /** Igual que marcar: la consulta respondida sustituye a la anterior. */
  async function responder(consulta: Consulta, texto: string) {
    const actualizada = await responderConsulta(consulta.id, texto);
    setCarga((previo) => {
      if (previo.estado !== 'EXITO') return previo;
      const content = previo.datos.content.map((c) => (c.id === actualizada.id ? actualizada : c));
      return { estado: 'EXITO', datos: { ...previo.datos, content } };
    });
  }

  const sinLeer =
    carga.estado === 'EXITO' ? carga.datos.content.filter((una) => !una.leida).length : 0;

  return (
    <section className={estilos.bloque}>
      <h2 className={estilos.titulo}>
        Buzón
        {sinLeer > 0 && <span className={estilos.pendientes}>{numero(sinLeer)} sin leer</span>}
      </h2>

      {fallo !== null && (
        <p className={estilos.fallo} role="alert">
          {fallo}
        </p>
      )}

      <Listado
        carga={carga}
        alPaginar={setPagina}
        alMarcar={marcar}
        alResponder={responder}
        alReintentar={() => setIntento((valor) => valor + 1)}
      />
    </section>
  );
}

interface PropsListado {
  readonly carga: EstadoCarga<Pagina<Consulta>>;
  readonly alPaginar: (pagina: number) => void;
  readonly alMarcar: (consulta: Consulta) => void;
  readonly alResponder: (consulta: Consulta, texto: string) => Promise<void>;
  readonly alReintentar: () => void;
}

function Listado({ carga, alPaginar, alMarcar, alResponder, alReintentar }: PropsListado) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoLista cuantas={2} />
        </div>
      );

    case 'ERROR':
      return (
        <div className={estilos.error} role="alert">
          <p>No se pudieron cargar las consultas: {carga.mensaje}.</p>
          <button type="button" className={estilos.secundario} onClick={alReintentar}>
            Reintentar
          </button>
        </div>
      );

    case 'EXITO': {
      const pagina = carga.datos;

      if (pagina.content.length === 0) {
        return <p className={estilos.vacio}>Todavía no te ha escrito nadie.</p>;
      }

      return (
        <>
          <ul className={estilos.lista}>
            {pagina.content.map((consulta) => (
              <ConsultaRecibida
                key={consulta.id}
                consulta={consulta}
                alMarcar={alMarcar}
                alResponder={alResponder}
              />
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

interface PropsConsulta {
  readonly consulta: Consulta;
  readonly alMarcar: (consulta: Consulta) => void;
  readonly alResponder: (consulta: Consulta, texto: string) => Promise<void>;
}

function ConsultaRecibida({ consulta, alMarcar, alResponder }: PropsConsulta) {
  const [editando, setEditando] = useState(false);
  const [texto, setTexto] = useState(consulta.respuesta ?? '');
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const abierto = consulta.respuesta === null || editando;

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (texto.trim() === '') {
      setFallo('Escribe la respuesta antes de enviarla');
      return;
    }
    setEnviando(true);
    setFallo(null);
    try {
      await alResponder(consulta, texto.trim());
      setEditando(false);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo enviar la respuesta');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <li className={consulta.leida ? estilos.consulta : `${estilos.consulta} ${estilos.sinLeer}`}>
      <div className={estilos.cabecera}>
        <h3 className={estilos.asunto}>{consulta.asunto}</h3>
        <time className={estilos.fecha} dateTime={consulta.fechaEnvio}>
          {fecha(consulta.fechaEnvio)}
        </time>
      </div>

      <p className={estilos.remitente}>
        {consulta.nombreCliente} <span aria-hidden="true">·</span>{' '}
        <a href={`mailto:${consulta.correoCliente}?subject=${encodeURIComponent(
          `Re: ${consulta.asunto}`,
        )}`}>
          {consulta.correoCliente}
        </a>
      </p>

      <p className={estilos.mensaje}>{consulta.mensaje}</p>

      {consulta.respuesta !== null && !editando && (
        <div className={estilos.respuesta}>
          <p className={estilos.respuestaTitulo}>Tu respuesta</p>
          <p>{consulta.respuesta}</p>
        </div>
      )}

      {abierto ? (
        <form className={estilos.responder} onSubmit={enviar} noValidate>
          <label htmlFor={`respuesta-${consulta.id}`}>Responder a {consulta.nombreCliente}</label>
          <textarea
            id={`respuesta-${consulta.id}`}
            rows={2}
            maxLength={MAXIMO_MENSAJE}
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Escribe tu respuesta…"
          />
          {fallo !== null && (
            <small className={estilos.errorCampo} role="alert">
              {fallo}
            </small>
          )}
          <div className={estilos.acciones}>
            <button type="submit" className={estilos.enviar} disabled={enviando}>
              {enviando ? 'Enviando…' : 'Responder'}
            </button>
            {editando && (
              <button type="button" className={estilos.marcar} onClick={() => setEditando(false)}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      ) : (
        <div className={estilos.acciones}>
          <button type="button" className={estilos.marcar} onClick={() => setEditando(true)}>
            Editar respuesta
          </button>
        </div>
      )}

      <button type="button" className={estilos.marcar} onClick={() => alMarcar(consulta)}>
        {consulta.leida ? 'Marcar como pendiente' : 'Marcar como leída'}
      </button>
    </li>
  );
}
