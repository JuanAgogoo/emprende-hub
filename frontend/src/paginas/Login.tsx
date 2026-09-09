import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { useSesion } from '../estado/SesionContext';
import type { Rol } from '../types/sesion';
import estilos from './Formulario.module.css';

interface Valores {
  readonly correo: string;
  readonly contrasena: string;
}

/**
 * Validación de forma, fuera del componente y sin librería. Es lo que el curso
 * hace con `Validators`, y se puede leer de una pasada.
 */
function validar(valores: Valores): Partial<Record<keyof Valores, string>> {
  const errores: Partial<Record<keyof Valores, string>> = {};

  if (valores.correo.trim() === '') errores.correo = 'El correo es obligatorio';
  else if (!valores.correo.includes('@')) errores.correo = 'El correo no tiene un formato válido';

  if (valores.contrasena === '') errores.contrasena = 'La contraseña es obligatoria';

  return errores;
}

/** A dónde va cada quien según su rol. */
function destino(rol: Rol): string {
  switch (rol) {
    case 'EMPRENDEDOR':
      return '/mi-negocio';
    case 'CLIENTE':
    // El panel de administración no es de esta fase.
    case 'ADMIN':
      return '/';
  }
}

export function Login() {
  const { entrar } = useSesion();
  const navegar = useNavigate();

  const [valores, setValores] = useState<Valores>({ correo: '', contrasena: '' });
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  const errores = validar(valores);
  const hayErrores = Object.keys(errores).length > 0;

  function cambiar(campo: keyof Valores, valor: string) {
    setValores((previos) => ({ ...previos, [campo]: valor }));
  }

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    setFallo(null);
    if (hayErrores) return;

    setEnviando(true);
    try {
      const sesion = await entrar(valores.correo.trim(), valores.contrasena);
      navegar(destino(sesion.rol), { replace: true });
    } catch (error: unknown) {
      // El 401 no vacía el formulario: solo se dice qué pasó.
      if (error instanceof ErrorApi && error.estado === 401) {
        setFallo('Correo o contraseña incorrectos.');
      } else {
        const mensaje = error instanceof Error ? error.message : 'No se pudo entrar';
        setFallo(`${mensaje}. Comprueba que la API esté funcionando.`);
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <div className={estilos.tarjeta}>
        <h1 className={estilos.titulo}>Entrar</h1>
        <p className={estilos.entrada}>
          Un solo acceso para todo el mundo: al entrar, cada cuenta llega a lo suyo.
        </p>

        <form className={estilos.formulario} onSubmit={alEnviar} noValidate>
          {fallo !== null && (
            <p className={estilos.fallo} role="alert">
              {fallo}
            </p>
          )}

          <div className={estilos.campo}>
            <label htmlFor="correo">Correo</label>
            <input
              id="correo"
              type="email"
              autoComplete="email"
              value={valores.correo}
              onChange={(evento) => cambiar('correo', evento.target.value)}
              aria-invalid={enviado && errores.correo !== undefined}
              aria-describedby={enviado && errores.correo !== undefined ? 'error-correo' : undefined}
            />
            {/* El mensaje solo aparece tras intentar enviar: su markAllAsTouched(). */}
            {enviado && errores.correo !== undefined && (
              <small id="error-correo" className={estilos.error}>
                {errores.correo}
              </small>
            )}
          </div>

          <div className={estilos.campo}>
            <label htmlFor="contrasena">Contraseña</label>
            <input
              id="contrasena"
              type="password"
              autoComplete="current-password"
              value={valores.contrasena}
              onChange={(evento) => cambiar('contrasena', evento.target.value)}
              aria-invalid={enviado && errores.contrasena !== undefined}
              aria-describedby={
                enviado && errores.contrasena !== undefined ? 'error-contrasena' : undefined
              }
            />
            {enviado && errores.contrasena !== undefined && (
              <small id="error-contrasena" className={estilos.error}>
                {errores.contrasena}
              </small>
            )}
          </div>

          {/* Deshabilitado mientras se envía: sin esto se pulsa dos veces. */}
          <button className={estilos.primario} type="submit" disabled={enviando}>
            {enviando ? 'Entrando…' : 'Entrar'}
          </button>
        </form>

        <p className={estilos.pie}>
          ¿No tienes cuenta? <Link to="/registro">Crear una cuenta</Link>
        </p>
      </div>
    </div>
  );
}
