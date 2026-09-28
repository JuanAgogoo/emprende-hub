import { useEffect, useState } from 'react';
import { listarMisConsultas, marcarConsultaLeida } from '../api/consultas';
import { EsqueletoLista } from './Esqueleto';
import { Paginacion } from './Paginacion';
import { fecha, numero } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { Consulta } from '../types/consulta';
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
 * **Trae el correo de quien escribe**, que es la única dirección que la API
 * enseña de otra persona (D2). Sin ella el buzón sería un montón de preguntas
 * sin forma de contestarlas, porque la plataforma no responde desde dentro: por
 * eso cada consulta lleva su enlace para responder por correo.
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
        alReintentar={() => setIntento((valor) => valor + 1)}
      />
    </section>
  );
}

interface PropsListado {
  readonly carga: EstadoCarga<Pagina<Consulta>>;
  readonly alPaginar: (pagina: number) => void;
  readonly alMarcar: (consulta: Consulta) => void;
  readonly alReintentar: () => void;
}

function Listado({ carga, alPaginar, alMarcar, alReintentar }: PropsListado) {
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
              <ConsultaRecibida key={consulta.id} consulta={consulta} alMarcar={alMarcar} />
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
}

function ConsultaRecibida({ consulta, alMarcar }: PropsConsulta) {
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

      <button type="button" className={estilos.marcar} onClick={() => alMarcar(consulta)}>
        {consulta.leida ? 'Marcar como pendiente' : 'Marcar como leída'}
      </button>
    </li>
  );
}
