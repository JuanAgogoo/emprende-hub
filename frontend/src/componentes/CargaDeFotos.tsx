import { useEffect, useRef, useState, type ChangeEvent, type RefObject } from 'react';
import { borrarFoto, reordenarFotos, subirFoto } from '../api/negocios';
import { ConfirmarBorrado } from './ConfirmarBorrado';
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
  /** La galería tal como está hoy. Vacía en el asistente, que empieza de cero. */
  readonly iniciales: readonly FotoNegocio[];
  /**
   * Cierra el paso del asistente, con fotos o sin ellas.
   *
   * Es lo único que distingue los dos sitios donde vive esta galería: en el
   * asistente hay un paso que terminar y por eso viene con su título y su
   * botón; en el panel del negocio la sección ya pone el suyo.
   */
  readonly alTerminar?: () => void;
  /** El título que enfoca el asistente al cambiar de paso. */
  readonly encabezado?: RefObject<HTMLHeadingElement | null>;
}

/**
 * La galería del negocio: subir, quitar, reordenar y elegir portada (B9).
 *
 * La usan el cuarto paso del registro y el panel del dueño. Son la misma
 * pantalla con distinto marco, así que el componente es uno solo y el marco
 * llega por `alTerminar`.
 */
export function CargaDeFotos({ iniciales, alTerminar, encabezado }: Props) {
  const [imagenes, setImagenes] = useState<readonly Imagen[]>(() =>
    iniciales.map((foto, posicion) => ({
      estado: 'SUBIDA',
      clave: foto.id,
      nombre: `Foto ${posicion + 1}`,
      foto,
    })),
  );
  /** Lo que se acaba de hacer, que la historia pide confirmar en pantalla. */
  const [aviso, setAviso] = useState<string | null>(null);
  const [porQuitar, setPorQuitar] = useState<Imagen | null>(null);
  const [rechazados, setRechazados] = useState<readonly string[]>([]);
  const [subiendo, setSubiendo] = useState(false);
  const [reordenando, setReordenando] = useState(false);
  const [borrando, setBorrando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  // Las que ya estaban usan su propio identificador como clave, así que las
  // nuevas empiezan por encima del mayor: si no, una local podría chocar con
  // una subida y React remontaría la fila equivocada.
  const siguienteClave = useRef(iniciales.reduce((mayor, foto) => Math.max(mayor, foto.id), 0) + 1);
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
  /** Mientras algo esté en marcha, ningún botón de la galería responde. */
  const ocupado = subiendo || reordenando || borrando;
  /** Con más de una foto y ninguna pendiente de subir, se puede ordenar. */
  const puedeOrdenar = porSubir.length === 0 && imagenes.length > 1;

  function elegir(evento: ChangeEvent<HTMLInputElement>) {
    const elegidos = Array.from(evento.target.files ?? []);
    // El mismo fichero dos veces seguidas no dispara `change` si no se limpia.
    evento.target.value = '';
    if (elegidos.length === 0) return;

    setFallo(null);
    setAviso(null);
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

  /**
   * Quita una foto ya subida, **después** de que se confirme.
   *
   * El borrado no se deshace: la imagen desaparece del servidor y con ella su
   * fichero. Por eso no cuelga del botón directamente, sino del diálogo.
   */
  async function quitarSubida(clave: number, fotoId: number) {
    setFallo(null);
    setBorrando(true);
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
      setAviso('Foto eliminada. La portada es la primera de las que quedan.');
      setPorQuitar(null);
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo quitar la foto';
      setFallo(mensaje);
    } finally {
      setBorrando(false);
    }
  }

  /**
   * Mueve una foto a otra posición y guarda el orden entero.
   *
   * Solo se ofrece cuando no queda ninguna sin subir: mezclar una foto que ya
   * tiene sitio en el servidor con otra que aún no existe haría que «la primera»
   * no significara lo mismo en los dos lados.
   *
   * El backend devuelve la galería recolocada y se toma esa, en vez de adivinar
   * el resultado aquí: si algo falla, la pantalla no se queda contando otra cosa.
   */
  async function mover(desde: number, hasta: number) {
    const orden = imagenes.flatMap((imagen) =>
      imagen.estado === 'SUBIDA' ? [imagen.foto.id] : [],
    );
    const movida = orden[desde];
    if (movida === undefined) return;
    orden.splice(desde, 1);
    orden.splice(hasta, 0, movida);

    setFallo(null);
    setReordenando(true);
    try {
      const galeria = await reordenarFotos(orden);
      setImagenes(
        galeria.map((foto, posicion) => ({
          estado: 'SUBIDA',
          clave: posicion,
          nombre: `Foto ${posicion + 1}`,
          foto,
        })),
      );
    } catch (error: unknown) {
      const mensaje = error instanceof Error ? error.message : 'No se pudo cambiar el orden';
      setFallo(mensaje);
    } finally {
      setReordenando(false);
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
      setAviso(
        porSubir.length === 1
          ? 'Foto subida y guardada en tu galería.'
          : `${porSubir.length} fotos subidas y guardadas en tu galería.`,
      );
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
      {alTerminar !== undefined && (
        <>
          <h2 className={estilos.titulo} ref={encabezado} tabIndex={-1}>
            Las fotos
          </h2>
          <p className={estilos.entrada}>
            Tu negocio ya está creado. Añadir fotos ahora es opcional: la primera será la portada,
            y puedes subir hasta {MAXIMO_FOTOS}. Al terminar entrarás con tu correo y tu
            contraseña.
          </p>
        </>
      )}

      {aviso !== null && (
        <p className={estilos.aviso} role="status">
          {aviso}
        </p>
      )}

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
        <label htmlFor="fotos">Fotos del negocio</label>

        {/* Igual que en el escaparate: la etiqueta hace de botón y el input de
            verdad se queda invisible pero enfocable con el tabulador. */}
        <div className={estilos.zonaFoto}>
          <label className={estilos.botonArchivo} htmlFor="fotos" aria-disabled={huecos === 0}>
            {imagenes.length === 0 ? 'Elegir imágenes' : 'Añadir más imágenes'}
          </label>
          <input
            id="fotos"
            className={estilos.archivoOculto}
            type="file"
            accept="image/jpeg,image/png"
            multiple
            disabled={huecos === 0 || subiendo}
            onChange={elegir}
            aria-describedby="ayuda-fotos"
          />
        </div>

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
          {imagenes.map((imagen, posicion) => {
            const nombre = imagen.estado === 'LOCAL' ? imagen.archivo.name : imagen.nombre;

            return (
            <li key={imagen.clave} className={estilos.miniatura}>
              {/* La marca va dentro del marco de la imagen y no suelta sobre la
                  fila: así se ancla al pie de la foto y no cae encima del texto
                  alternativo, que es lo que se ve cuando la imagen no carga. */}
              <div className={estilos.marco}>
                <img
                  src={imagen.estado === 'LOCAL' ? imagen.vistaPrevia : imagen.foto.url}
                  alt={nombre}
                />

                {/* La portada no se marca con un campo: es la primera por orden (B9). */}
                {posicion === 0 && <span className={estilos.portada}>Portada</span>}
              </div>

              <span className={imagen.estado === 'SUBIDA' ? estilos.subida : estilos.pendiente}>
                {imagen.estado === 'SUBIDA' ? 'Subida' : 'Sin subir'}
              </span>

              {/* Ordenar solo cuando todas están subidas: hasta entonces «la
                  primera» no significaría lo mismo aquí que en el servidor. */}
              {puedeOrdenar && (
                <div className={estilos.orden}>
                  <button
                    type="button"
                    className={estilos.mover}
                    disabled={posicion === 0 || ocupado}
                    onClick={() => mover(posicion, posicion - 1)}
                    aria-label={`Mover ${nombre} una posición antes`}
                  >
                    <span aria-hidden="true">←</span>
                  </button>
                  <button
                    type="button"
                    className={estilos.mover}
                    disabled={posicion === imagenes.length - 1 || ocupado}
                    onClick={() => mover(posicion, posicion + 1)}
                    aria-label={`Mover ${nombre} una posición después`}
                  >
                    <span aria-hidden="true">→</span>
                  </button>
                </div>
              )}

              {puedeOrdenar && posicion !== 0 && (
                <button
                  type="button"
                  className={estilos.hacerPortada}
                  disabled={ocupado}
                  onClick={() => mover(posicion, 0)}
                >
                  Hacer portada
                  <span className={estilos.oculto}> ({nombre})</span>
                </button>
              )}

              <button
                type="button"
                className={estilos.quitar}
                disabled={ocupado}
                onClick={() =>
                  imagen.estado === 'LOCAL'
                    ? quitarLocal(imagen.clave)
                    : setPorQuitar(imagen)
                }
              >
                Quitar
                <span className={estilos.oculto}> {nombre}</span>
              </button>
            </li>
            );
          })}

          {/* Los huecos que quedan, dibujados: así se ve cuántas caben todavía
              sin tener que leer el contador de arriba. */}
          {Array.from({ length: huecos }, (_, posicion) => (
            <li key={`hueco-${posicion}`} className={estilos.hueco} aria-hidden="true">
              <span>Libre</span>
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

        {alTerminar !== undefined && (
          <button
            type="button"
            className={porSubir.length > 0 ? estilos.secundario : estilos.primario}
            disabled={subiendo}
            onClick={alTerminar}
          >
            {subidas.length > 0 ? 'Terminar el registro' : 'Seguir sin fotos'}
          </button>
        )}
      </div>

      {porSubir.length > 0 && (
        <p className={estilos.avisoPendiente} role="status">
          Te quedan {porSubir.length === 1 ? '1 imagen' : `${porSubir.length} imágenes`} sin subir:
          si sales ahora, no se guardan. Podrás ordenarlas y elegir la portada en cuanto estén
          subidas.
        </p>
      )}

      {/* Quitar una foto del servidor no se deshace: se confirma antes. */}
      {porQuitar !== null && porQuitar.estado === 'SUBIDA' && (
        <ConfirmarBorrado
          titulo="¿Eliminar esta foto?"
          texto="Desaparece de tu galería y del perfil público. Si era la portada, pasa a serlo la siguiente."
          ocupado={borrando}
          alConfirmar={() => quitarSubida(porQuitar.clave, porQuitar.foto.id)}
          alCancelar={() => setPorQuitar(null)}
        />
      )}
    </>
  );
}
