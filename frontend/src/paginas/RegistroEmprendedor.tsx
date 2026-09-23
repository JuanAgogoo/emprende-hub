import { useEffect, useRef, useState, type FormEvent, type RefObject } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { obtenerCategoriasNegocio, obtenerCiudades } from '../api/catalogos';
import { crearProducto } from '../api/negocios';
import { ErrorApi } from '../api/cliente';
import { CargaDeFotos } from '../componentes/CargaDeFotos';
import { CampoContrasena } from '../componentes/CampoContrasena';
import { EnlaceLegal } from '../componentes/EnlaceLegal';
import { PasosAsistente } from '../componentes/PasosAsistente';
import { useSesion } from '../estado/SesionContext';
import { precio as formatearPrecio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CategoriaNegocio, Ciudad } from '../types/catalogo';
import type { NivelPrecio } from '../types/negocio';
import type { RegistroEmprendedor as Peticion } from '../types/registroEmprendedor';
import formulario from './Formulario.module.css';
import estilos from './RegistroEmprendedor.module.css';
import { useTitulo } from '../titulo';

/**
 * Los cuatro pasos que ve quien se registra.
 *
 * Los tres primeros recogen datos y se envían juntos; el cuarto son binarios y
 * van por su propio endpoint, ya con la sesión que el tercero acaba de abrir.
 */
const PASOS = ['Cuenta', 'Negocio', 'Escaparate', 'Fotos'] as const;

const PASO_CUENTA = 1;
const PASO_NEGOCIO = 2;
const PASO_ESCAPARATE = 3;
const PASO_FOTOS = 4;
/**
 * El paso cuyo envío crea la cuenta, el negocio y el escaparate de una vez.
 *
 * A partir de aquí no se puede volver atrás: reenviar el formulario chocaría
 * con el correo que se acaba de ocupar.
 */
const PASO_DEL_ENVIO = PASO_ESCAPARATE;

// ── Lo que se recoge en el navegador ──────────────────────────────────────

interface ValoresCuenta {
  readonly nombre: string;
  readonly correo: string;
  readonly contrasena: string;
  /** Solo vive en el navegador: no viaja en la petición, solo la comprueba. */
  readonly confirmacion: string;
  readonly aceptaDatos: boolean;
}

interface ValoresNegocio {
  readonly nombre: string;
  readonly descripcion: string;
  readonly telefono: string;
  readonly categoriaId: number | undefined;
  readonly ciudadId: number | undefined;
  readonly barrioId: number | undefined;
  readonly nivelPrecio: NivelPrecio | '';
  readonly instagram: string;
  readonly linkedin: string;
}

interface ValoresProducto {
  readonly nombre: string;
  readonly precio: string;
  readonly descripcion: string;
  readonly disponible: boolean;
  /** Obligatoria: sin imagen el backend no crea el producto. */
  readonly foto: File | null;
  /**
   * La URL local que pinta la miniatura mientras se rellena el formulario.
   *
   * Va en el estado y no se calcula al pintar: `createObjectURL` reserva memoria
   * cada vez que se llama, y hacerlo en cada render las iría acumulando.
   */
  readonly vistaPrevia: string | null;
}

/**
 * Un producto ya añadido a la lista, con su imagen ya elegida.
 *
 * El identificador es local y solo sirve de `key`: el de verdad lo pone el
 * backend cuando se crea. Sin él la lista tendría que usar el índice, y quitar
 * el primero renombraría a todos los demás.
 *
 * `foto` deja de ser nula aquí: no se añade a la lista sin ella. `vistaPrevia`
 * es la URL local que pinta la miniatura, y hay que liberarla al quitarlo.
 */
interface ProductoAnadido extends ValoresProducto {
  readonly id: number;
  readonly foto: File;
  readonly vistaPrevia: string;
}

interface Catalogos {
  readonly categorias: readonly CategoriaNegocio[];
  readonly ciudades: readonly Ciudad[];
}

type ErroresCuenta = Partial<Record<keyof ValoresCuenta, string>>;
type ErroresNegocio = Partial<Record<keyof ValoresNegocio, string>>;
type ErroresProducto = Partial<Record<keyof ValoresProducto, string>>;

const CUENTA_VACIA: ValoresCuenta = {
  nombre: '',
  correo: '',
  contrasena: '',
  confirmacion: '',
  aceptaDatos: false,
};

const NEGOCIO_VACIO: ValoresNegocio = {
  nombre: '',
  descripcion: '',
  telefono: '',
  categoriaId: undefined,
  ciudadId: undefined,
  barrioId: undefined,
  nivelPrecio: '',
  instagram: '',
  linkedin: '',
};

const PRODUCTO_VACIO: ValoresProducto = {
  nombre: '',
  precio: '',
  descripcion: '',
  disponible: true,
  foto: null,
  vistaPrevia: null,
};

/** Las mismas reglas de imagen que la galería del negocio (B9). */
const MAXIMO_BYTES_FOTO = 5 * 1024 * 1024;
const TIPOS_FOTO: readonly string[] = ['image/jpeg', 'image/png'];

const MINIMO_DESCRIPCION = 80;

const NIVELES: readonly { readonly valor: NivelPrecio; readonly texto: string }[] = [
  { valor: 'BAJO', texto: 'Económico ($)' },
  { valor: 'MEDIO', texto: 'Medio ($$)' },
  { valor: 'ALTO', texto: 'Alto ($$$)' },
];

// ── Validación: las mismas reglas que el backend, funciones puras ─────────

function validarCuenta(valores: ValoresCuenta): ErroresCuenta {
  const errores: ErroresCuenta = {};

  if (valores.nombre.trim() === '') errores.nombre = 'El nombre es obligatorio';
  else if (valores.nombre.trim().length > 120)
    errores.nombre = 'El nombre no puede pasar de 120 caracteres';

  if (valores.correo.trim() === '') errores.correo = 'El correo es obligatorio';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valores.correo.trim()))
    errores.correo = 'El correo no tiene un formato válido';

  if (valores.contrasena === '') errores.contrasena = 'La contraseña es obligatoria';
  else if (valores.contrasena.length < 8)
    errores.contrasena = 'La contraseña debe tener al menos 8 caracteres';

  // Se compara con la contraseña ya escrita, no con una regla propia: el campo
  // no tiene requisitos, solo tiene que coincidir.
  if (valores.confirmacion === '') errores.confirmacion = 'Repite la contraseña';
  else if (valores.confirmacion !== valores.contrasena)
    errores.confirmacion = 'Las dos contraseñas no coinciden';

  if (!valores.aceptaDatos)
    errores.aceptaDatos = 'Hay que aceptar el tratamiento de datos para crear la cuenta';

  return errores;
}

