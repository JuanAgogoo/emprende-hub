import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { BuzonDeConsultas } from '../componentes/BuzonDeConsultas';
import { CargaDeFotos } from '../componentes/CargaDeFotos';
import { EditarNegocio } from '../componentes/EditarNegocio';
import { EscaparateEditable } from '../componentes/EscaparateEditable';
import { Metricas } from '../componentes/Metricas';
import { Notificaciones } from '../componentes/Notificaciones';
import { obtenerMiNegocio, obtenerMisFotos, obtenerMisProductos } from '../api/negocios';
import { nivelPrecio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { EstadoNegocio, FotoNegocio, MiNegocio as Negocio, Producto } from '../types/negocio';
import estilos from './MiNegocio.module.css';
import { useTitulo } from '../titulo';

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
  useTitulo('Mi negocio');
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

  /**
   * Recoge el negocio que devolvió una edición, sin volver a pedirlo.
   *
   * La respuesta del backend ya trae cómo quedó —incluida la propuesta en cola
   * si el cambio espera revisión—, así que pedirlo otra vez sería una vuelta de
   * más para saber lo que ya se sabe.
   */
  function actualizarNegocio(negocio: Negocio) {
    setCarga((actual) =>
      actual.estado === 'EXITO'
        ? { estado: 'EXITO', datos: { ...actual.datos, negocio } }
        : actual,
    );
  }

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
      return <Contenido datos={carga.datos} alActualizarNegocio={actualizarNegocio} />;

    default:
      return casoImposible(carga);
  }
}

interface PropsContenido {
  readonly datos: Datos;
  readonly alActualizarNegocio: (negocio: Negocio) => void;
}

function Contenido({ datos, alActualizarNegocio }: PropsContenido) {
  const { negocio, fotos, productos } = datos;
  const estado = explicar(negocio.estado);
  const ubicacion =
    negocio.barrio === null ? negocio.ciudad : `${negocio.barrio}, ${negocio.ciudad}`;
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

      {/* Lo que está esperando al administrador, con lo propuesto a la vista:
          sin esto, editar y recargar parecería que el cambio se perdió. */}
      {negocio.cambioPendiente !== null && (
        <section className={estilos.propuesta}>
          <h2 className={estilos.tituloEstado}>Tienes un cambio esperando revisión</h2>
          <p>
            Tu negocio sigue publicado con los datos de arriba. Esto es lo que se publicará en
            cuanto lo aprueben:
          </p>
          <dl className={estilos.datos}>
            <dt>Nombre</dt>
            <dd>{negocio.cambioPendiente.nombre}</dd>
            <dt>Categoría</dt>
            <dd>{negocio.cambioPendiente.categoria ?? 'No cambia'}</dd>
            <dt>Descripción</dt>
            <dd>{negocio.cambioPendiente.descripcion}</dd>
          </dl>
        </section>
      )}

      <section className={estilos.bloque}>
        <h2 className={estilos.tituloBloque}>Datos del negocio</h2>
        <EditarNegocio negocio={negocio} alActualizar={alActualizarNegocio} />
      </section>

      <section className={estilos.bloque}>
        <h2 className={estilos.tituloBloque}>
          Fotos
          {sinRevisar > 0 && <span className={estilos.pendientes}>{sinRevisar} sin revisar</span>}
        </h2>
        {/* La misma galería que el paso 4 del registro, sin su botón de cerrar
            el paso: aquí no hay paso que cerrar. */}
        <CargaDeFotos iniciales={fotos} />
      </section>

      <section className={estilos.bloque}>
        <h2 className={estilos.tituloBloque}>Escaparate</h2>
        <EscaparateEditable iniciales={productos} />
      </section>

      <div className={estilos.panel}>
        <Metricas negocio={negocio} />
        <Notificaciones />
        <BuzonDeConsultas />
      </div>

      <p className={estilos.nota}>
        A las consultas se responde por correo, desde el enlace de cada una.
      </p>
    </div>
  );
}
