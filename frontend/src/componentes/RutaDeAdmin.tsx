import { Navigate } from 'react-router-dom';
import { useSesion } from '../estado/SesionContext';
import type { ReactNode } from 'react';

/**
 * Deja pasar solo a quien tiene sesión de administrador.
 *
 * Igual que `RutaDeEmprendedor`, es una comodidad de la interfaz y no la
 * seguridad: todo `/admin` del backend responde 403 a cualquier otro rol.
 */
export function RutaDeAdmin({ children }: { readonly children: ReactNode }) {
  const { sesion } = useSesion();

  if (sesion === null) return <Navigate to="/entrar" replace />;
  if (sesion.rol !== 'ADMIN') return <Navigate to="/" replace />;

  return <>{children}</>;
}
