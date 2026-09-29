import { useEffect, useState, type FormEvent } from 'react';
import { ErrorApi } from '../api/cliente';
import { obtenerCategoriasNegocio } from '../api/catalogos';
import { editarNegocio, editarRedes, editarTelefono } from '../api/negocios';
import { IconoInstagram } from './IconoInstagram';
import { IconoLinkedIn } from './IconoLinkedIn';
import { nivelPrecio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CategoriaNegocio } from '../types/catalogo';
import type { MiNegocio } from '../types/negocio';
import formulario from '../paginas/Formulario.module.css';
import estilos from './EditarNegocio.module.css';

/** Lo que el formulario tiene en la mano mientras se escribe. */
interface Valores {
  readonly nombre: string;
  readonly descripcion: string;
  readonly categoriaId: number;
  readonly telefono: string;
  /** Los dos de B8, opcionales. En blanco significa «no tengo». */
  readonly instagram: string;
  readonly linkedin: string;
}

/** Las mismas reglas que el backend, para no viajar hasta allí a por el error. */
const TELEFONO = /^(3\d{9}|60\d{8})$/;
const MINIMO_DESCRIPCION = 80;

/**
 * Los enlaces se validan contra su dominio, copiados del backend (B8).
 *
 * No es cosmético: sin esta comprobación el botón «Instagram» de un perfil
 * público podría llevar a cualquier sitio. El backend responde `400` igual; esto
 * solo evita el viaje.
 */
const INSTAGRAM = /^https:\/\/(www\.)?instagram\.com\/[A-Za-z0-9._]{1,60}\/?$/;
const LINKEDIN = /^https:\/\/(www\.)?linkedin\.com\/(in|company)\/[A-Za-z0-9-]{1,80}\/?$/;

/**
 * Los errores del formulario, por campo y como función pura.
 *
 * Fuera del componente porque no depende de nada suyo: se lee de un vistazo y
 * se prueba sola. Es la traducción de los `Validators` de Angular.
 */
function validar(valores: Valores): Readonly<Partial<Record<keyof Valores, string>>> {
  const errores: Partial<Record<keyof Valores, string>> = {};

  const nombre = valores.nombre.trim();
  if (nombre.length < 3 || nombre.length > 120) {
    errores.nombre = 'El nombre debe tener entre 3 y 120 caracteres';
  }

  const descripcion = valores.descripcion.trim();
  if (descripcion.length < MINIMO_DESCRIPCION) {
    errores.descripcion = `La descripción necesita al menos ${MINIMO_DESCRIPCION} caracteres`;
  } else if (descripcion.length > 2000) {
    errores.descripcion = 'La descripción no puede pasar de 2000 caracteres';
  }

  if (!TELEFONO.test(valores.telefono.trim())) {
    errores.telefono = 'Un móvil (3XXXXXXXXX) o un fijo (60XXXXXXXX)';
  }

  if (valores.categoriaId <= 0) {
    errores.categoriaId = 'Elige una categoría';
  }

  // Vacío es válido en los dos: es como se quita un enlace que ya no vale.
  const instagram = valores.instagram.trim();
  if (instagram.length > 0 && !INSTAGRAM.test(instagram)) {
    errores.instagram = 'Un perfil de Instagram: https://instagram.com/tu-cuenta';
  }

  const linkedin = valores.linkedin.trim();
  if (linkedin.length > 0 && !LINKEDIN.test(linkedin)) {
    errores.linkedin = 'Un perfil de LinkedIn: https://linkedin.com/in/tu-cuenta';
  }

  return errores;
}

/**
 * Cómo le fue al guardado, que no es solo «bien» o «mal».
 *
 * Con el negocio ya publicado y la revisión encendida, el backend acepta la
 * edición y **no la publica**: la deja esperando (B2-bis). Decir «guardado» a
 * secas ahí sería mentir, y decir «error» también.
 */
type Resultado =
  | { readonly tipo: 'PUBLICADO' }
  | { readonly tipo: 'EN_REVISION' }
  | { readonly tipo: 'SOLO_INMEDIATO' };

interface Props {
  readonly negocio: MiNegocio;
  /** Sube el negocio ya actualizado a quien lo pintó todo. */
  readonly alActualizar: (negocio: MiNegocio) => void;
}

/**
 * Editar los datos del negocio (HU-032).
 *
 * Este componente solo resuelve el catálogo de categorías; el formulario vive
 * en el de abajo y lo recibe ya cargado. Así el selector no necesita un efecto
 * que le rellene el valor inicial después: cuando se monta, ya lo sabe.
 */
