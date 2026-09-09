import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { obtenerMiNegocio, obtenerMisFotos, obtenerMisProductos } from '../api/negocios';
import { calificacion, nivelPrecio, numero, precio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { EstadoNegocio, FotoNegocio, MiNegocio as Negocio, Producto } from '../types/negocio';
import estilos from './MiNegocio.module.css';

interface Datos {
  readonly negocio: Negocio;
  readonly fotos: readonly FotoNegocio[];
  readonly productos: readonly Producto[];
}

/** Qué significa cada estado para quien es dueño del negocio. */
function explicar(estado: EstadoNegocio): { readonly titulo: string; readonly texto: string } {
  switch (estado) {
    case 'PENDIENTE':
      return {
        titulo: 'En revisión',
        texto:
          'Tu negocio todavía no aparece en el directorio. Alguien lo está revisando y en cuanto lo apruebe se publicará.',
      };
    case 'APROBADO':
      return {
        titulo: 'Publicado',
        texto: 'Tu negocio aparece en el directorio y cualquiera puede encontrarlo.',
      };
    case 'RECHAZADO':
      return {
        titulo: 'Rechazado',
        texto: 'Tu negocio no se ha publicado. Puedes corregir lo que se indica y volver a enviarlo.',
      };
  }
}

export function MiNegocio() {
  const [carga, setCarga] = useState<EstadoCarga<Datos>>({ estado: 'CARGANDO' });
  // Tener sesión de emprendedor y no tener negocio es un estado posible, no un
  // fallo: pasa entre crear la cuenta y registrar el negocio.
  const [sinNegocio, setSinNegocio] = useState(false);

  useEffect(() => {
    let vigente = true;

    Promise.all([obtenerMiNegocio(), obtenerMisFotos(), obtenerMisProductos()])
      .then(([negocio, fotos, productos]) => {
        if (vigente) setCarga({ estado: 'EXITO', datos: { negocio, fotos, productos } });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        if (error instanceof ErrorApi && error.estado === 404) {
          setSinNegocio(true);
          return;
        }
        const mensaje = error instanceof Error ? error.message : 'No se pudo cargar tu negocio';
        setCarga({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, []);

  if (sinNegocio) {
    return (
      <div className={`contenedor ${estilos.pagina}`}>
        <h1>Todavía no tienes un negocio</h1>
        <p className={estilos.entrada}>
          Cuando registres uno, aquí verás cómo va y en qué estado está.
        </p>
      </div>
    );
  }

  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div className={`contenedor ${estilos.pagina}`} aria-busy="true">
          <div className={estilos.esqueleto} />
        </div>
      );

    case 'ERROR':
      return (
        <div className={`contenedor ${estilos.pagina}`}>
          <p className={estilos.error} role="alert">
            No se pudo cargar tu negocio: {carga.mensaje}. Comprueba que la API esté funcionando y
            vuelve a intentarlo.
          </p>
        </div>
      );

    case 'EXITO':
      return <Contenido datos={carga.datos} />;

    default:
      return casoImposible(carga);
  }
}

function Contenido({ datos }: { readonly datos: Datos }) {
  const { negocio, fotos, productos } = datos;
  const estado = explicar(negocio.estado);
  const nota = calificacion(negocio.calificacionPromedio);
  const ubicacion = negocio.barrio === null ? negocio.ciudad : `${negocio.barrio}, ${negocio.ciudad}`;
  const sinRevisar = fotos.filter((foto) => foto.estado !== 'APROBADA').length;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.encabezado}>
        <h1>{negocio.nombre}</h1>
        <p className={estilos.meta}>
          {negocio.categoria} <span aria-hidden="true">·</span> {ubicacion}{' '}
          <span aria-hidden="true">·</span> {nivelPrecio(negocio.nivelPrecio)}
        </p>
      </header>

      {/* El estado no se marca solo con un color: lleva su título y su texto. */}
      <section className={`${estilos.estado} ${estilos[negocio.estado.toLowerCase()]}`}>
        <h2 className={estilos.tituloEstado}>{estado.titulo}</h2>
        <p>{estado.texto}</p>

        {negocio.estado === 'RECHAZADO' && negocio.motivoRechazo !== null && (
          <p className={estilos.motivo}>
            <strong>Motivo:</strong> {negocio.motivoRechazo}
          </p>
        )}

        {negocio.estado === 'APROBADO' && (
          <p>
            <Link to={`/negocios/${negocio.id}`}>Ver cómo lo ve el público</Link>
          </p>
        )}
      </section>

      <div className={estilos.columnas}>
        <section className={estilos.bloque}>
          <h2 className={estilos.tituloBloque}>Datos</h2>
          <dl className={estilos.datos}>
            <dt>Descripción</dt>
            <dd>{negocio.descripcion}</dd>
            <dt>Teléfono</dt>
            <dd>{negocio.telefono}</dd>
            <dt>Instagram</dt>
            <dd>{negocio.instagram ?? 'Sin enlace'}</dd>
            <dt>LinkedIn</dt>
            <dd>{negocio.linkedin ?? 'Sin enlace'}</dd>
            <dt>Calificación</dt>
            <dd>
              {nota === null
                ? 'Sin opiniones todavía'
                : `${nota} · ${numero(negocio.numeroOpiniones)} ${
                    negocio.numeroOpiniones === 1 ? 'opinión' : 'opiniones'
                  }`}
            </dd>
          </dl>
        </section>

        <section className={estilos.bloque}>
          <h2 className={estilos.tituloBloque}>
            Fotos
            {sinRevisar > 0 && (
              <span className={estilos.pendientes}>
                {numero(sinRevisar)} sin revisar
              </span>
            )}
          </h2>

          {fotos.length === 0 ? (
            <p className={estilos.vacio}>Todavía no has subido ninguna foto.</p>
          ) : (
            <ul className={estilos.galeria}>
              {fotos.map((foto) => (
                <li key={foto.id} className={estilos.miniatura}>
                  <img src={foto.url} alt="" loading="lazy" />
                  {/* Solo el dueño ve este estado; el público únicamente las aprobadas. */}
                  {foto.estado !== 'APROBADA' && (
                    <span className={estilos.marcaFoto}>Sin revisar</span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className={estilos.bloque}>
          <h2 className={estilos.tituloBloque}>Escaparate</h2>
          {productos.length === 0 ? (
            <p className={estilos.vacio}>Todavía no has añadido productos.</p>
          ) : (
            <ul className={estilos.productos}>
              {productos.map((producto) => (
                <li key={producto.id} className={estilos.producto}>
                  <span className={estilos.nombreProducto}>{producto.nombre}</span>
                  <span className={estilos.precio}>{precio(producto.precio)}</span>
                  {!producto.disponible && (
                    <span className={estilos.agotado}>No disponible</span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <p className={estilos.nota}>
        Esta pantalla es solo de consulta. Editar el negocio, responder consultas y ver las visitas
        llegan más adelante.
      </p>
    </div>
  );
}
