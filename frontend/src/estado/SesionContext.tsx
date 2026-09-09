import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { iniciarSesion, registrarCliente } from '../api/auth';
import { borrarSesion, guardarSesion, leerSesion } from '../almacenSesion';
import type { RespuestaAuth, Sesion } from '../types/sesion';

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
  registrar: (nombre: string, correo: string, contrasena: string) => Promise<Sesion>;
  salir: () => void;
}

const SesionContext = createContext<ValorSesion | null>(null);

/** La respuesta de /auth trae más de lo que se guarda: se queda con lo suyo. */
function deRespuesta(respuesta: RespuestaAuth): Sesion {
  return {
    token: respuesta.token,
    nombre: respuesta.nombre,
    correo: respuesta.correo,
    rol: respuesta.rol,
  };
}

export function SesionProvider({ children }: { readonly children: ReactNode }) {
  // Se lee del almacén al arrancar: recargar la página no echa a nadie.
  const [sesion, setSesion] = useState<Sesion | null>(() => leerSesion());

  const entrar = useCallback(async (correo: string, contrasena: string): Promise<Sesion> => {
    const respuesta = await iniciarSesion(correo, contrasena);
    const nueva = deRespuesta(respuesta);
    guardarSesion(nueva);
    setSesion(nueva);
    // Se devuelve para que quien llame pueda derivar según el rol sin esperar
    // a que el estado se haya propagado.
    return nueva;
  }, []);

  const registrar = useCallback(
    async (nombre: string, correo: string, contrasena: string): Promise<Sesion> => {
      // El registro devuelve el token, así que se entra sin pasar por el login.
      const respuesta = await registrarCliente(nombre, correo, contrasena);
      const nueva = deRespuesta(respuesta);
      guardarSesion(nueva);
      setSesion(nueva);
      return nueva;
    },
    [],
  );

  const salir = useCallback(() => {
    borrarSesion();
    setSesion(null);
  }, []);

  return (
    <SesionContext.Provider value={{ sesion, entrar, registrar, salir }}>
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
