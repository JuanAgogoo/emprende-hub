import { Navigate } from 'react-router-dom';
import { useSesion } from '../estado/SesionContext';
import type { ReactNode } from 'react';

/**
 * Deja pasar solo a quien tiene sesión de emprendedor.
 *
 * Es una comodidad de la interfaz, no una medida de seguridad: quien mande la
 * petición a mano sigue topándose con el backend, que es donde de verdad se
 * comprueba el rol. Aquí solo se evita enseñar una pantalla que no va a
 * funcionar.
 */
export function RutaDeEmprendedor({ children }: { readonly children: ReactNode }) {
  const { sesion } = useSesion();

  if (sesion === null) return <Navigate to="/entrar" replace />;
  // A un cliente no se le enseña un error: se le devuelve a la portada.
  if (sesion.rol !== 'EMPRENDEDOR') return <Navigate to="/" replace />;

  return <>{children}</>;
}
