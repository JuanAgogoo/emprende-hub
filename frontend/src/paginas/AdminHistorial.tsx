import { useEffect, useState } from 'react';
import { consultarHistorial } from '../api/moderacion';
import { EsqueletoLista } from '../componentes/Esqueleto';
import { Paginacion } from '../componentes/Paginacion';
import { fechaYHora } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { RegistroModeracion, TipoEventoModeracion } from '../types/moderacion';
import type { Pagina } from '../types/pagina';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

interface Rango {
  /** Tal como lo da un `<input type="date">`: `YYYY-MM-DD`, o vacío. */
  readonly desde: string;
  readonly hasta: string;
}

/**
 * Un día del calendario como instante, en la zona de quien mira.
 *
 * **No vale `new Date('2026-10-06')`**: eso es medianoche UTC, que en Colombia
 * es el día anterior a las siete de la tarde, y el filtro se comería las
 * últimas horas del día. Construido por partes, el día es el de la pantalla.
 * El final del rango es el último milisegundo de ese día, porque el backend
 * incluye los extremos.
 */
function instante(dia: string, finDelDia: boolean): string | undefined {
  if (dia === '') return undefined;
  const [anio, mes, numero] = dia.split('-').map(Number);
  const fecha = finDelDia
    ? new Date(anio, mes - 1, numero, 23, 59, 59, 999)
    : new Date(anio, mes - 1, numero);
  return fecha.toISOString();
}

function validar(rango: Rango): string | null {
  // Las cadenas `YYYY-MM-DD` se ordenan igual que las fechas.
  if (rango.desde !== '' && rango.hasta !== '' && rango.desde > rango.hasta) {
    return '«Desde» no puede ser posterior a «Hasta».';
  }
  return null;
}

/**
 * El color de cada acción. El `switch` es exhaustivo: un tipo nuevo en el
 * backend, añadido a la unión, no compila hasta decidir cómo se pinta.
 */
function tonoDe(tipo: TipoEventoModeracion): 'exito' | 'error' | 'neutro' {
  switch (tipo) {
    case 'NEGOCIO_APROBADO':
    case 'CAMBIO_APROBADO':
    case 'CUENTA_REACTIVADA':
    case 'CURSO_PUBLICADO':
      return 'exito';
    case 'NEGOCIO_RECHAZADO':
    case 'CAMBIO_RECHAZADO':
    case 'CUENTA_SUSPENDIDA':
    case 'OPINION_ELIMINADA':
      return 'error';
    case 'DENUNCIA_DESESTIMADA':
      return 'neutro';
  }
}

const CLASE_DEL_TONO = {
  exito: estilos.insigniaExito,
  error: estilos.insigniaError,
  neutro: estilos.insigniaNeutra,
} as const;

/**
 * El historial de todas las acciones de moderación (HU-041): qué se hizo, sobre
 * quién, quién lo hizo y cuándo. Solo se lee: no hay nada que editar ni borrar,
 * y el backend tampoco lo permitiría.
 */
