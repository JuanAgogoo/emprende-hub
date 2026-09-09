import { Link } from 'react-router-dom';
import estilos from './Inicio.module.css';

export function Inicio() {
  return (
    <section className={`contenedor ${estilos.hero}`}>
      <h1 className={estilos.titulo}>
        Los emprendimientos de tu barrio, <span className={estilos.destacado}>en un solo sitio</span>
      </h1>
      <p className={estilos.entrada}>
        Explora negocios del Valle de Aburrá sin necesidad de registrarte. Y si el negocio es
        tuyo, publícalo y deja que te encuentren.
      </p>
      <div className={estilos.acciones}>
        <Link to="/directorio" className={estilos.primario}>
          Explorar el directorio
        </Link>
      </div>
    </section>
  );
}