function validarNegocio(valores: ValoresNegocio): ErroresNegocio {
  const errores: ErroresNegocio = {};

  const nombre = valores.nombre.trim();
  if (nombre === '') errores.nombre = 'El nombre del negocio es obligatorio';
  else if (nombre.length < 3 || nombre.length > 120)
    errores.nombre = 'El nombre debe tener entre 3 y 120 caracteres';

  const descripcion = valores.descripcion.trim();
  if (descripcion === '') errores.descripcion = 'La descripción es obligatoria';
  else if (descripcion.length < MINIMO_DESCRIPCION)
    errores.descripcion = `La descripción debe tener al menos ${MINIMO_DESCRIPCION} caracteres`;
  else if (descripcion.length > 2000)
    errores.descripcion = 'La descripción no puede pasar de 2000 caracteres';

  // Móvil de 10 dígitos que empieza por 3, o fijo de 10 que empieza por 60 (G8).
  const telefono = valores.telefono.trim();
  if (telefono === '') errores.telefono = 'El teléfono es obligatorio';
  else if (!/^(3\d{9}|60\d{8})$/.test(telefono))
    errores.telefono = 'Debe ser un móvil (3XXXXXXXXX) o un fijo (60XXXXXXXX)';

  if (valores.categoriaId === undefined) errores.categoriaId = 'La categoría es obligatoria';
  if (valores.ciudadId === undefined) errores.ciudadId = 'La ciudad es obligatoria';
  if (valores.nivelPrecio === '') errores.nivelPrecio = 'El nivel de precio es obligatorio';

  // Los dominios de B8: un campo «Instagram» que enlaza a cualquier sitio es
  // justo lo que un perfil público no debe hacer.
  const instagram = valores.instagram.trim();
  if (instagram !== '' && !/^https:\/\/(www\.)?instagram\.com\/[A-Za-z0-9._]{1,60}\/?$/.test(instagram))
    errores.instagram = 'Debe ser un perfil de Instagram (https://instagram.com/tu-cuenta)';

  const linkedin = valores.linkedin.trim();
  if (
    linkedin !== '' &&
    !/^https:\/\/(www\.)?linkedin\.com\/(in|company)\/[A-Za-z0-9-]{1,80}\/?$/.test(linkedin)
  )
    errores.linkedin = 'Debe ser un perfil de LinkedIn (https://linkedin.com/in/tu-cuenta)';

  return errores;
}

function validarProducto(valores: ValoresProducto): ErroresProducto {
  const errores: ErroresProducto = {};

  const nombre = valores.nombre.trim();
  if (nombre === '') errores.nombre = 'El nombre del producto es obligatorio';
  else if (nombre.length < 2 || nombre.length > 120)
    errores.nombre = 'El nombre debe tener entre 2 y 120 caracteres';

  const precio = Number(valores.precio);
  if (valores.precio.trim() === '') errores.precio = 'El precio es obligatorio';
  else if (!Number.isFinite(precio)) errores.precio = 'El precio tiene que ser un número';
  else if (precio < 0) errores.precio = 'El precio no puede ser negativo';
  else if (Math.round(precio * 100) !== precio * 100)
    errores.precio = 'El precio admite como mucho dos decimales';

  if (valores.descripcion.trim().length > 500)
    errores.descripcion = 'La descripción no puede pasar de 500 caracteres';

  // Se comprueba aquí y no al llegar el 400: la imagen viaja entera para nada.
  if (valores.foto === null) errores.foto = 'Cada producto necesita una foto';
  else if (!TIPOS_FOTO.includes(valores.foto.type)) errores.foto = 'La foto tiene que ser JPG o PNG';
  else if (valores.foto.size > MAXIMO_BYTES_FOTO) errores.foto = 'La foto no puede pasar de 5 MB';

  return errores;
}

// ── Errores que devuelve el backend ───────────────────────────────────────

/**
 * A qué paso pertenece cada clave del `400`.
 *
 * Los campos anidados nombran su ruta —`negocio.descripcion`,
 * `redes.instagram`—, que es justo lo que permite devolver a quien rellena el
 * formulario al paso que falló.
 */
function pasoDeClave(clave: string): number {
  if (clave.startsWith('negocio.') || clave.startsWith('redes.')) return PASO_NEGOCIO;
  return PASO_CUENTA;
}

/** Quita una clave del mapa de errores del servidor, sin mutarlo. */
function sinClave(
  errores: Readonly<Record<string, string>>,
  clave: string,
): Readonly<Record<string, string>> {
  return Object.fromEntries(Object.entries(errores).filter(([nombre]) => nombre !== clave));
}

/** El del servidor manda; el del navegador solo aparece tras intentar avanzar. */
function errorDe(
  delServidor: string | undefined,
  delNavegador: string | undefined,
  intentado: boolean,
): string | undefined {
  return delServidor ?? (intentado ? delNavegador : undefined);
}

// ── La petición ───────────────────────────────────────────────────────────

/** Una cadena vacía no se envía: el backend distingue ausente de vacío. */
function opcional(valor: string): string | undefined {
  const limpio = valor.trim();
  return limpio === '' ? undefined : limpio;
}