export function EditarNegocio({ negocio, alActualizar }: Props) {
  const [catalogo, setCatalogo] = useState<EstadoCarga<readonly CategoriaNegocio[]>>({
    estado: 'CARGANDO',
  });

  useEffect(() => {
    let vigente = true;

    obtenerCategoriasNegocio()
      .then((categorias) => {
        if (vigente) setCatalogo({ estado: 'EXITO', datos: categorias });
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        const mensaje =
          error instanceof Error ? error.message : 'No se pudieron cargar las categorías';
        setCatalogo({ estado: 'ERROR', mensaje });
      });

    return () => {
      vigente = false;
    };
  }, []);

  switch (catalogo.estado) {
    case 'CARGANDO':
      return <div className={estilos.esqueleto} aria-busy="true" />;

    case 'ERROR':
      return (
        <p className={formulario.fallo} role="alert">
          No se pudieron cargar las categorías: {catalogo.mensaje}. Sin ellas no se puede cambiar
          la del negocio.
        </p>
      );

    case 'EXITO':
      return (
        <FormularioDeNegocio
          // Si el negocio cambia de identidad, el formulario empieza de cero en
          // vez de arrastrar lo que había escrito para otro.
          key={negocio.id}
          negocio={negocio}
          categorias={catalogo.datos}
          alActualizar={alActualizar}
        />
      );

    default:
      return casoImposible(catalogo);
  }
}

interface PropsFormulario {
  readonly negocio: MiNegocio;
  readonly categorias: readonly CategoriaNegocio[];
  readonly alActualizar: (negocio: MiNegocio) => void;
}

/**
 * El formulario en sí, con la vista previa al lado.
 *
 * Los campos no van todos al mismo sitio: el teléfono se aplica al momento y el
 * nombre, la descripción y la categoría pueden quedar esperando revisión (B2).
 * Son dos endpoints y por eso hay dos llamadas, pero un solo botón: quien edita
 * no tiene por qué saber dónde está esa frontera.
 */
