import { useEffect, useRef, useState, type ChangeEvent, type RefObject } from 'react';
import { borrarFoto, subirFoto } from '../api/negocios';
import type { FotoNegocio } from '../types/negocio';
import estilos from './CargaDeFotos.module.css';

/** B9: seis por negocio, cinco megas cada una, y solo JPG o PNG. */
const MAXIMO_FOTOS = 6;
const MAXIMO_BYTES = 5 * 1024 * 1024;
const TIPOS: readonly string[] = ['image/jpeg', 'image/png'];

/**
 * Una imagen del paso, antes o después de subirla.
 *
 * Es una unión discriminada porque los dos casos no tienen los mismos datos: la
 * que todavía está en el navegador no tiene identificador del servidor, y la
 * que ya subió no necesita el fichero. `clave` es local y solo hace de `key`,
 * para que la fila no se remonte al pasar de un estado al otro.
 */
type Imagen =
  | {
      readonly estado: 'LOCAL';
      readonly clave: number;
      readonly archivo: File;
      readonly vistaPrevia: string;
    }
  | {
      readonly estado: 'SUBIDA';
      readonly clave: number;
      readonly nombre: string;
      readonly foto: FotoNegocio;
    };

/**
 * Por qué un fichero no sirve, o `null` si sirve.
 *
 * Se comprueba **antes** de enviar: una imagen de 6 MB viaja entera por la red
 * para que el backend la rechace al llegar, y quien la subió espera todo ese
 * rato para que le digan que no. Aquí el aviso es inmediato.
 *
 * Comprobado con `curl`: el backend rechaza igual la de 5,95 MB —regla de
 * dominio— y la de 7,33 —límite del contenedor—, y **las dos con el mismo
 * mensaje**, así que la diferencia no es el mensaje sino la espera.
 */
function revisar(archivo: File): string | null {
  if (!TIPOS.includes(archivo.type)) return 'tiene que ser JPG o PNG';
  if (archivo.size > MAXIMO_BYTES) return 'pesa más de 5 MB';
  return null;
}

interface Props {
  /** Se llama al cerrar el paso, con fotos o sin ellas. */
  readonly alTerminar: () => void;
  /** El mismo título que enfoca el asistente al cambiar de paso. */
  readonly encabezado: RefObject<HTMLHeadingElement | null>;
}

/**
 * El cuarto paso del asistente, ya con la sesión abierta.
 *
 * El negocio existe desde el paso anterior, así que aquí nada es obligatorio:
 * saltarse este paso deja el registro igual de completo, solo que sin galería.
 */
