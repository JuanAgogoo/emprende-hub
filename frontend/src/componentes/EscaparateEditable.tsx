import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import {
  borrarProducto,
  cambiarDisponibilidad,
  cambiarFotoDeProducto,
  crearProducto,
  editarProducto,
} from '../api/negocios';
import { ConfirmarBorrado } from './ConfirmarBorrado';
import { precio as formatearPrecio } from '../formato';
import type { Producto } from '../types/negocio';
import formulario from '../paginas/Formulario.module.css';
import estilos from './EscaparateEditable.module.css';

/** Las mismas reglas de imagen que la galería (B9) y que exige F4. */
const MAXIMO_BYTES = 5 * 1024 * 1024;
const TIPOS: readonly string[] = ['image/jpeg', 'image/png'];

/**
 * El producto mientras se escribe.
 *
 * El precio vive como texto y no como número: un `<input>` vacío o a medio
 * teclear no es un número, y convertirlo en cada pulsación haría que borrar el
 * último dígito se leyera como un cero.
 */
interface Borrador {
  readonly nombre: string;
  readonly precio: string;
  readonly descripcion: string;
  readonly disponible: boolean;
}

const BORRADOR_VACIO: Borrador = {
  nombre: '',
  precio: '',
  descripcion: '',
  disponible: true,
};

type Errores = Readonly<Partial<Record<keyof Borrador | 'foto', string>>>;

/**
 * Los errores del producto, como función pura.
 *
 * `obligatoria` distingue los dos usos: al crear no hay producto sin foto (F4),
 * y al editar el producto ya tiene una, así que elegir otra es opcional. Lo que
 * no cambia es que **una foto elegida tiene que valer**, se haya pedido o no.
 */
function validar(borrador: Borrador, foto: File | null, obligatoria: boolean): Errores {
  const errores: Partial<Record<keyof Borrador | 'foto', string>> = {};

  const nombre = borrador.nombre.trim();
  if (nombre.length < 2 || nombre.length > 120) {
    errores.nombre = 'El nombre debe tener entre 2 y 120 caracteres';
  }

  const precio = borrador.precio.trim();
  if (precio.length === 0) {
    errores.precio = 'El precio es obligatorio';
  } else if (!/^\d+([.,]\d{1,2})?$/.test(precio)) {
    errores.precio = 'Solo el número, con dos decimales como mucho';
  }

  if (borrador.descripcion.trim().length > 500) {
    errores.descripcion = 'La descripción no puede pasar de 500 caracteres';
  }

  if (foto === null) {
    if (obligatoria) errores.foto = 'Cada producto necesita su foto';
  } else if (!TIPOS.includes(foto.type)) {
    errores.foto = 'La foto tiene que ser JPG o PNG';
  } else if (foto.size > MAXIMO_BYTES) {
    errores.foto = 'La foto pesa más de 5 MB';
  }

  return errores;
}

/** El precio se teclea con coma en español y el backend lo espera con punto. */
function aNumero(precio: string): number {
  return Number(precio.trim().replace(',', '.'));
}

function aBorrador(producto: Producto): Borrador {
  return {
    nombre: producto.nombre,
    precio: String(producto.precio),
    descripcion: producto.descripcion ?? '',
    disponible: producto.disponible,
  };
}

interface Props {
  readonly iniciales: readonly Producto[];
}

/**
 * El escaparate del dueño, editable (HU-034).
 *
 * Es un escaparate y no una tienda (F1): se listan, se añaden, se editan y se
 * borran, y el único estado que hay es «disponible» (F3). Nada de esto pasa por
 * revisión, así que lo que se toca aquí se ve en el perfil público enseguida.
 */
