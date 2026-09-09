import { Link } from 'react-router-dom';
import estilos from './Pie.module.css';

export function Pie() {
  return (
    <footer className={estilos.pie}>
      <div className={`contenedor ${estilos.interior}`}>
        <p className={estilos.nota}>
          EmprendeHub · Directorio de emprendimientos del Valle de Aburrá
        </p>
        <nav aria-label="Legal">
          <ul className={estilos.lista}>
            <li>
              <Link to="/tratamiento-de-datos" className={estilos.enlace}>
                Tratamiento de datos
              </Link>
            </li>
            <li>
              <Link to="/informacion-personal" className={estilos.enlace}>
                Información personal
              </Link>
            </li>
          </ul>
        </nav>
      </div>
    </footer>
  );
}
