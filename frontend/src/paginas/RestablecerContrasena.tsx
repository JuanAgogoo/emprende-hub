import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { comprobarEnlaceDeRecuperacion, restablecerContrasena } from '../api/auth';
import { ErrorApi } from '../api/cliente';
import { CampoContrasena } from '../componentes/CampoContrasena';
import { useTitulo } from '../titulo';
import estilos from './Formulario.module.css';

interface Valores {
  readonly contrasena: string;
  /** Solo vive en el navegador: no viaja en la petición, solo la comprueba. */
  readonly confirmacion: string;
}

type Errores = Partial<Record<keyof Valores, string>>;

/**
 * En qué estado está el enlace de la dirección.
 *
 * Caducado no es lo mismo que «no se pudo comprobar»: el primero se arregla
 * pidiendo otro y el segundo reintentando, así que no comparten pantalla.
 */
type EstadoEnlace =
  | { readonly estado: 'COMPROBANDO' }
  | { readonly estado: 'VALIDO' }
  | { readonly estado: 'CADUCADO' }
  | { readonly estado: 'ERROR'; readonly mensaje: string };

/** La misma comprobación de coincidencia que los dos registros. */
function validar(valores: Valores): Errores {
  const errores: Errores = {};

  if (valores.contrasena === '') errores.contrasena = 'La contraseña es obligatoria';
  else if (valores.contrasena.length < 8)
    errores.contrasena = 'La contraseña debe tener al menos 8 caracteres';

  if (valores.confirmacion === '') errores.confirmacion = 'Repite la contraseña';
  else if (valores.confirmacion !== valores.contrasena)
    errores.confirmacion = 'Las dos contraseñas no coinciden';

  return errores;
}

/**
 * Elegir la contraseña nueva desde el enlace del correo.
 *
 * **El enlace se comprueba al entrar**, antes de enseñar nada: pedir una
 * contraseña dos veces para después decir que el enlace ya no vale sería
 * hacerle perder el tiempo a quien lo abre tarde.
 */
export function RestablecerContrasena() {
  useTitulo('Elegir una contraseña nueva');
  const { token } = useParams();
  const navegar = useNavigate();

  const [enlace, setEnlace] = useState<EstadoEnlace>({ estado: 'COMPROBANDO' });
  const [valores, setValores] = useState<Valores>({ contrasena: '', confirmacion: '' });
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;

    if (token === undefined || token === '') {
      setEnlace({ estado: 'CADUCADO' });
      return;
    }

    setEnlace({ estado: 'COMPROBANDO' });

    comprobarEnlaceDeRecuperacion(token)
      .then(() => {
        if (vigente) setEnlace({ estado: 'VALIDO' });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        // 410 y no 404: el enlace existió y ha dejado de servir.
        if (error instanceof ErrorApi && error.estado === 410) {
          setEnlace({ estado: 'CADUCADO' });
          return;
        }
        const mensaje = error instanceof Error ? error.message : 'No se pudo comprobar el enlace';
        setEnlace({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [token]);

  const errores = validar(valores);
  const hayErrores = Object.keys(errores).length > 0;

  function cambiar(campo: keyof Valores, valor: string) {
    setValores((previos) => ({ ...previos, [campo]: valor }));
  }

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    setFallo(null);
    if (hayErrores || token === undefined) return;

    setEnviando(true);
    try {
      await restablecerContrasena(token, valores.contrasena);

      // Al login con el aviso, igual que el alta de cliente. `replace` evita
      // que «atrás» devuelva a un enlace que ya se ha gastado.
      navegar('/entrar', { replace: true, state: { restablecido: true } });
    } catch (error: unknown) {
      // El enlace pudo caducar entre la comprobación y el envío.
      if (error instanceof ErrorApi && error.estado === 410) {
        setEnlace({ estado: 'CADUCADO' });
        return;
      }
      const mensaje = error instanceof Error ? error.message : 'No se pudo guardar la contraseña';
      setFallo(mensaje);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <div className={estilos.tarjeta}>
        <h1 className={estilos.titulo}>Elegir una contraseña nueva</h1>

        {enlace.estado === 'COMPROBANDO' && (
          <p className={estilos.entrada} aria-busy="true">
            Comprobando el enlace…
          </p>
        )}

        {enlace.estado === 'CADUCADO' && (
          <>
            <p className={estilos.fallo} role="alert">
              Este enlace ya no vale. Los enlaces caducan a los 30 minutos y sirven una sola vez.
            </p>
            <p className={estilos.entrada}>
              Pide otro y ábrelo desde el correo más reciente.
            </p>
            <Link className={estilos.pie} to="/recuperar">
              Pedir un enlace nuevo
            </Link>
          </>
        )}

        {enlace.estado === 'ERROR' && (
          <p className={estilos.fallo} role="alert">
            No se pudo comprobar el enlace: {enlace.mensaje}. Comprueba que la API esté funcionando
            y vuelve a cargar la página.
          </p>
        )}

        {enlace.estado === 'VALIDO' && (
          <>
            <p className={estilos.entrada}>
              Escríbela dos veces. Al guardarla, este enlace deja de servir.
            </p>

            <form className={estilos.formulario} onSubmit={alEnviar} noValidate>
              {fallo !== null && (
                <p className={estilos.fallo} role="alert">
                  {fallo}
                </p>
              )}

              <div className={estilos.campo}>
                <label htmlFor="contrasena">Contraseña nueva</label>
                <CampoContrasena
                  id="contrasena"
                  name="contrasena"
                  valor={valores.contrasena}
                  autoComplete="new-password"
                  invalido={enviado && errores.contrasena !== undefined}
                  describedBy={
                    enviado && errores.contrasena !== undefined
                      ? 'error-contrasena'
                      : 'ayuda-contrasena'
                  }
                  alCambiar={(valor) => cambiar('contrasena', valor)}
                />
                {enviado && errores.contrasena !== undefined ? (
                  <small id="error-contrasena" className={estilos.error}>
                    {errores.contrasena}
                  </small>
                ) : (
                  <small id="ayuda-contrasena" className={estilos.ayuda}>
                    Al menos 8 caracteres
                  </small>
                )}
              </div>

              <div className={estilos.campo}>
                <label htmlFor="confirmacion">Repite la contraseña</label>
                <CampoContrasena
                  id="confirmacion"
                  name="confirmacion"
                  valor={valores.confirmacion}
                  autoComplete="new-password"
                  invalido={enviado && errores.confirmacion !== undefined}
                  describedBy={
                    enviado && errores.confirmacion !== undefined ? 'error-confirmacion' : undefined
                  }
                  alCambiar={(valor) => cambiar('confirmacion', valor)}
                />
                {enviado && errores.confirmacion !== undefined && (
                  <small id="error-confirmacion" className={estilos.error}>
                    {errores.confirmacion}
                  </small>
                )}
              </div>

              <button className={estilos.primario} type="submit" disabled={enviando}>
                {enviando ? 'Guardando…' : 'Guardar la contraseña'}
              </button>
            </form>
          </>
        )}

        <p className={estilos.pie}>
          <Link to="/entrar">Volver a entrar</Link>
        </p>
      </div>
    </div>
  );
}
