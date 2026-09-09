import { Link } from 'react-router-dom';
import { calificacion, nivelPrecio, numero } from '../formato';
import type { TarjetaNegocio as Negocio } from '../types/negocio';
import estilos from './TarjetaNegocio.module.css';

interface Props {
  readonly negocio: Negocio;
}

export function TarjetaNegocio({ negocio }: Props) {
  const nota = calificacion(negocio.calificacionPromedio);
  const ubicacion = negocio.barrio ?? negocio.ciudad;

  return (
    <article className={estilos.tarjeta}>
      <Link to={`/negocios/${negocio.id}`} className={estilos.enlace}>
        <div className={estilos.marco}>
          {negocio.fotoPrincipal === null ? (
            // Sin foto no se enseña una imagen rota: se enseña la inicial.
            <div className={estilos.sinFoto} aria-hidden="true">
              {negocio.nombre.charAt(0)}
            </div>
          ) : (
            <img
              className={estilos.foto}
              src={negocio.fotoPrincipal}
              alt={`Local de ${negocio.nombre}`}
              loading="lazy"
            />
          )}
          <span className={estilos.categoria}>{negocio.categoria}</span>
        </div>

        <div className={estilos.cuerpo}>
          <h3 className={estilos.nombre}>{negocio.nombre}</h3>

          <p className={estilos.meta}>
            <span>{ubicacion}</span>
            <span aria-hidden="true">·</span>
            <span aria-label={`Nivel de precio ${negocio.nivelPrecio.toLowerCase()}`}>
              {nivelPrecio(negocio.nivelPrecio)}
            </span>
          </p>

          {/* No tener opiniones no es tener malas notas (C5). */}
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
        </div>
      </Link>
    </article>
  );
}
