import { useState, type FormEvent } from 'react';
import { MAXIMO_ASUNTO, MAXIMO_MENSAJE, type DatosDeConsulta } from '../types/consulta';
import formulario from '../paginas/Formulario.module.css';
import estilos from './FormularioConsulta.module.css';

interface Props {
  readonly enviando: boolean;
  /** El fallo que devolvió la API, si el último intento no salió. */
  readonly fallo: string | null;
  readonly alEnviar: (datos: DatosDeConsulta) => void;
}

interface Valores {
  readonly asunto: string;
  readonly mensaje: string;
}

type Errores = Partial<Record<keyof Valores, string>>;

/**
 * Las mismas reglas que valida el backend, para que el 400 sea la excepción y
 * no la forma normal de descubrir un error. **El asunto también tiene tope**,
 * aunque sea menos conocido que el del mensaje.
 */
function validar(valores: Valores): Errores {
  const errores: Errores = {};

  if (valores.asunto.trim() === '') errores.asunto = 'El asunto es obligatorio';
  else if (valores.asunto.length > MAXIMO_ASUNTO)
    errores.asunto = `El asunto no puede pasar de ${MAXIMO_ASUNTO} caracteres`;

  if (valores.mensaje.trim() === '') errores.mensaje = 'El mensaje es obligatorio';
  else if (valores.mensaje.length > MAXIMO_MENSAJE)
    errores.mensaje = `El mensaje no puede pasar de ${MAXIMO_MENSAJE} caracteres`;

  return errores;
}

/**
 * Escribirle al negocio: asunto y mensaje, y nada más.
 *
 * No pide nombre ni correo a propósito: contactar exige sesión, así que los pone
 * la cuenta. Un formulario donde se escriben a mano dejaría firmar con el correo
 * de otra persona.
 */
export function FormularioConsulta({ enviando, fallo, alEnviar }: Props) {
  const [valores, setValores] = useState<Valores>({ asunto: '', mensaje: '' });
  const [enviado, setEnviado] = useState(false);

  const errores = validar(valores);
  const hayErrores = Object.keys(errores).length > 0;

  function cambiar(campo: keyof Valores, valor: string) {
    setValores((previos) => ({ ...previos, [campo]: valor }));
  }

  function alSoltar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    if (hayErrores) return;

    alEnviar({ asunto: valores.asunto.trim(), mensaje: valores.mensaje.trim() });
  }

  return (
    <form className={estilos.formulario} onSubmit={alSoltar} noValidate>
      {fallo !== null && (
        <p className={formulario.fallo} role="alert">
          {fallo}
        </p>
      )}

      <div className={formulario.campo}>
        <label htmlFor="asunto">Asunto</label>
        <input
          id="asunto"
          name="asunto"
          type="text"
          maxLength={MAXIMO_ASUNTO}
          value={valores.asunto}
          onChange={(evento) => cambiar('asunto', evento.target.value)}
          aria-invalid={enviado && errores.asunto !== undefined}
          aria-describedby={
            enviado && errores.asunto !== undefined ? 'error-asunto' : 'contador-asunto'
          }
        />
        <small id="contador-asunto" className={formulario.ayuda} aria-live="polite">
          {valores.asunto.length} de {MAXIMO_ASUNTO} caracteres
        </small>
        {/* El mensaje solo aparece tras intentar enviar: su markAllAsTouched(). */}
        {enviado && errores.asunto !== undefined && (
          <small id="error-asunto" className={formulario.error}>
            {errores.asunto}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="mensaje">Mensaje</label>
        <textarea
          id="mensaje"
          name="mensaje"
          rows={5}
          maxLength={MAXIMO_MENSAJE}
          value={valores.mensaje}
          onChange={(evento) => cambiar('mensaje', evento.target.value)}
          aria-invalid={enviado && errores.mensaje !== undefined}
          aria-describedby={
            enviado && errores.mensaje !== undefined ? 'error-mensaje' : 'contador-mensaje'
          }
        />
        <small id="contador-mensaje" className={formulario.ayuda} aria-live="polite">
          {valores.mensaje.length} de {MAXIMO_MENSAJE} caracteres
        </small>
        {enviado && errores.mensaje !== undefined && (
          <small id="error-mensaje" className={formulario.error}>
            {errores.mensaje}
          </small>
        )}
      </div>

      {/* Deshabilitado mientras se envía: sin esto se pulsa dos veces. */}
      <button className={formulario.primario} type="submit" disabled={enviando}>
        {enviando ? 'Enviando…' : 'Enviar el mensaje'}
      </button>
    </form>
  );
}
