import { EnlaceLegal } from './EnlaceLegal';
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
              <EnlaceLegal
                a="/tratamiento-de-datos"
                texto="Tratamiento de datos"
                className={estilos.enlace}
              />
            </li>
            <li>
              <EnlaceLegal
                a="/informacion-personal"
                texto="Información personal"
                className={estilos.enlace}
              />
            </li>
          </ul>
        </nav>
      </div>
    </footer>
  );
}