export function EscaparateEditable({ iniciales }: Props) {
  const [productos, setProductos] = useState<readonly Producto[]>(iniciales);
  const [editando, setEditando] = useState<number | null>(null);
  const [porBorrar, setPorBorrar] = useState<Producto | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);

  async function alternarDisponible(producto: Producto) {
    setFallo(null);
    setOcupado(true);
    try {
      const actualizado = await cambiarDisponibilidad(producto.id, !producto.disponible);
      setProductos((lista) => lista.map((p) => (p.id === producto.id ? actualizado : p)));
      setAviso(
        actualizado.disponible
          ? `«${actualizado.nombre}» vuelve a estar disponible.`
          : `«${actualizado.nombre}» queda marcado como no disponible.`,
      );
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo cambiar la disponibilidad');
    } finally {
      setOcupado(false);
    }
  }

  async function confirmarBorrado() {
    if (porBorrar === null) return;
    setFallo(null);
    setOcupado(true);
    try {
      await borrarProducto(porBorrar.id);
      setProductos((lista) => lista.filter((p) => p.id !== porBorrar.id));
      setAviso(`«${porBorrar.nombre}» ya no está en tu escaparate.`);
      setPorBorrar(null);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo eliminar el producto');
    } finally {
      setOcupado(false);
    }
  }

  function alGuardarEdicion(actualizado: Producto) {
    setProductos((lista) => lista.map((p) => (p.id === actualizado.id ? actualizado : p)));
    setEditando(null);
    setAviso(`«${actualizado.nombre}» quedó actualizado en tu perfil público.`);
  }

  function alCrear(nuevo: Producto) {
    setProductos((lista) => [...lista, nuevo]);
    setAviso(`«${nuevo.nombre}» ya aparece en tu perfil público.`);
  }

  return (
    <div className={estilos.escaparate}>
      {fallo !== null && (
        <p className={formulario.fallo} role="alert">
          {fallo}
        </p>
      )}

      {aviso !== null && (
        <p className={estilos.aviso} role="status">
          {aviso}
        </p>
      )}

      {productos.length === 0 ? (
        <p className={estilos.vacio}>
          Todavía no has añadido productos. Lo que pongas aquí es lo que ve quien entra en tu
          perfil.
        </p>
      ) : (
        <ul className={estilos.lista}>
          {productos.map((producto) =>
            editando === producto.id ? (
              <li key={producto.id} className={estilos.filaEditando}>
                <FormularioDeEdicion
                  producto={producto}
                  alGuardar={alGuardarEdicion}
                  alCancelar={() => setEditando(null)}
                />
              </li>
            ) : (
              <li key={producto.id} className={estilos.fila}>
                <img className={estilos.miniatura} src={producto.foto} alt="" loading="lazy" />

                <div className={estilos.datos}>
                  <span className={estilos.nombre}>{producto.nombre}</span>
                  <span className={estilos.precio}>{formatearPrecio(producto.precio)}</span>
                  {producto.descripcion !== null && (
                    <span className={estilos.descripcion}>{producto.descripcion}</span>
                  )}
                  {!producto.disponible && <span className={estilos.agotado}>No disponible</span>}
                </div>

                <div className={estilos.acciones}>
                  <button
                    type="button"
                    className={estilos.secundario}
                    disabled={ocupado}
                    onClick={() => alternarDisponible(producto)}
                  >
                    {producto.disponible ? 'Marcar no disponible' : 'Marcar disponible'}
                    <span className={estilos.oculto}> ({producto.nombre})</span>
                  </button>
                  <button
                    type="button"
                    className={estilos.secundario}
                    disabled={ocupado}
                    onClick={() => {
                      setAviso(null);
                      setEditando(producto.id);
                    }}
                  >
                    Editar
                    <span className={estilos.oculto}> {producto.nombre}</span>
                  </button>
                  <button
                    type="button"
                    className={estilos.destructivo}
                    disabled={ocupado}
                    onClick={() => {
                      setAviso(null);
                      setPorBorrar(producto);
                    }}
                  >
                    Eliminar
                    <span className={estilos.oculto}> {producto.nombre}</span>
                  </button>
                </div>
              </li>
            ),
          )}
        </ul>
      )}

      <FormularioDeAlta alCrear={alCrear} />

      {/* El borrado se lleva también la imagen del servidor y no se deshace. */}
      {porBorrar !== null && (
        <ConfirmarBorrado
          titulo={`¿Eliminar «${porBorrar.nombre}»?`}
          texto="Desaparece de tu escaparate y de tu perfil público, junto con su foto. No se puede deshacer."
          ocupado={ocupado}
          alConfirmar={confirmarBorrado}
          alCancelar={() => setPorBorrar(null)}
        />
      )}
    </div>
  );
}

