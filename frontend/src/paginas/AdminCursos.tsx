import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { eliminarCurso, listarCursosAdmin, publicarCurso, retirarCurso } from '../api/cursos';
import { avisoDeNavegacion } from '../avisoDeNavegacion';
import { ConfirmarBorrado } from '../componentes/ConfirmarBorrado';
import { EsqueletoLista } from '../componentes/Esqueleto';
import { Paginacion } from '../componentes/Paginacion';
import { precio } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { Curso } from '../types/curso';
import type { Pagina } from '../types/pagina';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

/**
 * Los cursos del catálogo, borradores incluidos (HU-039).
 *
 * Desde aquí se publican, se retiran y se borran; crearlos y editarlos tiene su
 * propia pantalla, porque el formulario no cabe en una fila.
 */
export function AdminCursos() {
  useTitulo('Cursos del catálogo');
  const avisoAlVolver = avisoDeNavegacion(useLocation().state);

  const [pagina, setPagina] = useState(0);
  const [carga, setCarga] = useState<EstadoCarga<Pagina<Curso>>>({ estado: 'CARGANDO' });
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [porBorrar, setPorBorrar] = useState<Curso | null>(null);
  const [aviso, setAviso] = useState<string | null>(avisoAlVolver);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });
    listarCursosAdmin(pagina)
      .then((datos) => {
        if (vigente) setCarga({ estado: 'EXITO', datos });
      })
      .catch((error: unknown) => {
        const mensaje = error instanceof Error ? error.message : 'No se pudieron cargar';
        if (vigente) setCarga({ estado: 'ERROR', mensaje });
      });
    return () => {
      vigente = false;
    };
  }, [pagina]);

  /** Sustituye un curso en la lista por cómo quedó en el backend. */
  function sustituir(curso: Curso) {
    setCarga((actual) =>
      actual.estado === 'EXITO'
        ? {
            ...actual,
            datos: {
              ...actual.datos,
              content: actual.datos.content.map((c) => (c.id === curso.id ? curso : c)),
            },
          }
        : actual,
    );
  }

  async function alternarPublicacion(curso: Curso) {
    setOcupado(curso.id);
    setFallo(null);
    setAviso(null);
    try {
      const publicado = curso.estado === 'BORRADOR';
      sustituir(publicado ? await publicarCurso(curso.id) : await retirarCurso(curso.id));
      setAviso(
        publicado
          ? `«${curso.titulo}» ya está en el catálogo de cursos.`
          : `«${curso.titulo}» salió del catálogo y vuelve a ser un borrador.`,
      );
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo cambiar el estado');
    } finally {
      setOcupado(null);
    }
  }

  async function borrar() {
    if (porBorrar === null) return;
    const curso = porBorrar;
    setOcupado(curso.id);
    setFallo(null);
    setAviso(null);
    try {
      await eliminarCurso(curso.id);
      setCarga((actual) =>
        actual.estado === 'EXITO'
          ? {
              ...actual,
              datos: {
                ...actual.datos,
                content: actual.datos.content.filter((c) => c.id !== curso.id),
                totalElements: actual.datos.totalElements - 1,
              },
            }
          : actual,
      );
      setAviso(`«${curso.titulo}» se eliminó.`);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo eliminar el curso');
    } finally {
      setOcupado(null);
      setPorBorrar(null);
    }
  }

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-cursos">
      <div className={estilos.cabeceraSeccion}>
        <h2 id="titulo-cursos" className={estilos.tituloSeccion}>
          Cursos del catálogo
        </h2>
        <Link to="/admin/cursos/nuevo" className={estilos.primario}>
          Nuevo curso
        </Link>
      </div>

      {aviso !== null && (
        <p className={estilos.aviso} role="status">
          {aviso}
        </p>
      )}
      {fallo !== null && (
        <p className={estilos.error} role="alert">
          {fallo}
        </p>
      )}

      <ListaCursos
        carga={carga}
        ocupado={ocupado}
        alAlternar={alternarPublicacion}
        alBorrar={setPorBorrar}
        alPaginar={setPagina}
      />

      {porBorrar !== null && (
        <ConfirmarBorrado
          titulo={`¿Eliminar «${porBorrar.titulo}»?`}
          texto={
            porBorrar.estado === 'PUBLICADO'
              ? 'Está publicado: desaparecerá del catálogo y no se puede deshacer. Si solo quieres sacarlo del catálogo, retíralo.'
              : 'Es un borrador, así que no está en el catálogo. Aun así, borrarlo no se puede deshacer.'
          }
          ocupado={ocupado !== null}
          alConfirmar={borrar}
          alCancelar={() => setPorBorrar(null)}
        />
      )}
    </section>
  );
}

