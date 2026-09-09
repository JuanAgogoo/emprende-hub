import { Link, NavLink } from 'react-router-dom';
import estilos from './Cabecera.module.css';

/** Las rutas que existen hoy. Crecerá con cada incremento. */
const ENLACES = [
  { a: '/', texto: 'Inicio' },
  { a: '/directorio', texto: 'Directorio' },
] as const;

export function Cabecera() {
  return (
    <header className={estilos.cabecera}>
      <div className={`contenedor ${estilos.interior}`}>
        <Link to="/" className={estilos.marca}>
          Emprende<span className={estilos.marcaAcento}>Hub</span>
        </Link>

        <nav aria-label="Principal">
          <ul className={estilos.lista}>
            {ENLACES.map((enlace) => (
              <li key={enlace.a}>
                <NavLink
                  to={enlace.a}
                  className={({ isActive }) =>
                    isActive ? `${estilos.enlace} ${estilos.enlaceActivo}` : estilos.enlace
                  }
                  end={enlace.a === '/'}
                >
                  {enlace.texto}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </div>
    </header>
  );
}
