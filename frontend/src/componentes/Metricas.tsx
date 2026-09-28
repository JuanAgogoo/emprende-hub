import { useEffect, useState, type ReactNode } from 'react';
import { listarMisConsultas } from '../api/consultas';
import { obtenerMetricasDeVisitas } from '../api/metricas';
import { GraficaDeVisitas } from './GraficaDeVisitas';
import { calificacion, numero, variacion } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { MetricasVisitas, PeriodoMetrica } from '../types/metricas';
import type { MiNegocio } from '../types/negocio';
import estilos from './Metricas.module.css';

interface Props {
  /** Lo trae la página, que ya lo pidió: la nota y el recuento salen de aquí. */
  readonly negocio: MiNegocio;
}

/** Lo que hace falta para las cuatro tarjetas, y llega por dos peticiones. */
interface Datos {
  readonly visitas: MetricasVisitas;
  readonly consultasRecibidas: number;
}

/**
 * Las cifras del negocio propio, para medir el impacto de estar publicado.
 *
 * Las visitas traen su comparación con el periodo anterior, que es lo que
 * convierte un número suelto en una medida (H1). **Las consultas y la
 * calificación no la traen**: la API no guarda su histórico por periodos, y
 * calcularla aquí obligaría a recorrer el buzón entero página por página para
 * contar por fechas, que es meter una regla de negocio en la vista.
 */
export function Metricas({ negocio }: Props) {
  const [carga, setCarga] = useState<EstadoCarga<Datos>>({ estado: 'CARGANDO' });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });

    // El buzón se pide de una en una: aquí solo interesa cuántas son en total.
    Promise.all([obtenerMetricasDeVisitas(), listarMisConsultas(0, 1)])
      .then(([visitas, buzon]) => {
        if (vigente) {
          setCarga({
            estado: 'EXITO',
            datos: { visitas, consultasRecibidas: buzon.totalElements },
          });
        }
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        const mensaje = error instanceof Error ? error.message : 'No se pudieron cargar las cifras';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [intento]);

  return (
    <section className={estilos.bloque}>
      <h2 className={estilos.titulo}>Tus cifras</h2>
      <Tarjetas
        carga={carga}
        negocio={negocio}
        alReintentar={() => setIntento((valor) => valor + 1)}
      />

      {/* La serie ya vino con las cifras: la gráfica no pide nada más. */}
      {carga.estado === 'EXITO' && <GraficaDeVisitas serie={carga.datos.visitas.serie} />}
    </section>
  );
}

interface PropsTarjetas {
  readonly carga: EstadoCarga<Datos>;
  readonly negocio: MiNegocio;
  readonly alReintentar: () => void;
}

function Tarjetas({ carga, negocio, alReintentar }: PropsTarjetas) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div className={estilos.rejilla} aria-busy="true">
          {/* Cuatro huecos con la forma de las tarjetas que vienen. Como en el
              resto de esqueletos, la clave es la posición: son huecos idénticos
              y sin identidad, que ni se reordenan ni se filtran. */}
          {[0, 1, 2, 3].map((posicion) => (
            <div key={posicion} className={estilos.esqueleto} />
          ))}
        </div>
      );

    case 'ERROR':
      return (
        <div className={estilos.error} role="alert">
          <p>No se pudieron cargar las cifras: {carga.mensaje}.</p>
          <button type="button" className={estilos.secundario} onClick={alReintentar}>
            Reintentar
          </button>
        </div>
      );

    case 'EXITO': {
      const { visitas, consultasRecibidas } = carga.datos;
      const nota = calificacion(negocio.calificacionPromedio);

      return (
        <div className={estilos.rejilla}>
          <Tarjeta titulo="Visitas de la semana" cifra={numero(visitas.semana.actual)}>
            <Comparacion periodo={visitas.semana} anterior="la semana pasada" />
          </Tarjeta>

          <Tarjeta titulo="Visitas del mes" cifra={numero(visitas.mes.actual)}>
            <Comparacion periodo={visitas.mes} anterior="el mes pasado" />
          </Tarjeta>

          <Tarjeta titulo="Consultas recibidas" cifra={numero(consultasRecibidas)}>
            <span className={estilos.apunte}>Desde que publicaste</span>
          </Tarjeta>

          {/* Sin opiniones el promedio es nulo, y eso no es un cero (C5). */}
          <Tarjeta titulo="Calificación promedio" cifra={nota ?? '—'}>
            <span className={estilos.apunte}>
              {nota === null
                ? 'Sin opiniones todavía'
                : `Sobre ${numero(negocio.numeroOpiniones)} ${
                    negocio.numeroOpiniones === 1 ? 'opinión' : 'opiniones'
                  }`}
            </span>
          </Tarjeta>
        </div>
      );
    }

    default:
      return casoImposible(carga);
  }
}

interface PropsTarjeta {
  readonly titulo: string;
  readonly cifra: string;
  readonly children: ReactNode;
}

function Tarjeta({ titulo, cifra, children }: PropsTarjeta) {
  return (
    <article className={estilos.tarjeta}>
      <h3 className={estilos.tituloTarjeta}>{titulo}</h3>
      <p className={estilos.cifra}>{cifra}</p>
      {children}
    </article>
  );
}

interface PropsComparacion {
  readonly periodo: PeriodoMetrica;
  /** Con qué se compara, en palabras: «la semana pasada», «el mes pasado». */
  readonly anterior: string;
}

/**
 * La comparación con el periodo anterior.
 *
 * La flecha va en `aria-hidden` y el sentido se dice también con palabras: el
 * color y el dibujo solos no sirven para quien no los distingue.
 */
function Comparacion({ periodo, anterior }: PropsComparacion) {
  const porcentaje = periodo.variacionPorcentual;

  // Nula cuando el periodo anterior fue cero: no es un aumento infinito, es que
  // no había con qué comparar (H1).
  if (porcentaje === null) {
    return (
      <span className={estilos.apunte}>Sin visitas en {anterior}, no hay con qué comparar</span>
    );
  }

  // Cero sí es un dato: no cambió nada. Por eso se compara con `null` arriba y
  // no con `||`, que se habría comido este caso.
  if (porcentaje === 0) {
    return <span className={estilos.apunte}>Igual que {anterior}</span>;
  }

  const sube = porcentaje > 0;

  return (
    <span className={sube ? estilos.sube : estilos.baja}>
      <span aria-hidden="true">{sube ? '▲' : '▼'}</span> {variacion(porcentaje)}{' '}
      <span className={estilos.apunte}>
        {sube ? 'más que' : 'menos que'} {anterior}
      </span>
    </span>
  );
}
