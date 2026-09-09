import estilos from './Directorio.module.css';

export function Directorio() {
  return (
    <section className={`contenedor ${estilos.pagina}`}>
      <h1>Directorio</h1>
      <p className={estilos.entrada}>
        La búsqueda, los filtros y el listado llegan en el siguiente incremento.
      </p>
    </section>
  );
}
