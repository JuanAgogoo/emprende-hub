import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { obtenerCategoriasCurso, obtenerNivelesCurso } from '../api/catalogos';
import { ErrorApi } from '../api/cliente';
import {
  actualizarCurso,
  crearCurso,
  obtenerCursoAdmin,
  publicarCurso,
  retirarCurso,
} from '../api/cursos';
import type { Opcion } from '../types/catalogo';
import type {
  CategoriaCurso,
  Curso,
  DatosCurso,
  EstadoCurso,
  NivelCurso,
} from '../types/curso';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';
import formulario from './Formulario.module.css';

const LISTA = '/admin/cursos';

/**
 * Lo que tiene el formulario mientras se escribe. Todo es texto, como lo dan
 * los campos; se convierte a los tipos del curso solo al enviar.
 */
interface Valores {
  readonly titulo: string;
  readonly descripcion: string;
  readonly duracion: string;
  readonly categoria: CategoriaCurso | '';
  readonly nivel: NivelCurso | '';
  readonly gratuito: boolean;
  readonly precio: string;
  readonly urlRecurso: string;
  readonly emoji: string;
  readonly estado: EstadoCurso;
}

type Errores = Partial<Record<keyof Valores, string>>;

const VACIO: Valores = {
  titulo: '',
  descripcion: '',
  duracion: '',
  categoria: '',
  nivel: '',
  gratuito: true,
  precio: '',
  urlRecurso: '',
  emoji: '',
  // Lo que pide HU-039: se puede guardar sin publicar.
  estado: 'BORRADOR',
};

/** Los mismos topes que `CrearCursoRequest`: aquí se avisa antes de enviar. */
const MAXIMOS = { titulo: 150, descripcion: 1000, duracion: 40, urlRecurso: 500, emoji: 8 };

function deCurso(curso: Curso): Valores {
  return {
    titulo: curso.titulo,
    descripcion: curso.descripcion,
    duracion: curso.duracion,
    categoria: curso.categoria,
    nivel: curso.nivel,
    gratuito: curso.gratuito,
    precio: curso.precio === null ? '' : String(curso.precio),
    urlRecurso: curso.urlRecurso,
    emoji: curso.emoji,
    estado: curso.estado,
  };
}

function esUrlWeb(texto: string): boolean {
  try {
    const url = new URL(texto);
    return url.protocol === 'https:' || url.protocol === 'http:';
  } catch {
    return false;
  }
}

/** Validación de forma, pura y fuera del componente, como en el resto de formularios. */
function validar(valores: Valores): Errores {
  const errores: Errores = {};

  if (valores.titulo.trim() === '') errores.titulo = 'El título es obligatorio';
  else if (valores.titulo.length > MAXIMOS.titulo)
    errores.titulo = `El título no puede pasar de ${MAXIMOS.titulo} caracteres`;

  if (valores.descripcion.trim() === '') errores.descripcion = 'La descripción es obligatoria';
  else if (valores.descripcion.length > MAXIMOS.descripcion)
    errores.descripcion = `La descripción no puede pasar de ${MAXIMOS.descripcion} caracteres`;

  if (valores.duracion.trim() === '') errores.duracion = 'La duración es obligatoria';
  else if (valores.duracion.length > MAXIMOS.duracion)
    errores.duracion = `La duración no puede pasar de ${MAXIMOS.duracion} caracteres`;

  if (valores.categoria === '') errores.categoria = 'Elige una categoría';
  if (valores.nivel === '') errores.nivel = 'Elige un nivel';

  // La regla que cruza dos campos, la misma que aplica el servicio del backend.
  if (!valores.gratuito) {
    const numero = Number(valores.precio);
    if (valores.precio.trim() === '') errores.precio = 'Un curso de pago tiene que llevar precio';
    else if (!Number.isFinite(numero) || numero <= 0)
      errores.precio = 'El precio debe ser mayor que cero';
  }

  if (valores.urlRecurso.trim() === '') errores.urlRecurso = 'La URL del recurso es obligatoria';
  else if (!esUrlWeb(valores.urlRecurso.trim()))
    errores.urlRecurso = 'Tiene que ser una dirección web completa, con https://';
  else if (valores.urlRecurso.length > MAXIMOS.urlRecurso)
    errores.urlRecurso = `La URL no puede pasar de ${MAXIMOS.urlRecurso} caracteres`;

  if (valores.emoji.trim() === '') errores.emoji = 'El emoji es obligatorio';
  else if (valores.emoji.length > MAXIMOS.emoji) errores.emoji = 'Un emoji, no una frase';

  return errores;
}

