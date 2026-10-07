import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { aprobarNegocio, obtenerVistaPrevia, rechazarNegocio } from '../api/moderacion';
import { DialogoMotivo } from '../componentes/DialogoMotivo';
import { Galeria } from '../componentes/Galeria';
import { localidad, nivelPrecio, precio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { VistaPreviaNegocio } from '../types/moderacion';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

const COLA = '/admin/negocios';

/**
 * Revisar un negocio antes de publicarlo: verlo como lo verá todo el mundo y
 * decidir (HU-036, HU-037).
 *
 * Las dos decisiones viven solo aquí y no en la cola, a propósito: así no se
 * puede aprobar un negocio sin haber pasado por su vista previa.
 */
export function AdminRevisarNegocio() {
  const { id } = useParams();
  const [carga, setCarga] = useState<EstadoCarga<VistaPreviaNegocio>>({ estado: 'CARGANDO' });
  const [noExiste, setNoExiste] = useState(false);

  useEffect(() => {
    let vigente = true;
    const identificador = Number(id);

    if (!Number.isInteger(identificador) || identificador <= 0) {
      setNoExiste(true);
      return;
    }

    setCarga({ estado: 'CARGANDO' });
    obtenerVistaPrevia(identificador)
      .then((negocio) => {
        if (vigente) setCarga({ estado: 'EXITO', datos: negocio });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        if (error instanceof ErrorApi && error.estado === 404) {
          setNoExiste(true);
          return;
        }
        const mensaje = error instanceof Error ? error.message : 'No se pudo cargar el negocio';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [id]);

  useTitulo(carga.estado === 'EXITO' ? `Revisar ${carga.datos.nombre}` : 'Revisar negocio');

  if (noExiste) {
    return (
      <div className={estilos.vacio}>
        <p className={estilos.tituloVacio}>Ese negocio no existe</p>
        <Link to={COLA}>Volver a la cola</Link>
      </div>
    );
  }

  switch (carga.estado) {
    case 'CARGANDO':
      return <div className={estilos.esqueletoRevision} aria-busy="true" />;

    case 'ERROR':
      return (
        <p className={estilos.error} role="alert">
          No se pudo cargar el negocio: {carga.mensaje}. Comprueba que la API esté funcionando y
          vuelve a intentarlo.
        </p>
      );

    case 'EXITO':
      return <Revision negocio={carga.datos} />;

    default:
      return casoImposible(carga);
  }
}

function Revision({ negocio }: { readonly negocio: VistaPreviaNegocio }) {
  const navegar = useNavigate();
  const [ocupado, setOcupado] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [rechazando, setRechazando] = useState(false);

  const fotosPendientes = negocio.fotos.filter((foto) => foto.estado === 'PENDIENTE').length;

  /**
   * El backend ya explica el fallo en español; un 400 aquí suele ser que el
   * negocio se resolvió en otra pestaña, y su mensaje lo dice así.
   */
  function mensajeDe(error: unknown): string {
    return error instanceof Error ? error.message : 'No se pudo guardar la decisión';
  }

  async function aprobar() {
    setOcupado(true);
    setFallo(null);
    try {
      await aprobarNegocio(negocio.id);
      navegar(COLA, {
        state: {
          aviso: `«${negocio.nombre}» ya está publicado. Se avisó a su dueño en su panel y por correo.`,
        },
      });
    } catch (error: unknown) {
      setFallo(mensajeDe(error));
      setOcupado(false);
    }
  }

  async function rechazar(motivo: string) {
    setOcupado(true);
    setFallo(null);
    try {
      await rechazarNegocio(negocio.id, motivo);
      navegar(COLA, {
        state: {
          aviso: `«${negocio.nombre}» quedó rechazado. Su dueño recibió el motivo en su panel y por correo.`,
        },
      });
    } catch (error: unknown) {
      setFallo(mensajeDe(error));
      setOcupado(false);
    }
  }

  return (
    <article className={estilos.revision} aria-labelledby="titulo-revision">
      <p className={estilos.migas}>
        <Link to={COLA}>Negocios por revisar</Link> <span aria-hidden="true">/</span>{' '}
        <span>{negocio.nombre}</span>
      </p>

      <p className={estilos.nota}>
        Vista previa: así se verá su perfil en el directorio cuando lo apruebes. Nadie más puede
        verlo todavía.
      </p>

      <div className={estilos.columnas}>
        <div className={estilos.principal}>
          <Galeria
            fotos={negocio.fotos}
            nombreNegocio={negocio.nombre}
            nombreTransicion={`revision-${negocio.id}`}
          />
          {fotosPendientes > 0 && (
            <p className={estilos.apagado}>
              {fotosPendientes === 1
                ? 'Esta foto espera revisión: se publicará al aprobar.'
                : `Estas ${fotosPendientes} fotos esperan revisión: se publicarán al aprobar.`}
            </p>
          )}

          <section className={estilos.bloque}>
            <h3 className={estilos.tituloBloque}>Sobre el negocio</h3>
            <p className={estilos.descripcion}>{negocio.descripcion}</p>
          </section>

          <section className={estilos.bloque}>
            <h3 className={estilos.tituloBloque}>Escaparate</h3>
            {negocio.productos.length === 0 ? (
              <p className={estilos.apagado}>Todavía no ha subido productos.</p>
            ) : (
              <ul className={estilos.productos}>
                {negocio.productos.map((producto) => (
                  <li key={producto.id} className={estilos.producto}>
                    <img src={producto.foto} alt="" loading="lazy" />
                    <span className={estilos.nombreProducto}>{producto.nombre}</span>
                    <span>{precio(producto.precio)}</span>
                    {!producto.disponible && (
                      <span className={estilos.apagado}>No disponible</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <aside className={estilos.ficha}>
          <h2 id="titulo-revision" className={estilos.nombreFicha}>
            {negocio.nombre}
          </h2>

          <dl className={estilos.datosFicha}>
            <dt>Categoría</dt>
            <dd>{negocio.categoria}</dd>
            <dt>Localidad</dt>
            <dd>{localidad(negocio)}</dd>
            <dt>Teléfono</dt>
            <dd>{negocio.telefono}</dd>
            <dt>Nivel de precio</dt>
            <dd>{nivelPrecio(negocio.nivelPrecio)}</dd>
            {negocio.instagram !== null && (
              <>
                <dt>Instagram</dt>
                <dd>
                  <a href={negocio.instagram} target="_blank" rel="noopener noreferrer">
                    {negocio.instagram}
                  </a>
                </dd>
              </>
            )}
            {negocio.linkedin !== null && (
              <>
                <dt>LinkedIn</dt>
                <dd>
                  <a href={negocio.linkedin} target="_blank" rel="noopener noreferrer">
                    {negocio.linkedin}
                  </a>
                </dd>
              </>
            )}
          </dl>

          {fallo !== null && !rechazando && (
            <p className={estilos.error} role="alert">
              {fallo}
            </p>
          )}

          {/* Diferenciados por color, por icono y por texto: con cualquiera de
              los tres se sabe cuál es cuál (HU-037). */}
          <div className={estilos.decision}>
            <button
              type="button"
              className={estilos.aprobar}
              disabled={ocupado}
              onClick={aprobar}
            >
              <span aria-hidden="true">✓</span> {ocupado && !rechazando ? 'Aprobando…' : 'Aprobar y publicar'}
            </button>
            <button
              type="button"
              className={estilos.rechazar}
              disabled={ocupado}
              onClick={() => {
                setFallo(null);
                setRechazando(true);
              }}
            >
              <span aria-hidden="true">✕</span> Rechazar
            </button>
          </div>
        </aside>
      </div>

      {rechazando && (
        <DialogoMotivo
          titulo={`Rechazar «${negocio.nombre}»`}
          texto="Su dueño leerá este motivo en su panel y en un correo, y desde ahí corregirá y volverá a enviarlo."
          etiquetaConfirmar="Rechazar"
          ocupado={ocupado}
          fallo={fallo}
          alConfirmar={rechazar}
          alCancelar={() => setRechazando(false)}
        />
      )}
    </article>
  );
}
