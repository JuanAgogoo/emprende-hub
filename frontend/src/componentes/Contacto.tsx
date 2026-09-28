import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { enviarConsulta } from '../api/consultas';
import { obtenerMiNegocio } from '../api/negocios';
import { useSesion } from '../estado/SesionContext';
import { FormularioConsulta } from './FormularioConsulta';
import { casoImposible } from '../types/estadoCarga';
import type { DatosDeConsulta } from '../types/consulta';
import estilos from './Contacto.module.css';

interface Props {
  readonly negocioId: number;
}

/**
 * Quién mira, que es lo que decide si se dibuja el formulario.
 *
 * Se pregunta antes de dibujar nada, igual que en las opiniones: quien no puede
 * escribir no entendería el 400 que le devolvería el backend.
 */
type QuienMira =
  | { readonly estado: 'COMPROBANDO' }
  | { readonly estado: 'SIN_SESION' }
  | { readonly estado: 'ES_DUENO' }
  | { readonly estado: 'PUEDE_ESCRIBIR' }
  | { readonly estado: 'YA_ESCRIBIO' };

/**
 * Un 404 aquí no es un error: es que esa cuenta no tiene negocio.
 *
 * El bloque de opiniones hace esta misma pregunta por su cuenta. Son dos GET
 * pequeños y dos bloques parecidos, que se leen mejor que una abstracción
 * compartida entre dos piezas que no comparten nada más.
 */
async function negocioPropio(): Promise<number | null> {
  try {
    return (await obtenerMiNegocio()).id;
  } catch (error: unknown) {
    if (error instanceof ErrorApi && error.estado === 404) return null;
    throw error;
  }
}

/**
 * Escribirle al negocio desde su perfil (D1).
 *
 * Exige sesión, y el backend no deja escribir al propio buzón. Lo segundo se
 * comprueba aquí solo por comodidad de la interfaz: quien mande la petición a
 * mano se topa igual con el backend.
 */
export function Contacto({ negocioId }: Props) {
  const { sesion } = useSesion();

  const [quienMira, setQuienMira] = useState<QuienMira>({ estado: 'COMPROBANDO' });
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;

    if (sesion === null) {
      setQuienMira({ estado: 'SIN_SESION' });
      return;
    }

    setQuienMira({ estado: 'COMPROBANDO' });

    // Solo un emprendedor puede ser el dueño, así que a nadie más se le pregunta.
    if (sesion.rol !== 'EMPRENDEDOR') {
      setQuienMira({ estado: 'PUEDE_ESCRIBIR' });
      return;
    }

    negocioPropio()
      .then((mio) => {
        if (vigente) {
          setQuienMira({ estado: mio === negocioId ? 'ES_DUENO' : 'PUEDE_ESCRIBIR' });
        }
      })
      .catch(() => {
        // Si la comprobación falla no se esconde el formulario: se deja
        // intentarlo, y el backend dirá que no si es que no.
        if (vigente) setQuienMira({ estado: 'PUEDE_ESCRIBIR' });
      });

    return () => {
      vigente = false;
    };
  }, [negocioId, sesion]);

  async function escribir(datos: DatosDeConsulta) {
    setEnviando(true);
    setFallo(null);

    try {
      await enviarConsulta(negocioId, datos);
      setQuienMira({ estado: 'YA_ESCRIBIO' });
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo enviar el mensaje';
      setFallo(mensaje);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className={estilos.bloque}>
      <h2 className={estilos.titulo}>Escríbele al negocio</h2>

      <TuParte
        quienMira={quienMira}
        enviando={enviando}
        fallo={fallo}
        alEscribir={escribir}
        alEscribirOtro={() => setQuienMira({ estado: 'PUEDE_ESCRIBIR' })}
      />
    </section>
  );
}

interface PropsTuParte {
  readonly quienMira: QuienMira;
  readonly enviando: boolean;
  readonly fallo: string | null;
  readonly alEscribir: (datos: DatosDeConsulta) => void;
  readonly alEscribirOtro: () => void;
}

/** Lo que se le ofrece a quien mira, según quién sea. */
function TuParte({ quienMira, enviando, fallo, alEscribir, alEscribirOtro }: PropsTuParte) {
  // Volver aquí después de entrar: el login lo lee de `state`.
  const { pathname } = useLocation();

  switch (quienMira.estado) {
    // Mientras se comprueba no se enseña nada: dibujar el formulario y quitarlo
    // medio segundo después es peor que esperar a saberlo.
    case 'COMPROBANDO':
      return null;

    case 'SIN_SESION':
      return (
        <p className={estilos.aviso}>
          <Link to="/entrar" state={{ volverA: pathname }}>
            Entra con tu cuenta
          </Link>{' '}
          para escribirle. Tu nombre y tu correo se los damos nosotros, para que pueda
          responderte.
        </p>
      );

    // El backend tampoco lo deja: no tiene sentido escribirse al buzón propio.
    case 'ES_DUENO':
      return (
        <p className={estilos.aviso}>
          Este negocio es tuyo. Los mensajes que recibas los leerás en tu panel.
        </p>
      );

    case 'PUEDE_ESCRIBIR':
      return <FormularioConsulta enviando={enviando} fallo={fallo} alEnviar={alEscribir} />;

    // La confirmación en pantalla: el mensaje salió y quien lo mandó tiene que
    // saberlo sin ir a buscarlo a ninguna parte.
    case 'YA_ESCRIBIO':
      return (
        <div className={estilos.enviado} role="status">
          <p>
            <strong>Mensaje enviado.</strong> Le llega a su panel con tu nombre y tu correo, así
            que puede responderte directamente.
          </p>
          <button type="button" className={estilos.otro} onClick={alEscribirOtro}>
            Escribir otro
          </button>
        </div>
      );

    default:
      return casoImposible(quienMira);
  }
}
