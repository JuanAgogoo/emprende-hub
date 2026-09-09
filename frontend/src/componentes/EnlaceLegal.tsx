import { Link } from 'react-router-dom';

interface Props {
  readonly a: string;
  /** Texto plano: además de pintarse, compone el nombre accesible. */
  readonly texto: string;
  readonly className?: string;
}

/**
 * Un enlace a una página legal, que **siempre abre en una pestaña nueva**.
 *
 * La regla vive aquí y no repetida en cada sitio: quien llega a estas páginas
 * suele estar rellenando un formulario, y llevárselo de la página le borraría lo
 * escrito. Con cuatro enlaces repartidos por la aplicación, la única forma de
 * que no se le olvide a nadie es que no haya que acordarse.
 *
 * `rel="noopener noreferrer"` no es adorno: sin él la pestaña nueva recibe un
 * `window.opener` que apunta a la anterior.
 *
 * El nombre accesible avisa de que se abre aparte. Un enlace que cambia de
 * pestaña sin decirlo desorienta a quien no ve el navegador.
 */
export function EnlaceLegal({ a, texto, className }: Props) {
  return (
    <Link
      to={a}
      className={className}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`${texto} (se abre en una pestaña nueva)`}
    >
      {texto}
    </Link>
  );
}
