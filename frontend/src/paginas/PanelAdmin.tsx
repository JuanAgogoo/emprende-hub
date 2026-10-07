import { NavLink, Outlet } from 'react-router-dom';
import estilos from './Admin.module.css';

/** Una pestaña por cola de trabajo. Crece con cada historia del panel. */
const SECCIONES = [
  { a: '/admin/negocios', texto: 'Negocios' },
  { a: '/admin/cambios', texto: 'Cambios' },
] as const;

/**
 * El armazón del panel de administración: el título y las pestañas, y debajo
 * la sección elegida (`<Outlet>`). Cada sección es una ruta, así que se puede
 * enlazar, recargar y volver atrás con el navegador.
 */
export function PanelAdmin() {
  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.encabezado}>
        <h1>Administración</h1>
        <p className={estilos.entrada}>
          Lo que espera una decisión tuya. Cada acción queda registrada en el historial de
          moderación.
        </p>
      </header>

      <nav aria-label="Secciones de administración">
        <ul className={estilos.pestanas}>
          {SECCIONES.map((seccion) => (
            <li key={seccion.a}>
              <NavLink
                to={seccion.a}
                className={({ isActive }) =>
                  isActive ? `${estilos.pestana} ${estilos.pestanaActiva}` : estilos.pestana
                }
              >
                {seccion.texto}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      <Outlet />
    </div>
  );
}
