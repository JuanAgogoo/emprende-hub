import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useSesion } from '../estado/SesionContext';
import estilos from './Cabecera.module.css';

/** Las rutas que existen hoy. Crecerá con cada incremento. */
const ENLACES = [
  { a: '/', texto: 'Inicio' },
  { a: '/directorio', texto: 'Directorio' },
] as const;

export function Cabecera() {
  const { sesion, salir } = useSesion();
  const navegar = useNavigate();

  function cerrar() {
    salir();
    navegar('/');
  }

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

            {sesion === null ? (
              <li>
                <Link to="/entrar" className={estilos.acceso}>
                  Entrar
                </Link>
              </li>
            ) : (
              <>
                {sesion.rol === 'EMPRENDEDOR' && (
                  <li>
                    <NavLink
                      to="/mi-negocio"
                      className={({ isActive }) =>
                        isActive ? `${estilos.enlace} ${estilos.enlaceActivo}` : estilos.enlace
                      }
                    >
                      Mi negocio
                    </NavLink>
                  </li>
                )}
                <li className={estilos.cuenta}>
                  {/* El nombre completo no cabe en un móvil estrecho. */}
                  <span className={estilos.nombre}>{sesion.nombre.split(' ')[0]}</span>
                </li>
                <li>
                  <button type="button" className={estilos.enlace} onClick={cerrar}>
                    Salir
                  </button>
                </li>
              </>
            )}
          </ul>
        </nav>
      </div>
    </header>
  );
}
