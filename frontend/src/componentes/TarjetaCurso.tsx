import { Link } from 'react-router-dom';
import { visualDelCurso } from '../cursosVisual';
import { precio } from '../formato';
import type { Curso } from '../types/curso';
import estilos from './TarjetaCurso.module.css';

interface Props {
  readonly curso: Curso;
  /** Los nombres ya resueltos desde el catálogo: el curso solo trae el código. */
  readonly categoria: string;
  readonly nivel: string;
}

/**
 * La ficha de un curso: lo que hace falta para decidir antes de abrirlo (HU-021).
 *
 * El acceso —gratis o su precio— va arriba y a la vista (HU-023). Un curso
 * gratuito lleva a su ficha, que pide sesión; uno de pago, a la pantalla de
 * pago simulado, porque no hay pasarela integrada (E1).
 */
export function TarjetaCurso({ curso, categoria, nivel }: Props) {
  const visual = visualDelCurso(curso.titulo);
  return (
    <article className={estilos.tarjeta}>
      <div className={estilos.cabecera} aria-hidden="true">
        {visual !== null ? <img className={estilos.imagen} src={visual.imagen} alt="" loading="lazy" /> : curso.emoji}
      </div>

      <div className={estilos.cuerpo}>
        <p className={estilos.etiquetas}>
          <span className={estilos.categoria}>{categoria}</span>
          {/* La palabra dice lo mismo que el color: el color nunca va solo. */}
          {curso.gratuito || curso.precio === null ? (
            <span className={`${estilos.acceso} ${estilos.gratis}`}>Gratis</span>
          ) : (
            <span className={estilos.acceso}>{precio(curso.precio)}</span>
          )}
        </p>

        <h3 className={estilos.titulo}>{curso.titulo}</h3>
        <p className={estilos.descripcion}>{curso.descripcion}</p>

        <dl className={estilos.datos}>
          <div>
            <dt>Duración</dt>
            <dd>{curso.duracion}</dd>
          </div>
          <div>
            <dt>Nivel</dt>
            <dd>{nivel}</dd>
          </div>
        </dl>

        {curso.gratuito || curso.precio === null ? (
          <Link className={estilos.enlace} to={`/cursos/${curso.id}`}>
            Empezar gratis
          </Link>
        ) : (
          <Link className={estilos.enlace} to={`/cursos/${curso.id}/pago`}>
            Comprar curso
          </Link>
        )}
      </div>
    </article>
  );
}
