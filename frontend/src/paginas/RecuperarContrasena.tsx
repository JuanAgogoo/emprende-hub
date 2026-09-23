import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { solicitarRecuperacion } from '../api/auth';
import { useTitulo } from '../titulo';
import estilos from './Formulario.module.css';

/**
 * Las mismas reglas de forma que el correo de los registros. Que el correo sea
 * válido no quiere decir que tenga cuenta: eso no se contesta nunca.
 */
function validar(correo: string): string | undefined {
  if (correo.trim() === '') return 'El correo es obligatorio';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo.trim()))
    return 'El correo no tiene un formato válido';
  return undefined;
}

/**
 * Pedir el enlace para recuperar la contraseña.
 *
 * **El mensaje de después es siempre el mismo**, tenga cuenta ese correo o no.
 * De nada serviría que el backend responda 200 a los dos casos si la pantalla
 * los distingue por él: sería la misma forma de averiguar qué direcciones están
 * registradas, solo que un paso más allá.
 */
export function RecuperarContrasena() {
  useTitulo('Recuperar la contraseña');

  const [correo, setCorreo] = useState('');
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [pedido, setPedido] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  const error = validar(correo);

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    setFallo(null);
    if (error !== undefined) return;

    setEnviando(true);
    try {
      await solicitarRecuperacion(correo.trim());
      setPedido(true);
    } catch (fallido: unknown) {
      // Solo se llega aquí si la API no contesta o el correo está mal formado:
      // una cuenta que no existe responde 200 como cualquier otra.
      const mensaje = fallido instanceof Error ? fallido.message : 'No se pudo pedir el enlace';
      setFallo(`${mensaje}. Comprueba que la API esté funcionando.`);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <div className={estilos.tarjeta}>
        <h1 className={estilos.titulo}>Recuperar la contraseña</h1>

        {pedido ? (
          <>
            <p className={estilos.aviso} role="status">
              Si ese correo tiene una cuenta, le acabamos de enviar un enlace para elegir una
              contraseña nueva. Caduca en 30 minutos y sirve una sola vez.
            </p>
            <p className={estilos.entrada}>
              ¿No te llega? Revisa que sea la dirección con la que te registraste y vuelve a
              pedirlo.
            </p>
            <button
              className={estilos.primario}
              type="button"
              onClick={() => {
                setPedido(false);
                setEnviado(false);
              }}
            >
              Pedirlo otra vez
            </button>
          </>
        ) : (
          <>
            <p className={estilos.entrada}>
              Escribe el correo de tu cuenta y te enviamos un enlace para elegir una contraseña
              nueva.
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
                  name="correo"
                  type="email"
                  autoComplete="email"
                  value={correo}
                  onChange={(evento) => setCorreo(evento.target.value)}
                  aria-invalid={enviado && error !== undefined}
                  aria-describedby={enviado && error !== undefined ? 'error-correo' : undefined}
                />
                {/* El mensaje solo aparece tras intentar enviar. */}
                {enviado && error !== undefined && (
                  <small id="error-correo" className={estilos.error}>
                    {error}
                  </small>
                )}
              </div>

              <button className={estilos.primario} type="submit" disabled={enviando}>
                {enviando ? 'Enviando el enlace…' : 'Enviarme el enlace'}
              </button>
            </form>
          </>
        )}

        <p className={estilos.pie}>
          ¿Te has acordado? <Link to="/entrar">Entrar</Link>
        </p>
      </div>
    </div>
  );
}