interface PropsEdicion {
  readonly producto: Producto;
  readonly alGuardar: (producto: Producto) => void;
  readonly alCancelar: () => void;
}

/**
 * Editar un producto que ya existe, imagen incluida.
 *
 * Son dos peticiones y un solo botón: los campos de texto van en JSON por `PUT`
 * y la imagen, que es un binario, por su propio `PATCH`. Quien edita no tiene
 * por qué saberlo, y corregir una errata del nombre no obliga a volver a subir
 * la foto.
 */
function FormularioDeEdicion({ producto, alGuardar, alCancelar }: PropsEdicion) {
  const [borrador, setBorrador] = useState<Borrador>(aBorrador(producto));
  const [foto, setFoto] = useState<File | null>(null);
  const [vistaPrevia, setVistaPrevia] = useState<string | null>(null);
  const [intentado, setIntentado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  // Misma limpieza que el alta: cada vista previa reserva memoria hasta que se
  // libera, y al cerrar la edición no queda nadie que lo haga.
  const vigente = useRef<string | null>(null);
  useEffect(() => {
    vigente.current = vistaPrevia;
  }, [vistaPrevia]);
  useEffect(() => {
    return () => {
      if (vigente.current !== null) URL.revokeObjectURL(vigente.current);
    };
  }, []);

  const errores = validar(borrador, foto, false);

  function elegirFoto(evento: ChangeEvent<HTMLInputElement>) {
    const elegida = evento.target.files?.[0] ?? null;
    setFoto(elegida);
    setVistaPrevia((anterior) => {
      if (anterior !== null) URL.revokeObjectURL(anterior);
      return elegida === null ? null : URL.createObjectURL(elegida);
    });
  }

  async function guardar() {
    setIntentado(true);
    if (Object.keys(errores).length > 0) return;

    setFallo(null);
    setGuardando(true);
    try {
      let actualizado = await editarProducto(producto.id, {
        nombre: borrador.nombre.trim(),
        precio: aNumero(borrador.precio),
        descripcion:
          borrador.descripcion.trim().length === 0 ? undefined : borrador.descripcion.trim(),
        disponible: borrador.disponible,
      });
      // La foto después, y solo si se eligió otra: así la respuesta que se queda
      // es la que ya trae la imagen nueva.
      if (foto !== null) {
        actualizado = await cambiarFotoDeProducto(producto.id, foto);
      }
      alGuardar(actualizado);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo guardar el producto');
    } finally {
      setGuardando(false);
    }
  }

  const errorFoto = intentado ? errores.foto : undefined;

  return (
    <div className={estilos.edicion}>
      <p className={estilos.tituloEdicion}>Editando «{producto.nombre}»</p>

      {fallo !== null && (
        <p className={formulario.fallo} role="alert">
          {fallo}
        </p>
      )}

      <CamposDeProducto
        prefijo={`editar-${producto.id}`}
        borrador={borrador}
        errores={intentado ? errores : {}}
        alCambiar={(cambio) => setBorrador((actual) => ({ ...actual, ...cambio }))}
      />

      <div className={formulario.campo}>
        <label htmlFor={`editar-${producto.id}-foto`}>Foto</label>

        <div className={estilos.zonaFoto}>
          {/* La de ahora hasta que se elija otra: así se ve qué se sustituye. */}
          <img className={estilos.previa} src={vistaPrevia ?? producto.foto} alt="" />

          <label className={estilos.botonArchivo} htmlFor={`editar-${producto.id}-foto`}>
            {foto === null ? 'Cambiar la foto' : 'Elegir otra'}
          </label>

          <input
            id={`editar-${producto.id}-foto`}
            className={estilos.archivoOculto}
            type="file"
            accept="image/jpeg,image/png"
            onChange={elegirFoto}
            aria-invalid={errorFoto !== undefined}
            aria-describedby={
              errorFoto !== undefined
                ? `error-editar-${producto.id}-foto`
                : `ayuda-editar-${producto.id}-foto`
            }
          />
        </div>

        {errorFoto !== undefined ? (
          <small id={`error-editar-${producto.id}-foto`} className={formulario.error}>
            {errorFoto}
          </small>
        ) : (
          <small id={`ayuda-editar-${producto.id}-foto`} className={formulario.ayuda}>
            {foto === null
              ? 'Déjala como está o elige otra. JPG o PNG, 5 MB como mucho'
              : 'Al guardar sustituye a la anterior, que se borra'}
          </small>
        )}
      </div>

      <div className={estilos.accionesEdicion}>
        <button
          type="button"
          className={estilos.secundario}
          disabled={guardando}
          onClick={alCancelar}
        >
          Cancelar
        </button>
        <button
          type="button"
          className={formulario.primario}
          disabled={guardando}
          onClick={guardar}
        >
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </div>
  );
}

/** Alta de un producto, con su imagen obligatoria en la misma petición (F4). */
function FormularioDeAlta({ alCrear }: { readonly alCrear: (producto: Producto) => void }) {
  const [borrador, setBorrador] = useState<Borrador>(BORRADOR_VACIO);
  const [foto, setFoto] = useState<File | null>(null);
  const [vistaPrevia, setVistaPrevia] = useState<string | null>(null);
  const [intentado, setIntentado] = useState(false);
  const [creando, setCreando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);

  // Cada vista previa reserva memoria hasta que se libera. Se guarda en una ref
  // porque el efecto de limpieza necesita la de ahora, no la del primer render.
  const vigente = useRef<string | null>(null);
  useEffect(() => {
    vigente.current = vistaPrevia;
  }, [vistaPrevia]);
  useEffect(() => {
    return () => {
      if (vigente.current !== null) URL.revokeObjectURL(vigente.current);
    };
  }, []);

  const errores = validar(borrador, foto, true);

  function elegirFoto(evento: ChangeEvent<HTMLInputElement>) {
    const elegida = evento.target.files?.[0] ?? null;
    setFoto(elegida);
    setVistaPrevia((anterior) => {
      if (anterior !== null) URL.revokeObjectURL(anterior);
      return elegida === null ? null : URL.createObjectURL(elegida);
    });
  }

  async function anadir() {
    setIntentado(true);
    if (Object.keys(errores).length > 0 || foto === null) return;

    setFallo(null);
    setCreando(true);
    try {
      const nuevo = await crearProducto(
        {
          nombre: borrador.nombre.trim(),
          precio: aNumero(borrador.precio),
          descripcion:
            borrador.descripcion.trim().length === 0 ? undefined : borrador.descripcion.trim(),
          disponible: borrador.disponible,
        },
        foto,
      );
      alCrear(nuevo);

      setBorrador(BORRADOR_VACIO);
      setFoto(null);
      setVistaPrevia((anterior) => {
        if (anterior !== null) URL.revokeObjectURL(anterior);
        return null;
      });
      setIntentado(false);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo añadir el producto');
    } finally {
      setCreando(false);
    }
  }

  const errorFoto = intentado ? errores.foto : undefined;

  return (
    <fieldset className={estilos.alta}>
      <legend className={estilos.leyenda}>Añadir un producto</legend>

      {fallo !== null && (
        <p className={formulario.fallo} role="alert">
          {fallo}
        </p>
      )}

      <CamposDeProducto
        prefijo="nuevo"
        borrador={borrador}
        errores={intentado ? errores : {}}
        alCambiar={(cambio) => setBorrador((actual) => ({ ...actual, ...cambio }))}
      />

      <div className={formulario.campo}>
        <label htmlFor="nuevo-foto">Foto *</label>

        <div className={estilos.zonaFoto}>
          {vistaPrevia === null ? (
            <p className={estilos.sinFoto}>Todavía sin foto</p>
          ) : (
            <img className={estilos.previa} src={vistaPrevia} alt="" />
          )}

          {/* La etiqueta hace de botón y el input de verdad se queda invisible
              pero enfocable con el tabulador, igual que en el asistente. */}
          <label className={estilos.botonArchivo} htmlFor="nuevo-foto">
            {foto === null ? 'Elegir una foto' : 'Cambiar la foto'}
          </label>

          <input
            id="nuevo-foto"
            className={estilos.archivoOculto}
            type="file"
            accept="image/jpeg,image/png"
            onChange={elegirFoto}
            aria-invalid={errorFoto !== undefined}
            aria-describedby={errorFoto !== undefined ? 'error-nuevo-foto' : 'ayuda-nuevo-foto'}
          />
        </div>

        {errorFoto !== undefined ? (
          <small id="error-nuevo-foto" className={formulario.error}>
            {errorFoto}
          </small>
        ) : (
          <small id="ayuda-nuevo-foto" className={formulario.ayuda}>
            JPG o PNG, 5 MB como mucho. Sin foto el producto no se puede añadir
          </small>
        )}
      </div>

      <button type="button" className={formulario.primario} disabled={creando} onClick={anadir}>
        {creando ? 'Añadiendo…' : 'Añadir al escaparate'}
      </button>
    </fieldset>
  );
}

interface PropsCampos {
  /** Los identificadores tienen que ser únicos: hay varios de estos a la vez. */
  readonly prefijo: string;
  readonly borrador: Borrador;
  readonly errores: Errores;
  readonly alCambiar: (cambio: Partial<Borrador>) => void;
}

/** Los cuatro campos de texto de un producto. Los mismos al crear y al editar. */
function CamposDeProducto({ prefijo, borrador, errores, alCambiar }: PropsCampos) {
  return (
    <>
      <div className={formulario.campo}>
        <label htmlFor={`${prefijo}-nombre`}>Nombre</label>
        <input
          id={`${prefijo}-nombre`}
          type="text"
          value={borrador.nombre}
          maxLength={120}
          aria-invalid={errores.nombre !== undefined}
          onChange={(evento) => alCambiar({ nombre: evento.target.value })}
        />
        {errores.nombre !== undefined && (
          <small className={formulario.error}>{errores.nombre}</small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor={`${prefijo}-precio`}>Precio en pesos</label>
        <input
          id={`${prefijo}-precio`}
          type="text"
          inputMode="decimal"
          value={borrador.precio}
          aria-invalid={errores.precio !== undefined}
          aria-describedby={`ayuda-${prefijo}-precio`}
          onChange={(evento) => alCambiar({ precio: evento.target.value })}
        />
        {errores.precio !== undefined ? (
          <small className={formulario.error}>{errores.precio}</small>
        ) : (
          <small id={`ayuda-${prefijo}-precio`} className={formulario.ayuda}>
            Solo el número: 12000, sin puntos ni símbolo
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor={`${prefijo}-descripcion`}>Descripción</label>
        <input
          id={`${prefijo}-descripcion`}
          type="text"
          value={borrador.descripcion}
          maxLength={500}
          aria-invalid={errores.descripcion !== undefined}
          aria-describedby={`ayuda-${prefijo}-descripcion`}
          onChange={(evento) => alCambiar({ descripcion: evento.target.value })}
        />
        {errores.descripcion !== undefined ? (
          <small className={formulario.error}>{errores.descripcion}</small>
        ) : (
          <small id={`ayuda-${prefijo}-descripcion`} className={formulario.ayuda}>
            Opcional
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label className={formulario.casilla} htmlFor={`${prefijo}-disponible`}>
          <input
            id={`${prefijo}-disponible`}
            type="checkbox"
            checked={borrador.disponible}
            onChange={(evento) => alCambiar({ disponible: evento.target.checked })}
          />
          <span>Disponible ahora mismo</span>
        </label>
      </div>
    </>
  );
}