interface PropsLista {
  readonly carga: EstadoCarga<Pagina<Curso>>;
  readonly ocupado: number | null;
  readonly alAlternar: (curso: Curso) => void;
  readonly alBorrar: (curso: Curso) => void;
  readonly alPaginar: (pagina: number) => void;
}

function ListaCursos({ carga, ocupado, alAlternar, alBorrar, alPaginar }: PropsLista) {
  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div aria-busy="true">
          <EsqueletoLista cuantas={4} />
        </div>
      );

    case 'ERROR':
      return (
        <p className={estilos.error} role="alert">
          No se pudieron cargar los cursos: {carga.mensaje}. Comprueba que la API esté
          funcionando y vuelve a intentarlo.
        </p>
      );

    case 'EXITO': {
      const { content: cursos, totalElements: total } = carga.datos;

      if (total === 0) {
        return (
          <div className={estilos.vacio}>
            <p className={estilos.tituloVacio}>Todavía no hay cursos</p>
            <p>Crea el primero con «Nuevo curso».</p>
          </div>
        );
      }

      return (
        <>
          <p className={estilos.recuento}>
            {total === 1 ? '1 curso' : `${total} cursos`}, borradores incluidos
          </p>
          <ul className={estilos.lista}>
            {cursos.map((curso) => (
              <li key={curso.id} className={estilos.fila}>
                <div className={estilos.filaCuerpo}>
                  <h3 className={estilos.nombre}>
                    <span aria-hidden="true">{curso.emoji}</span> {curso.titulo}
                  </h3>
                  <dl className={estilos.datos}>
                    <div>
                      <dt>Acceso</dt>
                      <dd>
                        {curso.gratuito || curso.precio === null ? 'Gratis' : precio(curso.precio)}
                      </dd>
                    </div>
                    <div>
                      <dt>Duración</dt>
                      <dd>{curso.duracion}</dd>
                    </div>
                    <div>
                      <dt>Estado</dt>
                      <dd>
                        {curso.estado === 'PUBLICADO' ? (
                          <span className={`${estilos.insignia} ${estilos.insigniaExito}`}>
                            <span aria-hidden="true">●</span> Publicado
                          </span>
                        ) : (
                          <span className={`${estilos.insignia} ${estilos.insigniaNeutra}`}>
                            <span aria-hidden="true">○</span> Borrador
                          </span>
                        )}
                      </dd>
                    </div>
                  </dl>
                </div>

                <div className={estilos.acciones}>
                  <Link
                    to={`/admin/cursos/${curso.id}`}
                    className={estilos.secundario}
                    aria-label={`Editar ${curso.titulo}`}
                  >
                    Editar
                  </Link>
                  <button
                    type="button"
                    className={estilos.secundario}
                    disabled={ocupado !== null}
                    onClick={() => alAlternar(curso)}
                    aria-label={
                      curso.estado === 'BORRADOR'
                        ? `Publicar ${curso.titulo}`
                        : `Retirar ${curso.titulo} del catálogo`
                    }
                  >
                    {curso.estado === 'BORRADOR' ? 'Publicar' : 'Retirar'}
                  </button>
                  <button
                    type="button"
                    className={estilos.rechazar}
                    disabled={ocupado !== null}
                    onClick={() => alBorrar(curso)}
                    aria-label={`Eliminar ${curso.titulo}`}
                  >
                    Eliminar
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <Paginacion
            pagina={carga.datos.number}
            totalPaginas={carga.datos.totalPages}
            alCambiar={alPaginar}
          />
        </>
      );
    }

    default:
      return casoImposible(carga);
  }
}