export function CargaDeFotos({ alTerminar, encabezado }: Props) {
  const [imagenes, setImagenes] = useState<readonly Imagen[]>([]);
  const [rechazados, setRechazados] = useState<readonly string[]>([]);
  const [subiendo, setSubiendo] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  const siguienteClave = useRef(1);
  // El efecto de desmontaje necesita la lista de ahora, no la del primer render,
  // y una ref no se puede escribir mientras se renderiza: se sincroniza aquí.
  const vigentes = useRef<readonly Imagen[]>(imagenes);
  useEffect(() => {
    vigentes.current = imagenes;
  }, [imagenes]);

  // Cada vista previa reserva memoria hasta que se libera. Al salir del paso no
  // queda ningún componente que lo haga, así que se hace aquí.
  useEffect(() => {
    return () => {
      for (const imagen of vigentes.current) {
        if (imagen.estado === 'LOCAL') URL.revokeObjectURL(imagen.vistaPrevia);
      }
    };
  }, []);

  const porSubir = imagenes.filter((imagen) => imagen.estado === 'LOCAL');
  const subidas = imagenes.filter((imagen) => imagen.estado === 'SUBIDA');
  const huecos = MAXIMO_FOTOS - imagenes.length;

  function elegir(evento: ChangeEvent<HTMLInputElement>) {
    const elegidos = Array.from(evento.target.files ?? []);
    // El mismo fichero dos veces seguidas no dispara `change` si no se limpia.
    evento.target.value = '';
    if (elegidos.length === 0) return;

    setFallo(null);
    const nuevas: Imagen[] = [];
    const motivos: string[] = [];

    for (const archivo of elegidos) {
      const motivo = revisar(archivo);
      if (motivo !== null) {
        motivos.push(`«${archivo.name}» ${motivo}`);
        continue;
      }
      if (imagenes.length + nuevas.length >= MAXIMO_FOTOS) {
        motivos.push(`«${archivo.name}» no cabe: la galería admite ${MAXIMO_FOTOS} fotos`);
        continue;
      }
      nuevas.push({
        estado: 'LOCAL',
        clave: siguienteClave.current++,
        archivo,
        vistaPrevia: URL.createObjectURL(archivo),
      });
    }

    setImagenes((lista) => [...lista, ...nuevas]);
    setRechazados(motivos);
  }

  function quitarLocal(clave: number) {
    setImagenes((lista) => {
      const fuera = lista.find((imagen) => imagen.clave === clave);
      if (fuera !== undefined && fuera.estado === 'LOCAL') URL.revokeObjectURL(fuera.vistaPrevia);
      return lista.filter((imagen) => imagen.clave !== clave);
    });
  }

  async function quitarSubida(clave: number, fotoId: number) {
    setFallo(null);
    try {
      await borrarFoto(fotoId);
      // El backend recoloca las que quedan: la portada pasa a ser la siguiente.
      setImagenes((lista) =>
        lista
          .filter((imagen) => imagen.clave !== clave)
          .map((imagen, posicion) =>
            imagen.estado === 'SUBIDA'
              ? { ...imagen, foto: { ...imagen.foto, orden: posicion, principal: posicion === 0 } }
              : imagen,
          ),
      );
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo quitar la foto';
      setFallo(mensaje);
    }
  }

  /**
   * Sube las pendientes **de una en una y en orden**.
   *
   * En paralelo llegarían desordenadas y la portada sería la que ganara la
   * carrera; el backend numera por orden de llegada y la primera es la principal
   * (B9).
   */
  async function subirTodas() {
    setFallo(null);
    setSubiendo(true);

    try {
      for (const pendiente of porSubir) {
        const foto = await subirFoto(pendiente.archivo);
        URL.revokeObjectURL(pendiente.vistaPrevia);
        setImagenes((lista) =>
          lista.map((imagen) =>
            imagen.clave === pendiente.clave
              ? {
                  estado: 'SUBIDA',
                  clave: pendiente.clave,
                  nombre: pendiente.archivo.name,
                  foto,
                }
              : imagen,
          ),
        );
      }
    } catch (error: unknown) {
      // Se para en la que falló: las anteriores ya están subidas y se quedan.
      const mensaje = error instanceof Error ? error.message : 'No se pudo subir la foto';
      setFallo(`${mensaje}. Las anteriores sí se subieron.`);
    } finally {
      setSubiendo(false);
    }
  }

  return (
    <>
      <h2 className={estilos.titulo} ref={encabezado} tabIndex={-1}>
        Las fotos
      </h2>
      <p className={estilos.entrada}>
        Tu negocio ya está creado y en revisión. Añadir fotos ahora es opcional: la primera será la
        portada, y puedes subir hasta {MAXIMO_FOTOS}.
      </p>

      {fallo !== null && (
        <p className={estilos.fallo} role="alert">
          {fallo}
        </p>
      )}

      {rechazados.length > 0 && (
        <ul className={estilos.rechazados} role="alert">
          {rechazados.map((motivo) => (
            <li key={motivo}>{motivo}</li>
          ))}
        </ul>
      )}

      <div className={estilos.campo}>
        <label htmlFor="fotos">Elegir imágenes</label>
        <input
          id="fotos"
          type="file"
          accept="image/jpeg,image/png"
          multiple
          disabled={huecos === 0 || subiendo}
          onChange={elegir}
          aria-describedby="ayuda-fotos"
        />
        <small id="ayuda-fotos" className={estilos.ayuda}>
          {huecos === 0
            ? `Ya tienes las ${MAXIMO_FOTOS} que caben`
            : `JPG o PNG, 5 MB como mucho · te quedan ${huecos}`}
        </small>
      </div>

      {imagenes.length === 0 ? (
        <p className={estilos.vacio}>
          Todavía no has elegido ninguna imagen. Puedes seguir sin fotos y añadirlas más adelante.
        </p>
      ) : (
        <ul className={estilos.galeria}>
          {imagenes.map((imagen, posicion) => (
            <li key={imagen.clave} className={estilos.miniatura}>
              <img
                src={imagen.estado === 'LOCAL' ? imagen.vistaPrevia : imagen.foto.url}
                alt={imagen.estado === 'LOCAL' ? imagen.archivo.name : imagen.nombre}
              />

              {/* La portada no se marca con un campo: es la primera por orden (B9). */}
              {posicion === 0 && <span className={estilos.portada}>Portada</span>}

              <span className={imagen.estado === 'SUBIDA' ? estilos.subida : estilos.pendiente}>
                {imagen.estado === 'SUBIDA' ? 'Subida' : 'Sin subir'}
              </span>

              <button
                type="button"
                className={estilos.quitar}
                disabled={subiendo}
                onClick={() =>
                  imagen.estado === 'LOCAL'
                    ? quitarLocal(imagen.clave)
                    : quitarSubida(imagen.clave, imagen.foto.id)
                }
              >
                Quitar
                <span className={estilos.oculto}>
                  {' '}
                  {imagen.estado === 'LOCAL' ? imagen.archivo.name : imagen.nombre}
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className={estilos.acciones}>
        {porSubir.length > 0 && (
          <button
            type="button"
            className={estilos.primario}
            disabled={subiendo}
            onClick={subirTodas}
          >
            {subiendo
              ? 'Subiendo…'
              : `Subir ${porSubir.length === 1 ? 'la foto' : `las ${porSubir.length} fotos`}`}
          </button>
        )}

        <button
          type="button"
          className={porSubir.length > 0 ? estilos.secundario : estilos.primario}
          disabled={subiendo}
          onClick={alTerminar}
        >
          {subidas.length > 0 ? 'Ir a mi negocio' : 'Seguir sin fotos'}
        </button>
      </div>

      {porSubir.length > 0 && (
        <p className={estilos.aviso} role="status">
          Te quedan {porSubir.length} imágenes sin subir: si sales ahora, no se guardan.
        </p>
      )}
    </>
  );
}
