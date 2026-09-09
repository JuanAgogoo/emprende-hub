import { Link } from 'react-router-dom';
import estilos from './Directorio.module.css';

export function NoEncontrada() {
  return (
    <section className={`contenedor ${estilos.pagina}`}>
      <h1>Aquí no hay nada</h1>
      <p className={estilos.entrada}>
        La página que buscas no existe o cambió de dirección.
      </p>
      <p>
        <Link to="/">Volver al inicio</Link>
      </p>
    </section>
  );
}
