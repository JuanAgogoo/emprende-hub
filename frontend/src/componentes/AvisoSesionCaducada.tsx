import { useSesion } from '../estado/SesionContext';
import estilos from './AvisoSesionCaducada.module.css';

/**
 * El aviso de que la sesión se cerró sola porque la API rechazó el token (401).
 *
 * Vive en la estructura común, encima de todas las páginas, y no en el login:
 * si el 401 llega en una página pública —al opinar, por ejemplo— no hay
 * redirección y el aviso tiene que verse allí. Si llega en una ruta protegida,
 * la ruta devuelve a `/entrar` y el aviso sigue puesto.
 *
 * `role="alert"` para que el lector de pantalla lo lea al aparecer. Se quita
 * con el botón o al volver a entrar.
 */
export function AvisoSesionCaducada() {
  const { sesionCaducada, cerrarAvisoDeCaducidad } = useSesion();

  if (!sesionCaducada) return null;

  return (
    <div className="contenedor">
      <div className={estilos.aviso} role="alert">
        <p>Tu sesión expiró o dejó de ser válida. Vuelve a iniciar sesión.</p>
        <button type="button" className={estilos.cerrar} onClick={cerrarAvisoDeCaducidad}>
          Cerrar el aviso
        </button>
      </div>
    </div>
  );
}