export function AdminHistorial() {
  useTitulo('Historial de moderación');
  const [borrador, setBorrador] = useState<Rango>({ desde: '', hasta: '' });
  const [aplicado, setAplicado] = useState<Rango>({ desde: '', hasta: '' });
  const [pagina, setPagina] = useState(0);
  const [carga, setCarga] = useState<EstadoCarga<Pagina<RegistroModeracion>>>({
    estado: 'CARGANDO',
  });

  const error = validar(borrador);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });
    consultarHistorial(instante(aplicado.desde, false), instante(aplicado.hasta, true), pagina)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((causa: unknown) => {
        const mensaje = causa instanceof Error ? causa.message : 'No se pudo cargar el historial';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });
    return () => {
      vigente = false;
    };
  }, [aplicado, pagina]);

  /** Cada fecha se aplica al cambiarla, si el rango tiene sentido. */
  function cambiar(campo: keyof Rango, valor: string) {
    const siguiente = { ...borrador, [campo]: valor };
    setBorrador(siguiente);
    if (validar(siguiente) === null) {
      setAplicado(siguiente);
      setPagina(0);
    }
  }

  function limpiar() {
    const vacio = { desde: '', hasta: '' };
    setBorrador(vacio);
    setAplicado(vacio);
    setPagina(0);
  }

  const hayFiltro = aplicado.desde !== '' || aplicado.hasta !== '';

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-historial">
      <h2 id="titulo-historial" className={estilos.tituloSeccion}>
        Historial de moderación
      </h2>
      <p className={estilos.entrada}>
        Cada decisión del panel queda aquí con quién la tomó y cuándo. Es de solo lectura: no se
        edita ni se borra.
      </p>

      <div className={estilos.filtroFechas}>
        <div className={estilos.campoFecha}>
          <label htmlFor="desde">Desde</label>
          <input
            id="desde"
            type="date"
            value={borrador.desde}
            max={borrador.hasta === '' ? undefined : borrador.hasta}
            onChange={(e) => cambiar('desde', e.target.value)}
            aria-invalid={error !== null}
            aria-describedby={error !== null ? 'error-rango' : undefined}
          />
        </div>
        <div className={estilos.campoFecha}>
          <label htmlFor="hasta">Hasta</label>
          <input
            id="hasta"
            type="date"
            value={borrador.hasta}
            min={borrador.desde === '' ? undefined : borrador.desde}
            onChange={(e) => cambiar('hasta', e.target.value)}
            aria-invalid={error !== null}
            aria-describedby={error !== null ? 'error-rango' : undefined}
          />
        </div>
        {(hayFiltro || error !== null) && (
          <button type="button" className={estilos.secundario} onClick={limpiar}>
            Quitar el filtro
          </button>
        )}
      </div>
      {error !== null && (
        <p id="error-rango" className={estilos.error} role="alert">
          {error}
        </p>
      )}

      <Registros carga={carga} hayFiltro={hayFiltro} alPaginar={setPagina} />
    </section>
  );
}

interface PropsRegistros {
  readonly carga: EstadoCarga<Pagina<RegistroModeracion>>;
  readonly hayFiltro: boolean;
  readonly alPaginar: (pagina: number) => void;
}

function Registros({ carga, hayFiltro, alPaginar }: PropsRegistros) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoLista cuantas={4} />
        </div>
      );

    case 'ERROR':
      return (
        <p className={estilos.error} role="alert">
          No se pudo cargar el historial: {carga.mensaje}. Comprueba que la API esté funcionando
          y vuelve a intentarlo.
        </p>
      );

    case 'EXITO': {
      const { content: registros, totalElements: total } = carga.datos;

      if (total === 0) {
        return (
          <div className={estilos.vacio}>
            <p className={estilos.tituloVacio}>
              {hayFiltro ? 'Nada en esas fechas' : 'Todavía no hay acciones registradas'}
            </p>
            {hayFiltro && <p>Prueba con un rango más amplio.</p>}
          </div>
        );
      }

      return (
        <>
          <p className={estilos.recuento}>
            {total === 1 ? '1 acción registrada' : `${total} acciones registradas`}
          </p>
          <ol className={estilos.lista}>
            {registros.map((registro) => (
              <li key={registro.id} className={estilos.registro}>
                <div className={estilos.registroCabecera}>
                  <span className={`${estilos.insignia} ${CLASE_DEL_TONO[tonoDe(registro.tipo)]}`}>
                    {registro.descripcionTipo}
                  </span>
                  <time className={estilos.apagado} dateTime={registro.fecha}>
                    {fechaYHora(registro.fecha)}
                  </time>
                </div>
                <dl className={estilos.datos}>
                  <div>
                    <dt>Sobre</dt>
                    <dd className={estilos.correo}>{registro.afectado}</dd>
                  </div>
                  <div>
                    <dt>Por</dt>
                    <dd className={estilos.correo}>{registro.administrador}</dd>
                  </div>
                </dl>
                {registro.detalle !== null && (
                  <p className={estilos.detalleRegistro}>{registro.detalle}</p>
                )}
              </li>
            ))}
          </ol>
          <Paginacion
            pagina={carga.datos.number}
            totalPaginas={carga.datos.totalPages}
            alCambiar={alPaginar}
          />
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}