function FormularioDeNegocio({ negocio, categorias, alActualizar }: PropsFormulario) {
  // El negocio trae el **nombre** de su categoría y el selector necesita el
  // identificador: se traduce una vez, aquí, con el catálogo ya en la mano.
  const suya = categorias.find((categoria) => categoria.nombre === negocio.categoria);

  const [valores, setValores] = useState<Valores>({
    nombre: negocio.nombre,
    descripcion: negocio.descripcion,
    categoriaId: suya?.id ?? 0,
    telefono: negocio.telefono,
    // El negocio los trae nulos cuando no tiene; el campo de texto necesita
    // una cadena. `??` y no `||` porque aquí la diferencia importa.
    instagram: negocio.instagram ?? '',
    linkedin: negocio.linkedin ?? '',
  });
  const [enviado, setEnviado] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState<Resultado | null>(null);
  const [fallo, setFallo] = useState<string | null>(null);

  const errores = validar(valores);
  const hayErrores = Object.keys(errores).length > 0;
  const elegida = categorias.find((categoria) => categoria.id === valores.categoriaId);

  /** Qué cambió de verdad: sin esto se mandarían tres peticiones para nada. */
  const cambioElTelefono = valores.telefono.trim() !== negocio.telefono;
  const cambiaronLasRedes =
    valores.instagram.trim() !== (negocio.instagram ?? '') ||
    valores.linkedin.trim() !== (negocio.linkedin ?? '');
  const cambiaronLosPublicos =
    valores.nombre.trim() !== negocio.nombre ||
    valores.descripcion.trim() !== negocio.descripcion ||
    elegida?.nombre !== negocio.categoria;
  const hayCambios = cambioElTelefono || cambiaronLasRedes || cambiaronLosPublicos;

  function escribir<C extends keyof Valores>(campo: C, valor: Valores[C]) {
    setValores((actuales) => ({ ...actuales, [campo]: valor }));
    // El cartel de «guardado» de la vez anterior ya no describe lo que hay.
    setResultado(null);
  }

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    if (hayErrores || !hayCambios) return;

    setFallo(null);
    setGuardando(true);

    try {
      // Los inmediatos primero —teléfono y redes— y los públicos al final: así
      // la respuesta que se queda es la del cambio que sí puede acabar
      // esperando, que es la que decide qué decir en pantalla.
      let actualizado = negocio;
      if (cambioElTelefono) {
        actualizado = await editarTelefono(valores.telefono.trim());
      }
      if (cambiaronLasRedes) {
        actualizado = await editarRedes(valores.instagram.trim(), valores.linkedin.trim());
      }
      if (cambiaronLosPublicos) {
        actualizado = await editarNegocio({
          nombre: valores.nombre.trim(),
          descripcion: valores.descripcion.trim(),
          categoriaId: valores.categoriaId,
        });
      }

      alActualizar(actualizado);
      setResultado(resultadoDe(cambiaronLosPublicos, actualizado));
    } catch (error: unknown) {
      setFallo(explicar(error));
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className={estilos.columnas}>
      <form
        className={`${formulario.formulario} ${estilos.formulario}`}
        onSubmit={alEnviar}
        noValidate
      >
        {fallo !== null && (
          <p className={formulario.fallo} role="alert">
            {fallo}
          </p>
        )}

        {resultado !== null && <Confirmacion resultado={resultado} />}

        <div className={formulario.campo}>
          <label htmlFor="editar-nombre">Nombre del negocio</label>
          <input
            id="editar-nombre"
            type="text"
            value={valores.nombre}
            maxLength={120}
            aria-invalid={enviado && errores.nombre !== undefined}
            onChange={(evento) => escribir('nombre', evento.target.value)}
          />
          {enviado && errores.nombre !== undefined && (
            <small className={formulario.error}>{errores.nombre}</small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="editar-categoria">Categoría</label>
          <select
            id="editar-categoria"
            value={valores.categoriaId}
            aria-invalid={enviado && errores.categoriaId !== undefined}
            onChange={(evento) => escribir('categoriaId', Number(evento.target.value))}
          >
            <option value={0}>Elige una categoría</option>
            {categorias.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.icono} {categoria.nombre}
              </option>
            ))}
          </select>
          {enviado && errores.categoriaId !== undefined && (
            <small className={formulario.error}>{errores.categoriaId}</small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="editar-telefono">Teléfono</label>
          <input
            id="editar-telefono"
            type="tel"
            value={valores.telefono}
            aria-invalid={enviado && errores.telefono !== undefined}
            aria-describedby="ayuda-telefono"
            onChange={(evento) => escribir('telefono', evento.target.value)}
          />
          {enviado && errores.telefono !== undefined ? (
            <small className={formulario.error}>{errores.telefono}</small>
          ) : (
            <small id="ayuda-telefono" className={formulario.ayuda}>
              Este se publica al momento, sin pasar por revisión
            </small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="editar-instagram">Instagram</label>
          <input
            id="editar-instagram"
            type="url"
            value={valores.instagram}
            placeholder="https://instagram.com/tu-cuenta"
            aria-invalid={enviado && errores.instagram !== undefined}
            aria-describedby="ayuda-instagram"
            onChange={(evento) => escribir('instagram', evento.target.value)}
          />
          {enviado && errores.instagram !== undefined ? (
            <small className={formulario.error}>{errores.instagram}</small>
          ) : (
            <small id="ayuda-instagram" className={formulario.ayuda}>
              Opcional. Déjalo en blanco para quitar el enlace
            </small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="editar-linkedin">LinkedIn</label>
          <input
            id="editar-linkedin"
            type="url"
            value={valores.linkedin}
            placeholder="https://linkedin.com/in/tu-cuenta"
            aria-invalid={enviado && errores.linkedin !== undefined}
            aria-describedby="ayuda-linkedin"
            onChange={(evento) => escribir('linkedin', evento.target.value)}
          />
          {enviado && errores.linkedin !== undefined ? (
            <small className={formulario.error}>{errores.linkedin}</small>
          ) : (
            <small id="ayuda-linkedin" className={formulario.ayuda}>
              Opcional. Déjalo en blanco para quitar el enlace
            </small>
          )}
        </div>

        <div className={formulario.campo}>
          <label htmlFor="editar-descripcion">Descripción</label>
          <textarea
            id="editar-descripcion"
            rows={6}
            value={valores.descripcion}
            maxLength={2000}
            aria-invalid={enviado && errores.descripcion !== undefined}
            aria-describedby="ayuda-descripcion"
            onChange={(evento) => escribir('descripcion', evento.target.value)}
          />
          {enviado && errores.descripcion !== undefined ? (
            <small className={formulario.error}>{errores.descripcion}</small>
          ) : (
            <small id="ayuda-descripcion" className={formulario.ayuda}>
              {valores.descripcion.trim().length} caracteres · el mínimo son {MINIMO_DESCRIPCION}
            </small>
          )}
        </div>

        <button type="submit" className={formulario.primario} disabled={guardando || !hayCambios}>
          {guardando ? 'Guardando…' : 'Guardar cambios'}
        </button>

        {!hayCambios && resultado === null && (
          <p className={formulario.ayuda}>Cambia algo para poder guardar.</p>
        )}
      </form>

      <VistaPrevia negocio={negocio} valores={valores} categoria={elegida} />
    </div>
  );
}

/**
 * El mensaje que se enseña cuando el guardado falla.
 *
 * Una validación del backend llega con `message` genérico y el detalle en
 * `porCampo`: enseñar el genérico deja a quien edita sin saber qué campo
 * corregir. Es el mismo criterio que sigue el registro.
 */
function explicar(error: unknown): string {
  if (error instanceof ErrorApi) {
    const porCampo = Object.values(error.porCampo);
    if (porCampo.length > 0) return porCampo.join('. ');
  }
  return error instanceof Error ? error.message : 'No se pudieron guardar los cambios';
}

/**
 * Qué decir después de guardar.
 *
 * Lo decide la respuesta del servidor y no una suposición: si vuelve con una
 * propuesta en cola, el cambio está esperando; si vuelve sin ella, ya se ve.
 */
function resultadoDe(cambiaronLosPublicos: boolean, actualizado: MiNegocio): Resultado {
  if (!cambiaronLosPublicos) return { tipo: 'SOLO_INMEDIATO' };
  return actualizado.cambioPendiente === null ? { tipo: 'PUBLICADO' } : { tipo: 'EN_REVISION' };
}

/** El cartel de después de guardar. Cada caso dice algo distinto. */
function Confirmacion({ resultado }: { readonly resultado: Resultado }) {
  switch (resultado.tipo) {
    case 'PUBLICADO':
      return (
        <p className={estilos.confirmacion} role="status">
          <strong>Guardado.</strong> Tus cambios ya están publicados: quien entre en el directorio
          los ve.
        </p>
      );

    case 'SOLO_INMEDIATO':
      return (
        <p className={estilos.confirmacion} role="status">
          <strong>Datos de contacto actualizados.</strong> El teléfono y las redes no pasan por
          revisión: ya aparecen en tu perfil público.
        </p>
      );

    case 'EN_REVISION':
      return (
        <p className={estilos.enRevision} role="status">
          <strong>Guardado, esperando revisión.</strong> Tu negocio sigue publicado con los datos de
          antes hasta que alguien apruebe el cambio: no desaparece del directorio mientras tanto.
        </p>
      );

    default:
      return casoImposible(resultado);
  }
}

interface PropsVistaPrevia {
  readonly negocio: MiNegocio;
  readonly valores: Valores;
  readonly categoria: CategoriaNegocio | undefined;
}

/**
 * Cómo se verá el perfil público con lo que hay escrito **ahora mismo**.
 *
 * No pide nada al servidor: pinta los valores del formulario con la misma forma
 * que la cabecera del perfil. Por eso cambia mientras se escribe, que es lo que
 * pide la historia. La ciudad y el nivel de precio salen del negocio tal cual:
 * este formulario no los toca.
 */
function VistaPrevia({ negocio, valores, categoria }: PropsVistaPrevia) {
  const ubicacion =
    negocio.barrio === null ? negocio.ciudad : `${negocio.barrio}, ${negocio.ciudad}`;
  const nombre = valores.nombre.trim();
  const descripcion = valores.descripcion.trim();
  const telefono = valores.telefono.trim();
  const instagram = valores.instagram.trim();
  const linkedin = valores.linkedin.trim();

  return (
    <aside className={estilos.vistaPrevia} aria-live="polite">
      <h3 className={estilos.tituloVista}>Vista previa</h3>
      <p className={estilos.pieVista}>Así se verá tu perfil público con lo que llevas escrito.</p>

      <div className={estilos.tarjeta}>
        <h4 className={estilos.nombreVista}>{nombre.length === 0 ? 'Sin nombre' : nombre}</h4>
        <p className={estilos.metaVista}>
          {categoria === undefined ? 'Sin categoría' : `${categoria.icono} ${categoria.nombre}`}{' '}
          <span aria-hidden="true">·</span> {ubicacion} <span aria-hidden="true">·</span>{' '}
          {nivelPrecio(negocio.nivelPrecio)}
        </p>
        <p className={estilos.descripcionVista}>
          {descripcion.length === 0 ? 'Sin descripción todavía' : descripcion}
        </p>
        <p className={estilos.telefonoVista}>
          {telefono.length === 0 ? 'Sin teléfono' : telefono}
        </p>

        {/* Los mismos iconos del perfil público, para que la vista previa no
            enseñe una cosa y el perfil otra. Sin enlace no se pintan: allí
            tampoco aparecen. */}
        {(instagram.length > 0 || linkedin.length > 0) && (
          <p className={estilos.redesVista}>
            {instagram.length > 0 && <IconoInstagram tamano={22} />}
            {linkedin.length > 0 && <IconoLinkedIn tamano={22} />}
          </p>
        )}
      </div>
    </aside>
  );
}