function construirPeticion(cuenta: ValoresCuenta, negocio: ValoresNegocio): Peticion {
  const instagram = opcional(negocio.instagram);
  const linkedin = opcional(negocio.linkedin);

  return {
    nombre: cuenta.nombre.trim(),
    correo: cuenta.correo.trim(),
    contrasena: cuenta.contrasena,
    negocio: {
      nombre: negocio.nombre.trim(),
      descripcion: negocio.descripcion.trim(),
      telefono: negocio.telefono.trim(),
      // Validado antes de llegar aquí: en este punto los tres están puestos.
      categoriaId: negocio.categoriaId ?? 0,
      ciudadId: negocio.ciudadId ?? 0,
      barrioId: negocio.barrioId,
      nivelPrecio: negocio.nivelPrecio === '' ? 'MEDIO' : negocio.nivelPrecio,
    },
    // Sin ninguna red, el campo entero no viaja.
    redes: instagram === undefined && linkedin === undefined ? undefined : { instagram, linkedin },
  };
}

// ── La página ─────────────────────────────────────────────────────────────

export function RegistroEmprendedor() {
  useTitulo('Registro de emprendedor');
  const { registrarNegocio, salir } = useSesion();
  const navegar = useNavigate();

  const [paso, setPaso] = useState(PASO_CUENTA);
  const [cuenta, setCuenta] = useState<ValoresCuenta>(CUENTA_VACIA);
  const [negocio, setNegocio] = useState<ValoresNegocio>(NEGOCIO_VACIO);
  const [borrador, setBorrador] = useState<ValoresProducto>(PRODUCTO_VACIO);
  const [productos, setProductos] = useState<readonly ProductoAnadido[]>([]);

  // Los mensajes solo salen tras intentar avanzar: el markAllAsTouched() del curso.
  const [cuentaIntentada, setCuentaIntentada] = useState(false);
  const [negocioIntentado, setNegocioIntentado] = useState(false);
  const [productoIntentado, setProductoIntentado] = useState(false);

  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [erroresServidor, setErroresServidor] = useState<Readonly<Record<string, string>>>({});

  const [catalogos, setCatalogos] = useState<EstadoCarga<Catalogos>>({ estado: 'CARGANDO' });
  const [intentoCatalogos, setIntentoCatalogos] = useState(0);

  const contenedor = useRef<HTMLDivElement>(null);
  const encabezado = useRef<HTMLHeadingElement>(null);
  const siguienteId = useRef(1);
  const primerRender = useRef(true);

  useEffect(() => {
    let vigente = true;

    Promise.all([obtenerCategoriasNegocio(), obtenerCiudades()])
      .then(([categorias, ciudades]) => {
        if (vigente) setCatalogos({ estado: 'EXITO', datos: { categorias, ciudades } });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudieron cargar los catálogos';
        if (vigente) setCatalogos({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, [intentoCatalogos]);

  // Las vistas previas del escaparate reservan memoria hasta que se liberan, y
  // al salir del asistente no queda nadie que lo haga. Misma pareja de efectos
  // que en el paso de las fotos: una ref al día y una limpieza al desmontar.
  const vigentes = useRef<readonly string[]>([]);
  useEffect(() => {
    vigentes.current = [
      ...productos.map((producto) => producto.vistaPrevia),
      ...(borrador.vistaPrevia === null ? [] : [borrador.vistaPrevia]),
    ];
  }, [productos, borrador.vistaPrevia]);

  useEffect(() => {
    return () => {
      for (const url of vigentes.current) URL.revokeObjectURL(url);
    };
  }, []);

  // Al cambiar de paso el foco va al título. Sin esto, quien navega con teclado
  // se queda en el botón de un formulario que ya no está en pantalla.
  useEffect(() => {
    if (primerRender.current) {
      primerRender.current = false;
      return;
    }
    encabezado.current?.focus();
  }, [paso]);

  const erroresCuenta = validarCuenta(cuenta);
  const erroresNegocio = validarNegocio(negocio);
  const erroresBorrador = validarProducto(borrador);

  /**
   * Cierra el asistente devolviendo al login.
   *
   * La sesión se abrió en el paso 3 porque **hacía falta**: crear los productos
   * y subir las fotos son llamadas con token. Cumplido eso, se revoca: quien
   * acaba de registrarse entra con las credenciales que eligió, igual que el
   * cliente.
   */
  function terminarElRegistro() {
    const correo = cuenta.correo.trim();
    salir();
    navegar('/entrar', { replace: true, state: { registrado: correo } });
  }

  function reintentarCatalogos() {
    setCatalogos({ estado: 'CARGANDO' });
    setIntentoCatalogos((veces) => veces + 1);
  }

  function enfocar(id: string) {
    contenedor.current?.querySelector<HTMLElement>(`#${id}`)?.focus();
  }

  function cambiarCuenta<C extends keyof ValoresCuenta>(campo: C, valor: ValoresCuenta[C]) {
    setCuenta((previos) => ({ ...previos, [campo]: valor }));
    setErroresServidor((previos) => sinClave(previos, campo));
    setFallo(null);
  }

  function cambiarNegocio(cambio: Partial<ValoresNegocio>) {
    setNegocio((previos) => ({ ...previos, ...cambio }));
    for (const campo of Object.keys(cambio)) {
      const clave = campo === 'instagram' || campo === 'linkedin' ? 'redes' : 'negocio';
      setErroresServidor((previos) => sinClave(previos, `${clave}.${campo}`));
    }
    setFallo(null);
  }

  /** Avanza si el paso actual está completo; si no, enseña por qué. */
  function siguiente() {
    if (paso === PASO_CUENTA) {
      setCuentaIntentada(true);
      const primero = (
        ['nombre', 'correo', 'contrasena', 'confirmacion', 'aceptaDatos'] as const
      ).find((campo) => erroresCuenta[campo] !== undefined);
      if (primero !== undefined) {
        enfocar(`cuenta-${primero}`);
        return;
      }
    }

    if (paso === PASO_NEGOCIO) {
      setNegocioIntentado(true);
      const primero = (
        [
          'nombre',
          'descripcion',
          'telefono',
          'categoriaId',
          'ciudadId',
          'nivelPrecio',
          'instagram',
          'linkedin',
        ] as const
      ).find((campo) => erroresNegocio[campo] !== undefined);
      if (primero !== undefined) {
        enfocar(`negocio-${primero}`);
        return;
      }
    }

    setPaso((actual) => actual + 1);
  }

  /** Cambiar de fichero libera la vista previa anterior antes de crear otra. */
  function elegirFotoDelProducto(archivo: File | null) {
    setBorrador((previo) => {
      if (previo.vistaPrevia !== null) URL.revokeObjectURL(previo.vistaPrevia);
      return {
        ...previo,
        foto: archivo,
        vistaPrevia: archivo === null ? null : URL.createObjectURL(archivo),
      };
    });
  }

  function anadirProducto() {
    setProductoIntentado(true);
    const primero = (['nombre', 'precio', 'descripcion', 'foto'] as const).find(
      (campo) => erroresBorrador[campo] !== undefined,
    );
    if (primero !== undefined) {
      enfocar(`producto-${primero}`);
      return;
    }

    // `validarProducto` ya descartó los casos nulos. Se copian a constantes para
    // que el compilador los estreche también dentro del callback, sin un `as`.
    const foto = borrador.foto;
    const vistaPrevia = borrador.vistaPrevia;
    if (foto === null || vistaPrevia === null) return;

    // La vista previa se hereda en vez de crearse otra: es la misma imagen, y
    // reservarla dos veces obligaría a liberar la del borrador aquí mismo.
    setProductos((lista) => [...lista, { ...borrador, foto, vistaPrevia, id: siguienteId.current++ }]);
    setBorrador(PRODUCTO_VACIO);
    setProductoIntentado(false);
    enfocar('producto-nombre');
  }

  function quitarProducto(id: number) {
    setProductos((lista) => {
      const fuera = lista.find((producto) => producto.id === id);
      // La vista previa reserva memoria hasta que se libera.
      if (fuera !== undefined) URL.revokeObjectURL(fuera.vistaPrevia);
      return lista.filter((producto) => producto.id !== id);
    });
  }

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    // El paso de las fotos ya no envía nada: tiene sus propios botones y el
    // negocio existe desde el paso anterior.
    if (paso === PASO_FOTOS) return;

    // El envío solo ocurre en el paso 3: en los anteriores el botón avanza.
    if (paso !== PASO_DEL_ENVIO) {
      siguiente();
      return;
    }

    setFallo(null);
    setErroresServidor({});
    setEnviando(true);

    // A partir del momento en que la cuenta existe, un fallo ya no se arregla
    // volviendo atrás: reenviar el formulario chocaría con el correo ocupado.
    let cuentaCreada = false;

    try {
      await registrarNegocio(construirPeticion(cuenta, negocio));
      cuentaCreada = true;

      // El escaparate va después y no dentro: cada producto lleva su imagen, y
      // subir binarios exige la sesión que el registro acaba de abrir. De uno en
      // uno, para que el que falle se pueda nombrar.
      for (const producto of productos) {
        await crearProducto(
          {
            nombre: producto.nombre.trim(),
            precio: Number(producto.precio),
            descripcion: opcional(producto.descripcion),
            disponible: producto.disponible,
          },
          producto.foto,
        );
      }

      setPaso(PASO_FOTOS);
    } catch (error: unknown) {
      if (cuentaCreada) {
        // La cuenta y el negocio están creados; lo que falló es un producto.
        // Se sigue adelante diciéndolo, en vez de mandar a repetir un registro
        // que ya no se puede repetir.
        const mensaje = error instanceof Error ? error.message : 'un producto no se pudo crear';
        setFallo(
          `Tu cuenta y tu negocio se crearon, pero el escaparate quedó incompleto: ${mensaje}.`,
        );
        setPaso(PASO_FOTOS);
        return;
      }

      if (error instanceof ErrorApi) {
        const porCampo = Object.entries(error.porCampo);

        if (porCampo.length > 0) {
          // Validación de forma: cada clave a su campo, y al paso que la contiene.
          setErroresServidor(error.porCampo);
          const pasos = porCampo.map(([clave]) => pasoDeClave(clave));
          setPaso(Math.min(...pasos));
          setFallo('Revisa los campos marcados: el registro no se completó.');
        } else {
          // Regla de negocio: solo trae `message`. Aquí el único conflicto
          // posible es el correo repetido —el barrio sale de la propia ciudad y
          // una cuenta recién creada no puede tener ya un negocio—, así que se
          // marca ese campo además de decirlo arriba.
          setErroresServidor({ correo: error.message });
          setPaso(PASO_CUENTA);
          setCuentaIntentada(true);
          setFallo(error.message);
        }
      } else {
        const mensaje = error instanceof Error ? error.message : 'No se pudo crear la cuenta';
        setFallo(`${mensaje}. Comprueba que la API esté funcionando.`);
      }
    } finally {
      setEnviando(false);
    }
  }

  // El paso incompleto, ya intentado: es la razón visible de que no se avance.
  const pasoIncompleto =
    (paso === PASO_CUENTA && cuentaIntentada && Object.keys(erroresCuenta).length > 0) ||
    (paso === PASO_NEGOCIO && negocioIntentado && Object.keys(erroresNegocio).length > 0);

  const textoDelBoton = enviando
    ? 'Creando el negocio…'
    : paso === PASO_DEL_ENVIO
      ? 'Crear el negocio'
      : 'Siguiente';

  return (
    <div className={`contenedor ${formulario.pagina}`}>
      <div className={`${formulario.tarjeta} ${formulario.ancha}`} ref={contenedor}>
        <h1 className={formulario.titulo}>Publicar mi negocio</h1>
        <p className={formulario.entrada}>
          La cuenta y el negocio se crean juntos, en un solo envío. Al terminar quedará en revisión
          antes de aparecer en el directorio.
        </p>

        <PasosAsistente pasos={PASOS} actual={paso} />

        <form className={formulario.formulario} onSubmit={alEnviar} noValidate>
          {fallo !== null && (
            <p className={formulario.fallo} role="alert">
              {fallo}
            </p>
          )}

          {paso === PASO_CUENTA && (
            <PasoCuenta
              valores={cuenta}
              errores={erroresCuenta}
              erroresServidor={erroresServidor}
              intentado={cuentaIntentada}
              alCambiar={cambiarCuenta}
              encabezado={encabezado}
            />
          )}

          {paso === PASO_NEGOCIO && (
            <PasoNegocio
              valores={negocio}
              errores={erroresNegocio}
              erroresServidor={erroresServidor}
              intentado={negocioIntentado}
              catalogos={catalogos}
              alCambiar={cambiarNegocio}
              alReintentarCatalogos={reintentarCatalogos}
              encabezado={encabezado}
            />
          )}

          {paso === PASO_ESCAPARATE && (
            <PasoEscaparate
              borrador={borrador}
              errores={erroresBorrador}
              intentado={productoIntentado}
              productos={productos}
              alCambiar={(cambio) => setBorrador((previos) => ({ ...previos, ...cambio }))}
              alElegirFoto={elegirFotoDelProducto}
              alAnadir={anadirProducto}
              alQuitar={quitarProducto}
              encabezado={encabezado}
            />
          )}

          {/* El paso 4 trae sus propios botones: ni envía el formulario ni
              vuelve atrás, porque el negocio ya está creado. */}
          {paso === PASO_FOTOS ? (
            <CargaDeFotos alTerminar={terminarElRegistro} encabezado={encabezado} />
          ) : (
            <>
              <div className={estilos.navegacion}>
                {paso > PASO_CUENTA && (
                  <button
                    type="button"
                    className={estilos.secundario}
                    onClick={() => setPaso((actual) => actual - 1)}
                    disabled={enviando}
                  >
                    Anterior
                  </button>
                )}

                <button className={formulario.primario} type="submit" disabled={enviando}>
                  {textoDelBoton}
                </button>
              </div>

              {/* El botón nunca se deshabilita en silencio: si falta algo, se dice. */}
              {pasoIncompleto && (
                <p className={estilos.aviso} role="status">
                  Revisa los campos marcados antes de continuar.
                </p>
              )}
            </>
          )}
        </form>

        {paso !== PASO_FOTOS && (
          <p className={formulario.pie}>
            ¿Solo quieres opinar y contactar negocios? <Link to="/registro">Crea una cuenta</Link>.
          </p>
        )}
      </div>
    </div>
  );
}

// ── Paso 1 · La cuenta ────────────────────────────────────────────────────

interface PropsCuenta {
  readonly valores: ValoresCuenta;
  readonly errores: ErroresCuenta;
  readonly erroresServidor: Readonly<Record<string, string>>;
  readonly intentado: boolean;
  readonly alCambiar: <C extends keyof ValoresCuenta>(campo: C, valor: ValoresCuenta[C]) => void;
  readonly encabezado: RefObject<HTMLHeadingElement | null>;
}

function PasoCuenta({
  valores,
  errores,
  erroresServidor,
  intentado,
  alCambiar,
  encabezado,
}: PropsCuenta) {
  const errorNombre = errorDe(erroresServidor.nombre, errores.nombre, intentado);
  const errorCorreo = errorDe(erroresServidor.correo, errores.correo, intentado);
  const errorContrasena = errorDe(erroresServidor.contrasena, errores.contrasena, intentado);
  const errorConfirmacion = intentado ? errores.confirmacion : undefined;
  const errorCasilla = intentado ? errores.aceptaDatos : undefined;

  return (
    <>
      <h2 className={estilos.tituloPaso} ref={encabezado} tabIndex={-1}>
        Tu cuenta
      </h2>
      <p className={estilos.entradaPaso}>
        Con estos datos entrarás después a gestionar el negocio.
      </p>

      <div className={formulario.campo}>
        <label htmlFor="cuenta-nombre">Tu nombre *</label>
        <input
          id="cuenta-nombre"
          type="text"
          autoComplete="name"
          value={valores.nombre}
          onChange={(evento) => alCambiar('nombre', evento.target.value)}
          aria-invalid={errorNombre !== undefined}
          aria-describedby={errorNombre !== undefined ? 'error-cuenta-nombre' : undefined}
        />
        {errorNombre !== undefined && (
          <small id="error-cuenta-nombre" className={formulario.error}>
            {errorNombre}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="cuenta-correo">Correo *</label>
        <input
          id="cuenta-correo"
          type="email"
          autoComplete="email"
          value={valores.correo}
          onChange={(evento) => alCambiar('correo', evento.target.value)}
          aria-invalid={errorCorreo !== undefined}
          aria-describedby={errorCorreo !== undefined ? 'error-cuenta-correo' : undefined}
        />
        {errorCorreo !== undefined && (
          <small id="error-cuenta-correo" className={formulario.error}>
            {errorCorreo}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="cuenta-contrasena">Contraseña *</label>
        <CampoContrasena
          id="cuenta-contrasena"
          valor={valores.contrasena}
          autoComplete="new-password"
          invalido={errorContrasena !== undefined}
          describedBy={
            errorContrasena !== undefined ? 'error-cuenta-contrasena' : 'ayuda-contrasena'
          }
          alCambiar={(valor) => alCambiar('contrasena', valor)}
        />
        {errorContrasena !== undefined ? (
          <small id="error-cuenta-contrasena" className={formulario.error}>
            {errorContrasena}
          </small>
        ) : (
          <small id="ayuda-contrasena" className={formulario.ayuda}>
            Al menos 8 caracteres
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="cuenta-confirmacion">Repite la contraseña *</label>
        <CampoContrasena
          id="cuenta-confirmacion"
          valor={valores.confirmacion}
          autoComplete="new-password"
          invalido={errorConfirmacion !== undefined}
          describedBy={errorConfirmacion !== undefined ? 'error-cuenta-confirmacion' : undefined}
          alCambiar={(valor) => alCambiar('confirmacion', valor)}
        />
        {errorConfirmacion !== undefined && (
          <small id="error-cuenta-confirmacion" className={formulario.error}>
            {errorConfirmacion}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label className={formulario.casilla} htmlFor="cuenta-aceptaDatos">
          <input
            id="cuenta-aceptaDatos"
            type="checkbox"
            checked={valores.aceptaDatos}
            onChange={(evento) => alCambiar('aceptaDatos', evento.target.checked)}
            aria-invalid={errorCasilla !== undefined}
            aria-describedby={errorCasilla !== undefined ? 'error-cuenta-acepta' : undefined}
          />
          <span>
            He leído y acepto el{' '}
            <EnlaceLegal a="/tratamiento-de-datos" texto="tratamiento de datos" />.
          </span>
        </label>
        {errorCasilla !== undefined && (
          <small id="error-cuenta-acepta" className={formulario.error}>
            {errorCasilla}
          </small>
        )}
      </div>
    </>
  );
}

// ── Paso 2 · El negocio ───────────────────────────────────────────────────

interface PropsNegocio {
  readonly valores: ValoresNegocio;
  readonly errores: ErroresNegocio;
  readonly erroresServidor: Readonly<Record<string, string>>;
  readonly intentado: boolean;
  readonly catalogos: EstadoCarga<Catalogos>;
  readonly alCambiar: (cambio: Partial<ValoresNegocio>) => void;
  readonly alReintentarCatalogos: () => void;
  readonly encabezado: RefObject<HTMLHeadingElement | null>;
}

/** Convierte el valor de un `select` en número, o en `undefined` si está vacío. */
function aNumero(valor: string): number | undefined {
  return valor === '' ? undefined : Number(valor);
}

function PasoNegocio({
  valores,
  errores,
  erroresServidor,
  intentado,
  catalogos,
  alCambiar,
  alReintentarCatalogos,
  encabezado,
}: PropsNegocio) {
  const errorNombre = errorDe(erroresServidor['negocio.nombre'], errores.nombre, intentado);
  const errorDescripcion = errorDe(
    erroresServidor['negocio.descripcion'],
    errores.descripcion,
    intentado,
  );
  const errorTelefono = errorDe(erroresServidor['negocio.telefono'], errores.telefono, intentado);
  const errorCategoria = errorDe(
    erroresServidor['negocio.categoriaId'],
    errores.categoriaId,
    intentado,
  );
  const errorCiudad = errorDe(erroresServidor['negocio.ciudadId'], errores.ciudadId, intentado);
  const errorBarrio = erroresServidor['negocio.barrioId'];
  const errorNivel = errorDe(erroresServidor['negocio.nivelPrecio'], errores.nivelPrecio, intentado);
  const errorInstagram = errorDe(erroresServidor['redes.instagram'], errores.instagram, intentado);
  const errorLinkedin = errorDe(erroresServidor['redes.linkedin'], errores.linkedin, intentado);

  const escritos = valores.descripcion.trim().length;
  const faltan = MINIMO_DESCRIPCION - escritos;

  return (
    <>
      <h2 className={estilos.tituloPaso} ref={encabezado} tabIndex={-1}>
        Tu negocio
      </h2>
      <p className={estilos.entradaPaso}>
        Esto es lo que verá quien te encuentre en el directorio.
      </p>

      <div className={formulario.campo}>
        <label htmlFor="negocio-nombre">Nombre del negocio *</label>
        <input
          id="negocio-nombre"
          type="text"
          value={valores.nombre}
          onChange={(evento) => alCambiar({ nombre: evento.target.value })}
          aria-invalid={errorNombre !== undefined}
          aria-describedby={errorNombre !== undefined ? 'error-negocio-nombre' : undefined}
        />
        {errorNombre !== undefined && (
          <small id="error-negocio-nombre" className={formulario.error}>
            {errorNombre}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="negocio-descripcion">Descripción *</label>
        <textarea
          id="negocio-descripcion"
          rows={5}
          value={valores.descripcion}
          onChange={(evento) => alCambiar({ descripcion: evento.target.value })}
          aria-invalid={errorDescripcion !== undefined}
          aria-describedby={
            errorDescripcion !== undefined
              ? 'error-negocio-descripcion contador-descripcion'
              : 'contador-descripcion'
          }
        />
        {/* El contador a la vista: que el 400 sea la excepción, no la norma. */}
        <small
          id="contador-descripcion"
          className={faltan > 0 ? formulario.ayuda : estilos.contadorListo}
          aria-live="polite"
        >
          {faltan > 0
            ? `${escritos} de ${MINIMO_DESCRIPCION} caracteres · faltan ${faltan}`
            : `${escritos} caracteres · ya cumple el mínimo`}
        </small>
        {errorDescripcion !== undefined && (
          <small id="error-negocio-descripcion" className={formulario.error}>
            {errorDescripcion}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="negocio-telefono">Teléfono *</label>
        <input
          id="negocio-telefono"
          type="tel"
          inputMode="numeric"
          maxLength={10}
          autoComplete="tel"
          value={valores.telefono}
          onChange={(evento) => alCambiar({ telefono: evento.target.value })}
          aria-invalid={errorTelefono !== undefined}
          aria-describedby={
            errorTelefono !== undefined ? 'error-negocio-telefono' : 'ayuda-telefono'
          }
        />
        {errorTelefono !== undefined ? (
          <small id="error-negocio-telefono" className={formulario.error}>
            {errorTelefono}
          </small>
        ) : (
          <small id="ayuda-telefono" className={formulario.ayuda}>
            Móvil (3105551234) o fijo (6045551234)
          </small>
        )}
      </div>

      <CamposDeCatalogo
        catalogos={catalogos}
        valores={valores}
        errorCategoria={errorCategoria}
        errorCiudad={errorCiudad}
        errorBarrio={errorBarrio}
        alCambiar={alCambiar}
        alReintentar={alReintentarCatalogos}
      />

      <div className={formulario.campo}>
        <label htmlFor="negocio-nivelPrecio">Nivel de precio *</label>
        <select
          id="negocio-nivelPrecio"
          value={valores.nivelPrecio}
          onChange={(evento) =>
            alCambiar({
              nivelPrecio: evento.target.value === '' ? '' : (evento.target.value as NivelPrecio),
            })
          }
          aria-invalid={errorNivel !== undefined}
          aria-describedby={errorNivel !== undefined ? 'error-negocio-nivel' : undefined}
        >
          <option value="">Elige uno</option>
          {NIVELES.map((nivel) => (
            <option key={nivel.valor} value={nivel.valor}>
              {nivel.texto}
            </option>
          ))}
        </select>
        {errorNivel !== undefined && (
          <small id="error-negocio-nivel" className={formulario.error}>
            {errorNivel}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="negocio-instagram">Instagram</label>
        <input
          id="negocio-instagram"
          type="url"
          placeholder="https://instagram.com/tu-cuenta"
          value={valores.instagram}
          onChange={(evento) => alCambiar({ instagram: evento.target.value })}
          aria-invalid={errorInstagram !== undefined}
          aria-describedby={errorInstagram !== undefined ? 'error-negocio-instagram' : undefined}
        />
        {errorInstagram !== undefined && (
          <small id="error-negocio-instagram" className={formulario.error}>
            {errorInstagram}
          </small>
        )}
      </div>

      <div className={formulario.campo}>
        <label htmlFor="negocio-linkedin">LinkedIn</label>
        <input
          id="negocio-linkedin"
          type="url"
          placeholder="https://linkedin.com/in/tu-cuenta"
          value={valores.linkedin}
          onChange={(evento) => alCambiar({ linkedin: evento.target.value })}
          aria-invalid={errorLinkedin !== undefined}
          aria-describedby={errorLinkedin !== undefined ? 'error-negocio-linkedin' : undefined}
        />
        {errorLinkedin !== undefined && (
          <small id="error-negocio-linkedin" className={formulario.error}>
            {errorLinkedin}
          </small>
        )}
      </div>
    </>
  );
}

interface PropsCatalogo {
  readonly catalogos: EstadoCarga<Catalogos>;
  readonly valores: ValoresNegocio;
  readonly errorCategoria: string | undefined;
  readonly errorCiudad: string | undefined;
  readonly errorBarrio: string | undefined;
  readonly alCambiar: (cambio: Partial<ValoresNegocio>) => void;
  readonly alReintentar: () => void;
}

/** Categoría, ciudad y barrio: los tres dependen del catálogo, que puede fallar. */
function CamposDeCatalogo({
  catalogos,
  valores,
  errorCategoria,
  errorCiudad,
  errorBarrio,
  alCambiar,
  alReintentar,
}: PropsCatalogo) {
  switch (catalogos.estado) {
    case 'CARGANDO':
      return (
        <p className={formulario.ayuda} aria-busy="true">
          Cargando las categorías y las ciudades…
        </p>
      );

    case 'ERROR':
      return (
        <div className={estilos.errorCatalogos} role="alert">
          <p>No se pudieron cargar las categorías y las ciudades: {catalogos.mensaje}.</p>
          <button type="button" className={estilos.secundario} onClick={alReintentar}>
            Reintentar
          </button>
        </div>
      );

    case 'EXITO': {
      const { categorias, ciudades } = catalogos.datos;
      const ciudadElegida = ciudades.find((ciudad) => ciudad.id === valores.ciudadId);
      const barrios = ciudadElegida?.barrios ?? [];

      return (
        <>
          <div className={formulario.campo}>
            <label htmlFor="negocio-categoriaId">Categoría *</label>
            <select
              id="negocio-categoriaId"
              value={valores.categoriaId ?? ''}
              onChange={(evento) => alCambiar({ categoriaId: aNumero(evento.target.value) })}
              aria-invalid={errorCategoria !== undefined}
              aria-describedby={errorCategoria !== undefined ? 'error-negocio-categoria' : undefined}
            >
              <option value="">Elige una</option>
              {categorias.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>
                  {categoria.nombre}
                </option>
              ))}
            </select>
            {errorCategoria !== undefined && (
              <small id="error-negocio-categoria" className={formulario.error}>
                {errorCategoria}
              </small>
            )}
          </div>

          <div className={formulario.campo}>
            <label htmlFor="negocio-ciudadId">Ciudad *</label>
            <select
              id="negocio-ciudadId"
              value={valores.ciudadId ?? ''}
              onChange={(evento) =>
                // Cambiar de ciudad invalida el barrio elegido: se limpia a la vez.
                alCambiar({ ciudadId: aNumero(evento.target.value), barrioId: undefined })
              }
              aria-invalid={errorCiudad !== undefined}
              aria-describedby={errorCiudad !== undefined ? 'error-negocio-ciudad' : undefined}
            >
              <option value="">Elige una</option>
              {ciudades.map((ciudad) => (
                <option key={ciudad.id} value={ciudad.id}>
                  {ciudad.nombre}
                </option>
              ))}
            </select>
            {errorCiudad !== undefined && (
              <small id="error-negocio-ciudad" className={formulario.error}>
                {errorCiudad}
              </small>
            )}
          </div>

          <div className={formulario.campo}>
            <label htmlFor="negocio-barrioId">Barrio</label>
            <select
              id="negocio-barrioId"
              value={valores.barrioId ?? ''}
              disabled={barrios.length === 0}
              onChange={(evento) => alCambiar({ barrioId: aNumero(evento.target.value) })}
              aria-invalid={errorBarrio !== undefined}
              aria-describedby={
                errorBarrio !== undefined
                  ? 'error-negocio-barrio'
                  : barrios.length === 0
                    ? 'ayuda-barrio'
                    : undefined
              }
            >
              <option value="">Sin especificar</option>
              {barrios.map((barrio) => (
                <option key={barrio.id} value={barrio.id}>
                  {barrio.nombre}
                </option>
              ))}
            </select>
            {/* Deshabilitado en silencio no se entiende: se dice por qué. */}
            {barrios.length === 0 && (
              <small id="ayuda-barrio" className={formulario.ayuda}>
                {ciudadElegida === undefined
                  ? 'Elige antes una ciudad'
                  : `${ciudadElegida.nombre} no tiene barrios cargados`}
              </small>
            )}
            {errorBarrio !== undefined && (
              <small id="error-negocio-barrio" className={formulario.error}>
                {errorBarrio}
              </small>
            )}
          </div>
        </>
      );
    }

    default:
      return casoImposible(catalogos);
  }
}

// ── Paso 3 · El escaparate ────────────────────────────────────────────────

interface PropsEscaparate {
  readonly borrador: ValoresProducto;
  readonly errores: ErroresProducto;
  readonly intentado: boolean;
  readonly productos: readonly ProductoAnadido[];
  readonly alCambiar: (cambio: Partial<ValoresProducto>) => void;
  readonly alElegirFoto: (archivo: File | null) => void;
  readonly alAnadir: () => void;
  readonly alQuitar: (id: number) => void;
  readonly encabezado: RefObject<HTMLHeadingElement | null>;
}

function PasoEscaparate({
  borrador,
  errores,
  intentado,
  productos,
  alCambiar,
  alElegirFoto,
  alAnadir,
  alQuitar,
  encabezado,
}: PropsEscaparate) {
  const errorNombre = intentado ? errores.nombre : undefined;
  const errorPrecio = intentado ? errores.precio : undefined;
  const errorDescripcion = intentado ? errores.descripcion : undefined;
  const errorFoto = intentado ? errores.foto : undefined;

  return (
    <>
      <h2 className={estilos.tituloPaso} ref={encabezado} tabIndex={-1}>
        Tu escaparate
      </h2>
      <p className={estilos.entradaPaso}>
        Añade lo que vendes, uno a uno. Puedes dejarlo vacío y montarlo más adelante.
      </p>

      {productos.length === 0 ? (
        <p className={estilos.vacio}>Todavía no has añadido ningún producto.</p>
      ) : (
        <ul className={estilos.lista}>
          {productos.map((producto) => (
            <li key={producto.id} className={estilos.producto}>
              <img className={estilos.miniatura} src={producto.vistaPrevia} alt="" />
              <div className={estilos.datosProducto}>
                <span className={estilos.nombreProducto}>{producto.nombre}</span>
                <span className={estilos.precioProducto}>
                  {formatearPrecio(Number(producto.precio))}
                </span>
                {!producto.disponible && <span className={estilos.agotado}>No disponible</span>}
              </div>
              <button
                type="button"
                className={estilos.quitar}
                onClick={() => alQuitar(producto.id)}
              >
                Quitar
                <span className={estilos.oculto}> {producto.nombre}</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      <fieldset className={estilos.nuevoProducto}>
        <legend className={estilos.leyenda}>Añadir un producto</legend>

        <div className={formulario.campo}>
          <label htmlFor="producto-nombre">Nombre</label>
          <input
            id="producto-nombre"
            type="text"
            value={borrador.nombre}
            onChange={(evento) => alCambiar({ nombre: evento.target.value })}
            aria-invalid={errorNombre !== undefined}
            aria-describedby={errorNombre !== undefined ? 'error-producto-nombre' : undefined}
          />
          {errorNombre !== undefined && (
            <small id="error-producto-nombre" className={formulario.error}>
              {errorNombre}
            </small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="producto-precio">Precio en pesos</label>
          <input
            id="producto-precio"
            type="number"
            min={0}
            step={1}
            inputMode="numeric"
            value={borrador.precio}
            onChange={(evento) => alCambiar({ precio: evento.target.value })}
            aria-invalid={errorPrecio !== undefined}
            aria-describedby={errorPrecio !== undefined ? 'error-producto-precio' : 'ayuda-precio'}
          />
          {errorPrecio !== undefined ? (
            <small id="error-producto-precio" className={formulario.error}>
              {errorPrecio}
            </small>
          ) : (
            <small id="ayuda-precio" className={formulario.ayuda}>
              Solo el número: 12000, sin puntos ni símbolo
            </small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="producto-descripcion">Descripción</label>
          <input
            id="producto-descripcion"
            type="text"
            value={borrador.descripcion}
            onChange={(evento) => alCambiar({ descripcion: evento.target.value })}
            aria-invalid={errorDescripcion !== undefined}
            aria-describedby={
              errorDescripcion !== undefined ? 'error-producto-descripcion' : undefined
            }
          />
          {errorDescripcion !== undefined && (
            <small id="error-producto-descripcion" className={formulario.error}>
              {errorDescripcion}
            </small>
          )}
        </div>

        <div className={formulario.campo}>
          <label className={formulario.casilla} htmlFor="producto-disponible">
            <input
              id="producto-disponible"
              type="checkbox"
              checked={borrador.disponible}
              onChange={(evento) => alCambiar({ disponible: evento.target.checked })}
            />
            <span>Disponible ahora mismo</span>
          </label>
        </div>

        <div className={formulario.campo}>
          <label htmlFor="producto-foto">Foto *</label>

          <div className={estilos.zonaFoto}>
            {borrador.vistaPrevia === null ? (
              <p className={estilos.sinFoto}>Todavía sin foto</p>
            ) : (
              <>
                <img className={estilos.previa} src={borrador.vistaPrevia} alt="" />
                <p className={estilos.nombreArchivo}>{borrador.foto?.name}</p>
              </>
            )}

            {/* El botón es la etiqueta del campo: al pulsarla el navegador abre
                el selector, sin una línea de JavaScript. El input sigue ahí,
                invisible pero enfocable, para que el tabulador lo alcance. */}
            <label className={estilos.botonArchivo} htmlFor="producto-foto">
              {borrador.foto === null ? 'Elegir una foto' : 'Cambiar la foto'}
            </label>

            <input
              id="producto-foto"
              className={estilos.archivoOculto}
              type="file"
              accept="image/jpeg,image/png"
              onChange={(evento) => alElegirFoto(evento.target.files?.[0] ?? null)}
              aria-invalid={errorFoto !== undefined}
              aria-describedby={
                errorFoto !== undefined ? 'error-producto-foto' : 'ayuda-producto-foto'
              }
            />
          </div>

          {errorFoto !== undefined ? (
            <small id="error-producto-foto" className={formulario.error}>
              {errorFoto}
            </small>
          ) : (
            <small id="ayuda-producto-foto" className={formulario.ayuda}>
              JPG o PNG, 5 MB como mucho. Sin foto el producto no se puede añadir
            </small>
          )}
        </div>

        {/* Botón corriente y no `submit`: el envío del formulario es el registro. */}
        <button type="button" className={estilos.secundario} onClick={alAnadir}>
          Añadir a la lista
        </button>
      </fieldset>
    </>
  );
}
