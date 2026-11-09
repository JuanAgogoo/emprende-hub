import { useEffect, useState } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { obtenerCategoriasCurso, obtenerNivelesCurso } from '../api/catalogos';
import { obtenerCurso } from '../api/cursos';
import { visualDelCurso } from '../cursosVisual';
import { useSesion } from '../estado/SesionContext';
import { precio } from '../formato';
import { useTitulo } from '../titulo';
import type { Curso } from '../types/curso';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import formulario from './Formulario.module.css';
import estilos from './CursoDetalle.module.css';

interface Ficha {
  readonly curso: Curso;
  readonly categoria: string;
  readonly nivel: string;
}

/**
 * La ficha de un curso, con su descripción y lo que se aprende.
 *
 * **Pide sesión**, de cliente o de emprendedor: el catálogo se explora sin
 * registrarse, pero empezar un curso es ya usar la plataforma. Sin sesión se
 * manda al login con la vuelta puesta, como hace el bloque de opiniones.
 *
 * Empezar es simulado: los recursos viven fuera (E1) y la plataforma no lleva
 * el progreso (E2), así que el botón confirma el inicio y enseña el primer
 * módulo.
 */
export function CursoDetalle() {
  const { id } = useParams();
  const { sesion } = useSesion();
  const [carga, setCarga] = useState<EstadoCarga<Ficha>>({ estado: 'CARGANDO' });
  const [empezado, setEmpezado] = useState(false);
  useTitulo(carga.estado === 'EXITO' ? carga.datos.curso.titulo : 'Curso');

  useEffect(() => {
    if (sesion === null) return;
    let vigente = true;
    Promise.all([obtenerCurso(Number(id)), obtenerCategoriasCurso(), obtenerNivelesCurso()])
      .then(([curso, categorias, niveles]) => {
        if (!vigente) return;
        setCarga({
          estado: 'EXITO',
          datos: {
            curso,
            categoria: categorias.find((c) => c.codigo === curso.categoria)?.nombre ?? curso.categoria,
            nivel: niveles.find((n) => n.codigo === curso.nivel)?.nombre ?? curso.nivel,
          },
        });
      })
      .catch((fallo: unknown) => {
        if (!vigente) return;
        const mensaje = fallo instanceof Error ? fallo.message : 'No se pudo cargar el curso';
        setCarga({ estado: 'ERROR', mensaje });
      });
    return () => {
      vigente = false;
    };
  }, [id, sesion]);

  if (sesion === null) return <Navigate to="/entrar" replace state={{ volverA: `/cursos/${id}` }} />;

  switch (carga.estado) {
    case 'CARGANDO':
      return (
        <div className={`contenedor ${estilos.pagina}`}>
          <p className={formulario.entrada}>Cargando el curso…</p>
        </div>
      );
    case 'ERROR':
      return (
        <div className={`contenedor ${formulario.pagina}`}>
          <div className={formulario.tarjeta}>
            <h1 className={formulario.titulo}>No encontramos el curso</h1>
            <p className={formulario.fallo} role="alert">
              {carga.mensaje}
            </p>
            <Link to="/cursos">Volver a los cursos</Link>
          </div>
        </div>
      );
    case 'EXITO':
      break;
    default:
      return casoImposible(carga);
  }

  const { curso, categoria, nivel } = carga.datos;
  const visual = visualDelCurso(curso.titulo);
  const gratuito = curso.gratuito || curso.precio === null;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <Link className={estilos.volver} to="/cursos">
        ← Volver a los cursos
      </Link>

      <article className={estilos.ficha}>
        <div className={estilos.portada} aria-hidden="true">
          {visual !== null ? <img src={visual.imagen} alt="" /> : <span>{curso.emoji}</span>}
        </div>

        <div className={estilos.cuerpo}>
          <p className={estilos.etiquetas}>
            <span>{categoria}</span>
            <span>{nivel}</span>
            <span>{curso.duracion}</span>
            <span className={gratuito ? estilos.gratis : estilos.pago}>
              {gratuito ? 'Gratis' : precio(curso.precio ?? 0)}
            </span>
          </p>

          <h1 className={estilos.titulo}>{curso.titulo}</h1>
          <p className={estilos.descripcion}>{curso.descripcion}</p>

          {visual !== null && (
            <section>
              <h2 className={estilos.subtitulo}>Qué vas a aprender</h2>
              <ol className={estilos.temario}>
                {visual.temario.map((tema, i) => (
                  <li key={tema} className={empezado && i === 0 ? estilos.actual : undefined}>
                    <span className={estilos.numero}>{i + 1}</span>
                    {tema}
                  </li>
                ))}
              </ol>
            </section>
          )}

          {empezado ? (
            <p className={formulario.aviso} role="status">
              ¡Ya empezaste, {sesion.nombre.split(' ')[0]}!{' '}
              {visual !== null
                ? `Tu primer módulo es «${visual.temario[0]}».`
                : 'Ya tienes acceso al contenido del curso.'}
            </p>
          ) : gratuito ? (
            <button type="button" className={`${formulario.primario} ${estilos.accion}`} onClick={() => setEmpezado(true)}>
              Empezar el curso
            </button>
          ) : (
            <Link className={`${formulario.primario} ${estilos.accion}`} to={`/cursos/${curso.id}/pago`}>
              Comprar por {precio(curso.precio ?? 0)}
            </Link>
          )}
        </div>
      </article>
    </div>
  );
}
