import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { CampoContrasena } from '../componentes/CampoContrasena';
import { useSesion } from '../estado/SesionContext';
import type { Rol } from '../types/sesion';
import estilos from './Formulario.module.css';
import { useTitulo } from '../titulo';

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

/**
 * El correo con el que se acaba de registrar, si viene del alta.
 *
 * `location.state` es dato externo —lo pone quien navega, y sobrevive a una
 * recarga—, así que se comprueba la forma antes de usarlo en vez de confiar en
 * que sea lo que esperamos.
 */
function correoRecienRegistrado(estado: unknown): string | null {
  if (typeof estado !== 'object' || estado === null) return null;
  const traspaso = estado as Record<string, unknown>;
  return typeof traspaso.registrado === 'string' ? traspaso.registrado : null;
}

/**
 * La página desde la que se pidió entrar, si la hay.
 *
 * La pone quien enlaza al login —hoy, el bloque de opiniones del perfil— para
 * volver justo a donde se estaba. Se comprueba que sea una ruta de esta
 * aplicación: `state` es dato externo y una dirección completa enviaría a
 * cualquier sitio.
 */
function destinoDeVuelta(estado: unknown): string | null {
  if (typeof estado !== 'object' || estado === null) return null;
  const traspaso = estado as Record<string, unknown>;
  const volverA = traspaso.volverA;

  if (typeof volverA !== 'string') return null;
  return volverA.startsWith('/') && !volverA.startsWith('//') ? volverA : null;
}

/** A dónde va cada quien según su rol. */
function destino(rol: Rol): string {
  switch (rol) {
    case 'EMPRENDEDOR':
      return '/mi-negocio';
    // Quien busca negocios entra directo al directorio, no a la portada, que
    // ya ha visto para llegar hasta aquí.
    case 'CLIENTE':
      return '/directorio';
    // El panel de administración no es de esta fase.
    case 'ADMIN':
      return '/';
  }
}

export function Login() {
  useTitulo('Inicio de sesión');
  const { entrar } = useSesion();
  const navegar = useNavigate();

  const estadoDeNavegacion = useLocation().state;
  const recienRegistrado = correoRecienRegistrado(estadoDeNavegacion);
  const vuelta = destinoDeVuelta(estadoDeNavegacion);

  const [valores, setValores] = useState<Valores>({
    correo: recienRegistrado ?? '',
    contrasena: '',
  });
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
      // Volver a donde se estaba manda sobre el destino del rol: quien venía a
      // opinar sobre un negocio quiere ese negocio, no su panel.
      navegar(vuelta ?? destino(sesion.rol), { replace: true });
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
          {/* El fallo manda sobre el aviso: si el intento acaba de fallar, lo
              último que pasó no es que la cuenta se creara. */}
          {fallo === null && recienRegistrado !== null && (
            <p className={estilos.aviso} role="status">
              Tu cuenta está creada. Entra con el correo y la contraseña que acabas de elegir.
            </p>
          )}

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
            <CampoContrasena
              id="contrasena"
              valor={valores.contrasena}
              autoComplete="current-password"
              invalido={enviado && errores.contrasena !== undefined}
              describedBy={
                enviado && errores.contrasena !== undefined ? 'error-contrasena' : undefined
              }
              alCambiar={(valor) => cambiar('contrasena', valor)}
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
