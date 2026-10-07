import { useEffect, useRef } from 'react';
import estilos from './ConfirmarBorrado.module.css';

interface Props {
  /** Qué se va a borrar, para que el aviso nombre la cosa y no «el elemento». */
  readonly titulo: string;
  readonly texto: string;
  readonly ocupado: boolean;
  /**
   * El botón, cuando lo que no se deshace no es borrar: suspender una cuenta
   * también pide este paso. Por defecto, «Sí, eliminar».
   */
  readonly etiquetaConfirmar?: string;
  readonly etiquetaOcupado?: string;
  readonly alConfirmar: () => void;
  readonly alCancelar: () => void;
}

/**
 * El paso previo a un borrado, que no se deshace.
 *
 * Es un `<dialog>` del navegador y no un `confirm()`: el nativo bloquea la
 * pestaña entera, no se puede dar estilo y no deja decir qué se está borrando.
 * Abrirlo con `showModal()` trae gratis lo que costaría escribir a mano: el
 * foco atrapado dentro, `Esc` para salir y el resto de la página inerte.
 */
export function ConfirmarBorrado({
  titulo,
  texto,
  ocupado,
  etiquetaConfirmar = 'Sí, eliminar',
  etiquetaOcupado = 'Eliminando…',
  alConfirmar,
  alCancelar,
}: Props) {
  const dialogo = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    // `showModal()` no es un atributo: solo existe como llamada, y hay que
    // hacerla después de que el elemento esté en el documento.
    dialogo.current?.showModal();
  }, []);

  return (
    <dialog
      ref={dialogo}
      className={estilos.dialogo}
      // `Esc` cierra el diálogo por su cuenta; sin esto el estado de quien lo
      // abrió seguiría creyendo que está abierto.
      onCancel={alCancelar}
      aria-labelledby="titulo-confirmar"
    >
      <h2 id="titulo-confirmar" className={estilos.titulo}>
        {titulo}
      </h2>
      <p className={estilos.texto}>{texto}</p>

      <div className={estilos.acciones}>
        <button type="button" className={estilos.cancelar} disabled={ocupado} onClick={alCancelar}>
          Cancelar
        </button>
        <button type="button" className={estilos.borrar} disabled={ocupado} onClick={alConfirmar}>
          {ocupado ? etiquetaOcupado : etiquetaConfirmar}
        </button>
      </div>
    </dialog>
  );
}
