import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { iniciarSesion } from '../api/auth';
import { borrarSesion, guardarSesion, leerSesion } from '../almacenSesion';
import type { Sesion } from '../types/sesion';

/**
 * Lo que ve quien consume el contexto: los datos y las acciones.
 *
 * **Nunca el `setState`.** Es la traducción del patrón del curso, donde el
 * servicio guarda un signal privado, expone su lectura y deja que las
 * mutaciones pasen por métodos.
 */
interface ValorSesion {
  readonly sesion: Sesion | null;
  entrar: (correo: string, contrasena: string) => Promise<Sesion>;
  salir: () => void;
}

const SesionContext = createContext<ValorSesion | null>(null);

export function SesionProvider({ children }: { readonly children: ReactNode }) {
  // Se lee del almacén al arrancar: recargar la página no echa a nadie.
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion());

  const entrar = useCallback(async (correo: string, contrasena: string): Promise<Sesion> => {
    const respuesta = await iniciarSesion(correo, contrasena);
    const nueva: Sesion = {
      token: respuesta.token,
      nombre: respuesta.nombre,
      correo: respuesta.correo,
      rol: respuesta.rol,
    };
    guardarSesion(nueva);
    setSesion(nueva);
    // Se devuelve para que quien llame pueda derivar según el rol sin esperar
    // a que el estado se haya propagado.
    return nueva;
  }, []);

  const salir = useCallback(() => {
    borrarSesion();
    setSesion(null);
  }, []);

  return (
    <SesionContext.Provider value={{ sesion, entrar, salir }}>
      {children}
    </SesionContext.Provider>
  );
}

/** El equivalente del `inject(AuthService)` del curso. */
export function useSesion(): ValorSesion {
  const valor = useContext(SesionContext);
  if (valor === null) throw new Error('useSesion debe usarse dentro de SesionProvider');
  return valor;
}
