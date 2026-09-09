import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { obtenerPerfil } from '../api/directorio';
import { ErrorApi } from '../api/cliente';
import { Galeria } from '../componentes/Galeria';
import { calificacion, nivelPrecio, numero, precio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { PerfilNegocio as Perfil } from '../types/negocio';
import { useTitulo } from '../titulo';
import { NoEncontrada } from './NoEncontrada';
import estilos from './PerfilNegocio.module.css';

export function PerfilNegocio() {
  const { id } = useParams();
  const [carga, setCarga] = useState<EstadoCarga<Perfil>>({ estado: 'CARGANDO' });
  // Un 404 no es un error que reintentar: es que ese negocio no se ve (B6).
  const [noExiste, setNoExiste] = useState(false);

  useEffect(() => {
    let vigente = true;
    const identificador = Number(id);

    if (!Number.isInteger(identificador) || identificador <= 0) {
      setNoExiste(true);
      return;
    }

    setCarga({ estado: 'CARGANDO' });
    setNoExiste(false);

    obtenerPerfil(identificador)
      .then((perfil) => {
        if (vigente) setCarga({ estado: 'EXITO', datos: perfil });
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

  // El caso de «no existe» se contempla aquí aunque `NoEncontrada` ponga el
  // suyo: los efectos del hijo corren antes que los del padre, así que este lo
  // pisaría después y la pestaña acabaría diciendo «Negocio».
  useTitulo(
    noExiste
      ? 'Página no encontrada'
      : carga.estado === 'EXITO'
        ? carga.datos.nombre
        : 'Negocio',
  );

  if (noExiste) return <NoEncontrada />;

  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div className={`contenedor ${estilos.pagina}`} aria-busy="true">
          <div className={estilos.esqueletoFoto} />
          <div className={estilos.esqueletoTexto} />
        </div>
      );

    case 'ERROR':
      return (
        <div className={`contenedor ${estilos.pagina}`}>
          <p className={estilos.error} role="alert">
            No se pudo cargar el negocio: {carga.mensaje}. Comprueba que la API esté funcionando y
            vuelve a intentarlo.
          </p>
        </div>
      );

    case 'EXITO':
      return <Contenido negocio={carga.datos} />;

    default:
      return casoImposible(carga);
  }
}

function Contenido({ negocio }: { readonly negocio: Perfil }) {
  const nota = calificacion(negocio.calificacionPromedio);
  const ubicacion = negocio.barrio === null ? negocio.ciudad : `${negocio.barrio}, ${negocio.ciudad}`;
  const disponibles = negocio.productos.filter((producto) => producto.disponible).length;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <p className={estilos.migas}>
        <Link to="/directorio">Directorio</Link> <span aria-hidden="true">/</span>{' '}
        <span>{negocio.categoria}</span>
      </p>

      <div className={estilos.columnas}>
        <div className={estilos.principal}>
          <Galeria
            fotos={negocio.fotos}
            nombreNegocio={negocio.nombre}
            nombreTransicion={`negocio-${negocio.id}`}
          />

          <section className={estilos.bloque}>
            <h2 className={estilos.tituloBloque}>Sobre el negocio</h2>
            <p className={estilos.descripcion}>{negocio.descripcion}</p>
          </section>

          <section className={estilos.bloque}>
            <h2 className={estilos.tituloBloque}>
              Escaparate
              {negocio.productos.length > 0 && (
                <span className={estilos.contador}>
                  {numero(disponibles)} de {numero(negocio.productos.length)} disponibles
                </span>
              )}
            </h2>

            {negocio.productos.length === 0 ? (
              <p className={estilos.vacio}>Este negocio todavía no ha publicado productos.</p>
            ) : (
              <ul className={estilos.productos}>
                {negocio.productos.map((producto) => (
                  <li
                    key={producto.id}
                    className={
                      producto.disponible ? estilos.producto : `${estilos.producto} ${estilos.agotado}`
                    }
                  >
                    {/* Decorativa: el nombre va justo debajo y el lector de
                        pantalla lo leería dos veces. */}
                    <img
                      className={estilos.fotoProducto}
                      src={producto.foto}
                      alt=""
                      loading="lazy"
                    />
                    <div className={estilos.filaProducto}>
                      <h3 className={estilos.nombreProducto}>{producto.nombre}</h3>
                      <span className={estilos.precio}>{precio(producto.precio)}</span>
                    </div>
                    {producto.descripcion !== null && producto.descripcion !== '' && (
                      <p className={estilos.descripcionProducto}>{producto.descripcion}</p>
                    )}
                    {/* La disponibilidad no se marca solo con un color más pálido. */}
                    {!producto.disponible && (
                      <span className={estilos.etiquetaAgotado}>No disponible ahora</span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>

        <aside className={estilos.ficha}>
          <h1 className={estilos.nombre}>{negocio.nombre}</h1>

          <p className={estilos.meta}>
            <span className={estilos.categoria}>{negocio.categoria}</span>
            <span aria-hidden="true">·</span>
            <span aria-label={`Nivel de precio ${negocio.nivelPrecio.toLowerCase()}`}>
              {nivelPrecio(negocio.nivelPrecio)}
            </span>
          </p>

          {nota === null ? (
            <p className={estilos.sinNota}>Sin opiniones todavía</p>
          ) : (
            <p className={estilos.nota}>
              <span className={estilos.estrella} aria-hidden="true">
                ★
              </span>
              <strong>{nota}</strong>
              <span className={estilos.opiniones}>
                {numero(negocio.numeroOpiniones)}{' '}
                {negocio.numeroOpiniones === 1 ? 'opinión' : 'opiniones'}
              </span>
            </p>
          )}

          <dl className={estilos.datos}>
            <dt>Dónde</dt>
            <dd>{ubicacion}</dd>
            <dt>Teléfono</dt>
            <dd>
              <a href={`tel:${negocio.telefono}`}>{negocio.telefono}</a>
            </dd>
          </dl>

          {(negocio.instagram !== null || negocio.linkedin !== null) && (
            <div className={estilos.redes}>
              {negocio.instagram !== null && (
                <a
                  className={estilos.red}
                  href={negocio.instagram}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  Instagram
                </a>
              )}
              {negocio.linkedin !== null && (
                <a
                  className={estilos.red}
                  href={negocio.linkedin}
                  target="_blank"
                  rel="noreferrer noopener"
                >
                  LinkedIn
                </a>
              )}
            </div>
          )}

          <a className={estilos.primario} href={`tel:${negocio.telefono}`}>
            Llamar al negocio
          </a>
        </aside>
      </div>
    </div>
  );
}