/** El cuerpo que espera el backend. Solo se llama con valores ya validados. */
function aDatos(valores: Valores): DatosCurso {
  return {
    titulo: valores.titulo.trim(),
    descripcion: valores.descripcion.trim(),
    duracion: valores.duracion.trim(),
    categoria: valores.categoria as CategoriaCurso,
    nivel: valores.nivel as NivelCurso,
    gratuito: valores.gratuito,
    // Un curso gratuito no puede llevar precio: el backend lo rechazaría.
    precio: valores.gratuito ? null : Number(valores.precio),
    urlRecurso: valores.urlRecurso.trim(),
    emoji: valores.emoji.trim(),
  };
}

/**
 * Crear y editar un curso (HU-039). La misma pantalla para las dos cosas: con
 * `:id` en la ruta carga el curso y lo edita; sin él, empieza en blanco.
 *
 * El estado no viaja en el cuerpo: el backend lo cambia con sus propias rutas.
 * Por eso guardar puede ser dos peticiones —los datos y luego publicar o
 * retirar— y, si la segunda falla, se dice que los datos sí se guardaron.
 */
export function AdminFormularioCurso() {
  const { id } = useParams();
  const navegar = useNavigate();
  const identificador = id === undefined ? null : Number(id);
  const editando = identificador !== null;
  useTitulo(editando ? 'Editar curso' : 'Nuevo curso');

  const [categorias, setCategorias] = useState<readonly Opcion<CategoriaCurso>[]>([]);
  const [niveles, setNiveles] = useState<readonly Opcion<NivelCurso>[]>([]);
  const [original, setOriginal] = useState<Curso | null>(null);
  const [valores, setValores] = useState<Valores>(VACIO);
  const [cargando, setCargando] = useState(editando);
  const [noExiste, setNoExiste] = useState(false);

  const [enviado, setEnviado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const [delServidor, setDelServidor] = useState<Errores>({});

  useEffect(() => {
    let vigente = true;
    Promise.all([obtenerCategoriasCurso(), obtenerNivelesCurso()])
      .then(([cats, nivs]) => {
        if (!vigente) return;
        setCategorias(cats);
        setNiveles(nivs);
      })
      .catch(() => {
        if (vigente) setFallo('No se pudieron cargar las categorías y los niveles.');
      });
    return () => {
      vigente = false;
    };
  }, []);

  useEffect(() => {
    if (identificador === null) return;
    let vigente = true;

    if (!Number.isInteger(identificador) || identificador <= 0) {
      setNoExiste(true);
      setCargando(false);
      return;
    }

    obtenerCursoAdmin(identificador)
      .then((curso) => {
        if (!vigente) return;
        setOriginal(curso);
        setValores(deCurso(curso));
      })
      .catch((error: unknown) => {
        if (!vigente) return;
        if (error instanceof ErrorApi && error.estado === 404) setNoExiste(true);
        else setFallo(error instanceof Error ? error.message : 'No se pudo cargar el curso');
      })
      .finally(() => {
        if (vigente) setCargando(false);
      });

    return () => {
      vigente = false;
    };
  }, [identificador]);

  const propios = validar(valores);
  const hayErrores = Object.keys(propios).length > 0;

  /**
   * El error que se enseña junto a un campo. Los propios, solo tras intentar
   * enviar (el `markAllAsTouched()` del curso); los del backend, en cuanto
   * llegan, y desaparecen al tocar el campo.
   */
  function ver(campo: keyof Valores): string | undefined {
    return delServidor[campo] ?? (enviado ? propios[campo] : undefined);
  }

  function cambiar<K extends keyof Valores>(campo: K, valor: Valores[K]) {
    setValores((previos) => ({ ...previos, [campo]: valor }));
    setDelServidor((previos) => ({ ...previos, [campo]: undefined }));
  }

  async function alEnviar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    setFallo(null);
    setDelServidor({});
    if (hayErrores) return;

    setEnviando(true);
    let guardado: Curso;
    try {
      guardado =
        identificador === null
          ? await crearCurso(aDatos(valores))
          : await actualizarCurso(identificador, aDatos(valores));
    } catch (error: unknown) {
      if (error instanceof ErrorApi && Object.keys(error.porCampo).length > 0) {
        setDelServidor(error.porCampo);
      }
      setFallo(error instanceof Error ? error.message : 'No se pudo guardar el curso');
      setEnviando(false);
      return;
    }

    // Los datos ya están guardados. Falta, si cambió, el estado.
    try {
      if (valores.estado !== guardado.estado) {
        guardado =
          valores.estado === 'PUBLICADO'
            ? await publicarCurso(guardado.id)
            : await retirarCurso(guardado.id);
      }
    } catch (error: unknown) {
      const motivo = error instanceof Error ? error.message : 'error desconocido';
      navegar(LISTA, {
        state: {
          aviso: `«${guardado.titulo}» se guardó, pero sigue como ${guardado.estado === 'PUBLICADO' ? 'publicado' : 'borrador'}: no se pudo cambiar su estado (${motivo}).`,
        },
      });
      return;
    }

    navegar(LISTA, {
      state: {
        aviso:
          guardado.estado === 'PUBLICADO'
            ? `«${guardado.titulo}» está guardado y publicado en el catálogo.`
            : `«${guardado.titulo}» está guardado como borrador: no sale en el catálogo.`,
      },
    });
  }

  if (noExiste) {
    return (
      <div className={estilos.vacio}>
        <p className={estilos.tituloVacio}>Ese curso no existe</p>
        <Link to={LISTA}>Volver a los cursos</Link>
      </div>
    );
  }

  if (cargando) return <div className={estilos.esqueletoRevision} aria-busy="true" />;

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-formulario-curso">
      <p className={estilos.migas}>
        <Link to={LISTA}>Cursos del catálogo</Link> <span aria-hidden="true">/</span>{' '}
        <span>{original?.titulo ?? 'Nuevo curso'}</span>
      </p>
      <h2 id="titulo-formulario-curso" className={estilos.tituloSeccion}>
        {editando ? 'Editar curso' : 'Nuevo curso'}
      </h2>

      <form className={`${formulario.formulario} ${estilos.formularioCurso}`} onSubmit={alEnviar} noValidate>
        {fallo !== null && (
          <p className={formulario.fallo} role="alert">
            {fallo}
          </p>
        )}

        <Campo id="titulo" etiqueta="Título" error={ver('titulo')}>
          <input
            id="titulo"
            value={valores.titulo}
            onChange={(e) => cambiar('titulo', e.target.value)}
            aria-invalid={ver('titulo') !== undefined}
            aria-describedby={ver('titulo') !== undefined ? 'error-titulo' : undefined}
          />
        </Campo>

        <Campo
          id="descripcion"
          etiqueta="Descripción"
          error={ver('descripcion')}
          ayuda={`${valores.descripcion.length} / ${MAXIMOS.descripcion}`}
        >
          <textarea
            id="descripcion"
            rows={4}
            value={valores.descripcion}
            onChange={(e) => cambiar('descripcion', e.target.value)}
            aria-invalid={ver('descripcion') !== undefined}
            aria-describedby={ver('descripcion') !== undefined ? 'error-descripcion' : 'ayuda-descripcion'}
          />
        </Campo>

        <div className={estilos.dosColumnas}>
          <Campo id="categoria" etiqueta="Categoría" error={ver('categoria')}>
            <select
              id="categoria"
              value={valores.categoria}
              onChange={(e) => cambiar('categoria', e.target.value as CategoriaCurso | '')}
              aria-invalid={ver('categoria') !== undefined}
              aria-describedby={ver('categoria') !== undefined ? 'error-categoria' : undefined}
            >
              <option value="">Elige una</option>
              {categorias.map((c) => (
                <option key={c.codigo} value={c.codigo}>
                  {c.nombre}
                </option>
              ))}
            </select>
          </Campo>

          <Campo id="nivel" etiqueta="Nivel" error={ver('nivel')}>
            <select
              id="nivel"
              value={valores.nivel}
              onChange={(e) => cambiar('nivel', e.target.value as NivelCurso | '')}
              aria-invalid={ver('nivel') !== undefined}
              aria-describedby={ver('nivel') !== undefined ? 'error-nivel' : undefined}
            >
              <option value="">Elige uno</option>
              {niveles.map((n) => (
                <option key={n.codigo} value={n.codigo}>
                  {n.nombre}
                </option>
              ))}
            </select>
          </Campo>

          <Campo id="duracion" etiqueta="Duración" error={ver('duracion')}>
            <input
              id="duracion"
              placeholder="4 horas"
              value={valores.duracion}
              onChange={(e) => cambiar('duracion', e.target.value)}
              aria-invalid={ver('duracion') !== undefined}
              aria-describedby={ver('duracion') !== undefined ? 'error-duracion' : undefined}
            />
          </Campo>

          <Campo id="emoji" etiqueta="Emoji de la tarjeta" error={ver('emoji')}>
            <input
              id="emoji"
              placeholder="📊"
              value={valores.emoji}
              onChange={(e) => cambiar('emoji', e.target.value)}
              aria-invalid={ver('emoji') !== undefined}
              aria-describedby={ver('emoji') !== undefined ? 'error-emoji' : undefined}
            />
          </Campo>
        </div>

        <fieldset className={estilos.grupo}>
          <legend>Tipo</legend>
          <label className={formulario.casilla}>
            <input
              type="radio"
              name="tipo"
              checked={valores.gratuito}
              onChange={() => cambiar('gratuito', true)}
            />
            Gratuito
          </label>
          <label className={formulario.casilla}>
            <input
              type="radio"
              name="tipo"
              checked={!valores.gratuito}
              onChange={() => cambiar('gratuito', false)}
            />
            De pago
          </label>
        </fieldset>

        {/* El precio solo existe para uno de pago: uno gratuito no puede llevarlo. */}
        {!valores.gratuito && (
          <Campo id="precio" etiqueta="Precio, en pesos" error={ver('precio')}>
            <input
              id="precio"
              type="number"
              inputMode="numeric"
              min={1}
              step={1}
              value={valores.precio}
              onChange={(e) => cambiar('precio', e.target.value)}
              aria-invalid={ver('precio') !== undefined}
              aria-describedby={ver('precio') !== undefined ? 'error-precio' : undefined}
            />
          </Campo>
        )}

        <Campo
          id="urlRecurso"
          etiqueta="URL del recurso"
          error={ver('urlRecurso')}
          ayuda="A dónde lleva el botón de la tarjeta. La plataforma no aloja ni cobra el curso."
        >
          <input
            id="urlRecurso"
            type="url"
            placeholder="https://"
            value={valores.urlRecurso}
            onChange={(e) => cambiar('urlRecurso', e.target.value)}
            aria-invalid={ver('urlRecurso') !== undefined}
            aria-describedby={ver('urlRecurso') !== undefined ? 'error-urlRecurso' : 'ayuda-urlRecurso'}
          />
        </Campo>

        <fieldset className={estilos.grupo}>
          <legend>Estado</legend>
          <label className={formulario.casilla}>
            <input
              type="radio"
              name="estado"
              checked={valores.estado === 'BORRADOR'}
              onChange={() => cambiar('estado', 'BORRADOR')}
            />
            Borrador: se guarda sin salir en el catálogo
          </label>
          <label className={formulario.casilla}>
            <input
              type="radio"
              name="estado"
              checked={valores.estado === 'PUBLICADO'}
              onChange={() => cambiar('estado', 'PUBLICADO')}
            />
            Publicado: sale en el catálogo de cursos al momento
          </label>
        </fieldset>

        <div className={estilos.acciones}>
          <button className={formulario.primario} type="submit" disabled={enviando}>
            {enviando ? 'Guardando…' : 'Guardar'}
          </button>
          <Link to={LISTA} className={estilos.secundario}>
            Cancelar
          </Link>
        </div>
      </form>
    </section>
  );
}

interface PropsCampo {
  readonly id: keyof Valores;
  readonly etiqueta: string;
  readonly error: string | undefined;
  readonly ayuda?: string;
  readonly children: ReactNode;
}

/** Etiqueta, control y, debajo, el error o la ayuda: lo mismo en los ocho campos. */
function Campo({ id, etiqueta, error, ayuda, children }: PropsCampo) {
  return (
    <div className={formulario.campo}>
      <label htmlFor={id}>{etiqueta}</label>
      {children}
      {error !== undefined ? (
        <small id={`error-${id}`} className={formulario.error}>
          {error}
        </small>
      ) : (
        ayuda !== undefined && (
          <small id={`ayuda-${id}`} className={formulario.ayuda}>
            {ayuda}
          </small>
        )
      )}
    </div>
  );
}
