import { useEffect, useRef, useState, type FormEvent } from 'react';
import { obtenerMotivosDenuncia } from '../api/catalogos';
import { denunciarOpinion } from '../api/denuncias';
import type { Opcion } from '../types/catalogo';
import type { MotivoDenuncia } from '../types/denuncia';
import type { Opinion } from '../types/opinion';
import estilos from './DialogoMotivo.module.css';

interface Props {
  readonly opinion: Opinion;
  readonly alDenunciar: () => void;
  readonly alCancelar: () => void;
}

/**
 * Denunciar una opinión ajena (C3): se elige el motivo de una lista cerrada
 * (C6), no se escribe. La opinión no se oculta; decide el administrador.
 *
 * Comparte el aspecto de `DialogoMotivo`, que es el mismo diálogo con un
 * campo distinto dentro.
 */
export function DialogoDenuncia({ opinion, alDenunciar, alCancelar }: Props) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const [motivos, setMotivos] = useState<readonly Opcion<MotivoDenuncia>[]>([]);
  const [elegido, setElegido] = useState<MotivoDenuncia | null>(null);
  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    dialogo.current?.showModal();
  }, []);

  useEffect(() => {
    let vigente = true;
    obtenerMotivosDenuncia()
      .then((lista) => {
        if (vigente) setMotivos(lista);
      })
      .catch(() => {
        if (vigente) setFallo('No se pudieron cargar los motivos. Cierra y vuelve a intentarlo.');
      });
    return () => {
      vigente = false;
    };
  }, []);

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    if (elegido === null) return;

    setEnviando(true);
    setFallo(null);
    try {
      await denunciarOpinion(opinion.id, elegido);
      alDenunciar();
    } catch (error: unknown) {
      // El 400 de «ya la denunciaste» trae su propio mensaje, y es el que sirve.
      setFallo(error instanceof Error ? error.message : 'No se pudo enviar la denuncia');
      setEnviando(false);
    }
  }

  const sinMotivo = enviado && elegido === null;

  return (
    <dialog
      ref={dialogo}
      className={estilos.dialogo}
      onCancel={alCancelar}
      aria-labelledby="titulo-denuncia"
    >
      <form className={estilos.formulario} onSubmit={alEnviar} noValidate>
        <h2 id="titulo-denuncia" className={estilos.titulo}>
          Denunciar la opinión de {opinion.autor}
        </h2>
        <p className={estilos.texto}>
          La opinión sigue publicada mientras la administración la revisa. Si incumple las normas,
          se borrará.
        </p>

        {fallo !== null && (
          <p className={estilos.fallo} role="alert">
            {fallo}
          </p>
        )}

        <fieldset
          className={estilos.motivos}
          aria-invalid={sinMotivo}
          aria-describedby={sinMotivo ? 'error-motivo-denuncia' : undefined}
        >
          <legend>¿Qué le pasa?</legend>
          {motivos.map((motivo) => (
            <label key={motivo.codigo} className={estilos.opcion}>
              <input
                type="radio"
                name="motivo-denuncia"
                checked={elegido === motivo.codigo}
                onChange={() => setElegido(motivo.codigo)}
              />
              {motivo.nombre}
            </label>
          ))}
        </fieldset>
        {sinMotivo && (
          <small id="error-motivo-denuncia" className={estilos.error}>
            Elige un motivo
          </small>
        )}

        <div className={estilos.acciones}>
          <button
            type="button"
            className={estilos.cancelar}
            disabled={enviando}
            onClick={alCancelar}
          >
            Cancelar
          </button>
          <button type="submit" className={estilos.confirmar} disabled={enviando}>
            {enviando ? 'Enviando…' : 'Denunciar'}
          </button>
        </div>
      </form>
    </dialog>
  );
}
