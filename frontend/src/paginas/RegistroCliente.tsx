import { useRef, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { CampoContrasena } from '../componentes/CampoContrasena';
import { EnlaceLegal } from '../componentes/EnlaceLegal';
import { useSesion } from '../estado/SesionContext';
import estilos from './Formulario.module.css';
import { useTitulo } from '../titulo';

interface Valores {
  readonly nombre: string;
  readonly correo: string;
  readonly contrasena: string;
  /** Solo vive en el navegador: no viaja en la petición, solo la comprueba. */
  readonly confirmacion: string;
  readonly aceptaDatos: boolean;
}

type Errores = Partial<Record<keyof Valores, string>>;

const VACIO: Valores = {
  nombre: '',
  correo: '',
  contrasena: '',
  confirmacion: '',
  aceptaDatos: false,
};

/**
 * Las mismas reglas que valida el backend, para que el 400 sea la excepción y
 * no la forma normal de descubrir un error.
 */
function validar(valores: Valores): Errores {
  const errores: Errores = {};

  if (valores.nombre.trim() === '') errores.nombre = 'El nombre es obligatorio';
  else if (valores.nombre.trim().length > 120)
    errores.nombre = 'El nombre no puede pasar de 120 caracteres';

  if (valores.correo.trim() === '') errores.correo = 'El correo es obligatorio';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valores.correo.trim()))
    errores.correo = 'El correo no tiene un formato válido';

  if (valores.contrasena === '') errores.contrasena = 'La contraseña es obligatoria';
  else if (valores.contrasena.length < 8)
    errores.contrasena = 'La contraseña debe tener al menos 8 caracteres';

  // Se compara con la contraseña ya escrita, no con una regla propia: el campo
  // no tiene requisitos, solo tiene que coincidir.
  if (valores.confirmacion === '') errores.confirmacion = 'Repite la contraseña';
  else if (valores.confirmacion !== valores.contrasena)
    errores.confirmacion = 'Las dos contraseñas no coinciden';

  if (!valores.aceptaDatos)
    errores.aceptaDatos = 'Hay que aceptar el tratamiento de datos para crear la cuenta';

  return errores;
}

/** El orden en que se recorre para llevar el foco al primer campo con error. */
const ORDEN: readonly (keyof Valores)[] = [
  'nombre',
  'correo',
  'contrasena',
  'confirmacion',
  'aceptaDatos',
];

export function RegistroCliente() {
  useTitulo('Registro de cliente');
  const { registrar } = useSesion();
  const navegar = useNavigate();
  const formulario = useRef<HTMLFormElement>(null);

  const [valores, setValores] = useState<Valores>(VACIO);
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [correoOcupado, setCorreoOcupado] = useState(false);

  const errores = validar(valores);
  const hayErrores = Object.keys(errores).length > 0;

  function cambiar<C extends keyof Valores>(campo: C, valor: Valores[C]) {
    setValores((previos) => ({ ...previos, [campo]: valor }));
    if (campo === 'correo') setCorreoOcupado(false);
  }

  function enfocarPrimerError(actuales: Errores) {
    const primero = ORDEN.find((campo) => actuales[campo] !== undefined);
    if (primero === undefined) return;
    formulario.current?.querySelector<HTMLElement>(`#${primero}`)?.focus();
  }

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    setFallo(null);
    setCorreoOcupado(false);

    if (hayErrores) {
      enfocarPrimerError(errores);
      return;
    }

    setEnviando(true);
    try {
      await registrar(valores.nombre.trim(), valores.correo.trim(), valores.contrasena);
      // El mismo destino que el inicio de sesión: los dos caminos crean un
      // cliente y tienen que dejarlo en el mismo sitio.
      navegar('/directorio', { replace: true });
    } catch (error: unknown) {
      if (error instanceof ErrorApi) {
        // Los errores de forma traen una clave por campo; los de negocio, no.
        // En este formulario el único conflicto posible es el correo ya usado,
        // así que se marca ese campo además de decirlo arriba.
        const porCampo = Object.entries(error.porCampo);
        if (porCampo.length > 0) {
          setFallo(porCampo.map(([, mensaje]) => mensaje).join('. '));
        } else {
          setFallo(error.message);
          if (error.estado === 400) setCorreoOcupado(true);
        }
      } else {
        const mensaje = error instanceof Error ? error.message : 'No se pudo crear la cuenta';
        setFallo(`${mensaje}. Comprueba que la API esté funcionando.`);
      }
    } finally {
      setEnviando(false);
    }
  }

  const errorCorreo = enviado ? errores.correo : undefined;
  const correoInvalido = errorCorreo !== undefined || correoOcupado;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <div className={estilos.tarjeta}>
        <h1 className={estilos.titulo}>Crear una cuenta</h1>
        <p className={estilos.entrada}>
          Con una cuenta puedes opinar sobre los negocios y escribirles. Explorar el directorio no
          necesita registro.
        </p>

        <form className={estilos.formulario} onSubmit={alEnviar} noValidate ref={formulario}>
          {fallo !== null && (
            <p className={estilos.fallo} role="alert">
              {fallo}
            </p>
          )}

          <div className={estilos.campo}>
            <label htmlFor="nombre">Nombre</label>
            <input
              id="nombre"
              name="nombre"
              type="text"
              autoComplete="name"
              value={valores.nombre}
              onChange={(evento) => cambiar('nombre', evento.target.value)}
              aria-invalid={enviado && errores.nombre !== undefined}
              aria-describedby={enviado && errores.nombre !== undefined ? 'error-nombre' : undefined}
            />
            {enviado && errores.nombre !== undefined && (
              <small id="error-nombre" className={estilos.error}>
                {errores.nombre}
              </small>
            )}
          </div>

          <div className={estilos.campo}>
            <label htmlFor="correo">Correo</label>
            <input
              id="correo"
              name="correo"
              type="email"
              autoComplete="email"
              value={valores.correo}
              onChange={(evento) => cambiar('correo', evento.target.value)}
              aria-invalid={correoInvalido}
              aria-describedby={errorCorreo !== undefined ? 'error-correo' : undefined}
            />
            {errorCorreo !== undefined && (
              <small id="error-correo" className={estilos.error}>
                {errorCorreo}
              </small>
            )}
          </div>

          <div className={estilos.campo}>
            <label htmlFor="contrasena">Contraseña</label>
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

          <div className={estilos.campo}>
            <label className={estilos.casilla} htmlFor="aceptaDatos">
              <input
                id="aceptaDatos"
                name="aceptaDatos"
                type="checkbox"
                checked={valores.aceptaDatos}
                onChange={(evento) => cambiar('aceptaDatos', evento.target.checked)}
                aria-invalid={enviado && errores.aceptaDatos !== undefined}
                aria-describedby={
                  enviado && errores.aceptaDatos !== undefined ? 'error-acepta' : undefined
                }
              />
              <span>
                He leído y acepto el{' '}
                <EnlaceLegal a="/tratamiento-de-datos" texto="tratamiento de datos" />.
              </span>
            </label>
            {enviado && errores.aceptaDatos !== undefined && (
              <small id="error-acepta" className={estilos.error}>
                {errores.aceptaDatos}
              </small>
            )}
          </div>

          <button className={estilos.primario} type="submit" disabled={enviando}>
            {enviando ? 'Creando la cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className={estilos.pie}>
          ¿Ya tienes cuenta? <Link to="/entrar">Entrar</Link>
        </p>
        <p className={estilos.pie}>
          ¿Tienes un negocio? <Link to="/registro-emprendedor">Publícalo en el directorio</Link>.
        </p>
      </div>
    </div>
  );
}
