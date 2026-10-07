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
 * El acceso —gratis o su precio— va arriba y a la vista (HU-023), y el enlace
 * lleva directo al recurso: la plataforma no cobra ni intermedia (E1), así que
 * un curso gratuito se abre sin pasar por ninguna página de pago.
 */
export function TarjetaCurso({ curso, categoria, nivel }: Props) {
  return (
    <article className={estilos.tarjeta}>
      <div className={estilos.cabecera} aria-hidden="true">
        {curso.emoji}
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

        {/* Un enlace y no un botón: lleva a otra página, no hace una acción.
            `noopener` impide que la página de fuera controle esta pestaña. */}
        <a
          className={estilos.enlace}
          href={curso.urlRecurso}
          target="_blank"
          rel="noopener noreferrer"
        >
          {curso.gratuito ? 'Empezar gratis' : 'Ver el curso'}
          <span className={estilos.oculto}> (se abre en otra pestaña)</span>
          <span aria-hidden="true"> ↗</span>
        </a>
      </div>
    </article>
  );
}
