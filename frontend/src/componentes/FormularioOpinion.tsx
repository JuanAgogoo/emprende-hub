import { useState, type FormEvent } from 'react';
import { EstrellasElegibles } from './Estrellas';
import { MAXIMO_COMENTARIO, type DatosDeOpinion } from '../types/opinion';
import formulario from '../paginas/Formulario.module.css';
import estilos from './FormularioOpinion.module.css';

interface Props {
  /** Los valores de partida. Con ellos, el formulario edita en vez de publicar. */
  readonly inicial?: DatosDeOpinion;
  readonly enviando: boolean;
  /** El fallo que devolvió la API, si el último intento no salió. */
  readonly fallo: string | null;
  readonly alEnviar: (datos: DatosDeOpinion) => void;
  /** Solo al editar: dejar los cambios sin guardar. */
  readonly alCancelar?: () => void;
}

interface Valores {
  /** 0 mientras no se ha elegido estrella. La API solo acepta de 1 a 5. */
  readonly calificacion: number;
  readonly comentario: string;
}

/** Lo que dice el botón de enviar, que es lo único que cambia al editar. */
function textoDelBoton(editando: boolean, enviando: boolean): string {
  if (editando) return enviando ? 'Guardando…' : 'Guardar cambios';
  return enviando ? 'Publicando…' : 'Publicar opinión';
}

/**
 * Validación de forma, fuera del componente y sin librería, como en los dos
 * registros. La del comentario es la misma que la del backend, para que el
 * `400` sea la excepción y no la norma.
 */
function validar(valores: Valores): Partial<Record<keyof Valores, string>> {
  const errores: Partial<Record<keyof Valores, string>> = {};

  if (valores.calificacion < 1 || valores.calificacion > 5) {
    errores.calificacion = 'Elige de 1 a 5 estrellas';
  }
  if (valores.comentario.length > MAXIMO_COMENTARIO) {
    errores.comentario = `El comentario no puede pasar de ${MAXIMO_COMENTARIO} caracteres`;
  }

  return errores;
}

/**
 * Calificar y escribir la reseña. Es una sola pieza porque no hay forma de
 * publicar sin nota: `calificacion` es obligatoria y el comentario no.
 *
 * **El mismo formulario publica y edita.** Con `inicial` empieza con la reseña
 * puesta y guarda los cambios; sin él, empieza vacío y publica. Es lo único que
 * cambia entre los dos casos, así que no hay dos formularios que mantener.
 */
export function FormularioOpinion({ inicial, enviando, fallo, alEnviar, alCancelar }: Props) {
  const editando = inicial !== undefined;
  const [valores, setValores] = useState<Valores>({
    calificacion: inicial?.calificacion ?? 0,
    comentario: inicial?.comentario ?? '',
  });
  const [enviado, setEnviado] = useState(false);

  const errores = validar(valores);
  const hayErrores = Object.keys(errores).length > 0;
  const escritos = valores.comentario.length;

  function alSoltar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    if (hayErrores) return;

    const comentario = valores.comentario.trim();
    // Un comentario vacío no se manda: el backend distingue ausente de vacío.
    alEnviar({
      calificacion: valores.calificacion,
      comentario: comentario === '' ? undefined : comentario,
    });
  }

  return (
    <form className={estilos.formulario} onSubmit={alSoltar} noValidate>
      {fallo !== null && (
        <p className={formulario.fallo} role="alert">
          {fallo}
        </p>
      )}

      <EstrellasElegibles
        valor={valores.calificacion}
        alElegir={(calificacion) => setValores((previos) => ({ ...previos, calificacion }))}
        invalido={enviado && errores.calificacion !== undefined}
        describedBy={
          enviado && errores.calificacion !== undefined ? 'error-calificacion' : undefined
        }
      />
      {/* El mensaje solo aparece tras intentar enviar: su markAllAsTouched(). */}
      {enviado && errores.calificacion !== undefined && (
        <small id="error-calificacion" className={formulario.error}>
          {errores.calificacion}
        </small>
      )}

      <div className={formulario.campo}>
        <label htmlFor="comentario">Tu comentario (opcional)</label>
        <textarea
          id="comentario"
          rows={4}
          maxLength={MAXIMO_COMENTARIO}
          value={valores.comentario}
          onChange={(evento) =>
            setValores((previos) => ({ ...previos, comentario: evento.target.value }))
          }
          aria-describedby="contador-comentario"
        />
        {/* El contador a la vista, como en la descripción del negocio. */}
        <small id="contador-comentario" className={formulario.ayuda} aria-live="polite">
          {escritos} de {MAXIMO_COMENTARIO} caracteres
        </small>
      </div>

      <div className={estilos.acciones}>
        {/* Deshabilitado mientras se envía: sin esto se pulsa dos veces. */}
        <button className={formulario.primario} type="submit" disabled={enviando}>
          {textoDelBoton(editando, enviando)}
        </button>

        {alCancelar !== undefined && (
          <button
            className={estilos.secundario}
            type="button"
            onClick={alCancelar}
            disabled={enviando}
          >
            Cancelar
          </button>
        )}
      </div>
    </form>
  );
}
