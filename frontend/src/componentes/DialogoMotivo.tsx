import { useEffect, useRef, useState, type FormEvent } from 'react';
import estilos from './DialogoMotivo.module.css';

/** El mismo tope que el backend: `DecisionRequest` no admite más. */
const MAXIMO = 1000;

interface Props {
  readonly titulo: string;
  /** A quién le llega el motivo y qué va a pasar, en una frase. */
  readonly texto: string;
  readonly etiquetaConfirmar: string;
  readonly ocupado: boolean;
  /** El fallo de la última petición, para decirlo dentro del propio diálogo. */
  readonly fallo: string | null;
  readonly alConfirmar: (motivo: string) => void;
  readonly alCancelar: () => void;
}

/** Validación de forma, fuera del componente como en el resto de formularios. */
function validar(motivo: string): string | null {
  if (motivo.trim() === '') return 'Escribe el motivo: es lo que va a leer la otra persona';
  if (motivo.length > MAXIMO) return `El motivo no puede pasar de ${MAXIMO} caracteres`;
  return null;
}

/**
 * Pide el motivo de una decisión del administrador antes de tomarla.
 *
 * Lo usan las tres decisiones que lo exigen: rechazar un negocio (HU-037),
 * rechazar un cambio propuesto y borrar una opinión denunciada. Es un
 * `<dialog>` con `showModal()`, igual que `ConfirmarBorrado`, por lo mismo: el
 * foco atrapado, `Esc` para salir y el resto de la página inerte vienen de
 * serie.
 */
export function DialogoMotivo({
  titulo,
  texto,
  etiquetaConfirmar,
  ocupado,
  fallo,
  alConfirmar,
  alCancelar,
}: Props) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [motivo, setMotivo] = useState('');
  const [enviado, setEnviado] = useState(false);

  useEffect(() => {
    dialogo.current?.showModal();
  }, []);

  const error = validar(motivo);

  function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    if (error !== null) return;
    alConfirmar(motivo.trim());
  }

  return (
    <dialog
      ref={dialogo}
      className={estilos.dialogo}
      onCancel={alCancelar}
      aria-labelledby="titulo-motivo"
    >
      <form className={estilos.formulario} onSubmit={alEnviar} noValidate>
        <h2 id="titulo-motivo" className={estilos.titulo}>
          {titulo}
        </h2>
        <p className={estilos.texto}>{texto}</p>

        {fallo !== null && (
          <p className={estilos.fallo} role="alert">
            {fallo}
          </p>
        )}

        <div className={estilos.campo}>
          <label htmlFor="motivo">Motivo</label>
          <textarea
            id="motivo"
            rows={4}
            value={motivo}
            onChange={(evento) => setMotivo(evento.target.value)}
            aria-invalid={enviado && error !== null}
            aria-describedby={enviado && error !== null ? 'error-motivo' : 'contador-motivo'}
          />
          {enviado && error !== null ? (
            <small id="error-motivo" className={estilos.error}>
              {error}
            </small>
          ) : (
            <small id="contador-motivo" className={estilos.contador}>
              {motivo.length} / {MAXIMO}
            </small>
          )}
        </div>

        <div className={estilos.acciones}>
          <button
            type="button"
            className={estilos.cancelar}
            disabled={ocupado}
            onClick={alCancelar}
          >
            Cancelar
          </button>
          <button type="submit" className={estilos.confirmar} disabled={ocupado}>
            {ocupado ? 'Enviando…' : etiquetaConfirmar}
          </button>
        </div>
      </form>
    </dialog>
  );
}
