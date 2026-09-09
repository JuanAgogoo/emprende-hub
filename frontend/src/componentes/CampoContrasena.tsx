import { useState } from 'react';
import { IconoOjo } from './IconoOjo';
// La hoja de los formularios, que es la que usan las tres pantallas donde
// aparece este campo. Las reglas se quedan ahí y no aquí porque tienen que
// ganarle en la cascada a la de los campos de texto, que vive en ese mismo
// fichero: separarlas dejaría el orden al azar y el texto pasaría bajo el ojo.
import estilos from '../paginas/Formulario.module.css';

interface Props {
  readonly id: string;
  /** Solo donde ya lo llevaba: ayuda a los gestores de contraseñas. */
  readonly name?: string;
  readonly valor: string;
  /** Lista cerrada: `new-password` al registrarse, `current-password` al entrar. */
  readonly autoComplete: 'new-password' | 'current-password';
  readonly invalido: boolean;
  readonly describedBy: string | undefined;
  readonly alCambiar: (valor: string) => void;
}

/**
 * El campo de contraseña, con el botón de revelarla.
 *
 * Es solo el control: la etiqueta y los mensajes se quedan en cada formulario,
 * porque cada uno dice cosas distintas.
 *
 * Se extrajo al aparecer el tercero —los dos registros y el acceso—, que es
 * cuando lo manda la regla de tres. Con dos seguían siendo dos bloques
 * repetidos, más baratos de leer que una abstracción prematura.
 */
export function CampoContrasena({
  id,
  name,
  valor,
  autoComplete,
  invalido,
  describedBy,
  alCambiar,
}: Props) {
  const [visible, setVisible] = useState(false);

  return (
    <div className={estilos.conRevelado}>
      <input
        id={id}
        name={name}
        type={visible ? 'text' : 'password'}
        autoComplete={autoComplete}
        value={valor}
        onChange={(evento) => alCambiar(evento.target.value)}
        aria-invalid={invalido}
        aria-describedby={describedBy}
      />
      {/* El nombre accesible dice qué hace al pulsarlo, no en qué estado está:
          el icono ya enseña el estado, y ponerlo también aquí hace que el lector
          de pantalla anuncie lo contrario de lo que se espera. */}
      <button
        type="button"
        className={estilos.ojo}
        onClick={() => setVisible((mostrada) => !mostrada)}
        aria-label={visible ? 'Ocultar la contraseña' : 'Mostrar la contraseña'}
      >
        <IconoOjo abierto={visible} />
      </button>
    </div>
  );
}
