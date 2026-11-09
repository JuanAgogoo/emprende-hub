import { Fragment, useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { ErrorApi } from '../api/cliente';
import { enviarConsulta, listarConsultasEnviadas } from '../api/consultas';
import { obtenerPerfil } from '../api/directorio';
import { obtenerMiNegocio } from '../api/negocios';
import { obtenerMiOpinion } from '../api/opiniones';
import { IconoMensaje } from '../componentes/IconoMensaje';
import { useSesion } from '../estado/SesionContext';
import { fechaYHora } from '../formato';
import { useTitulo } from '../titulo';
import { MAXIMO_MENSAJE, type Consulta } from '../types/consulta';
import { casoImposible } from '../types/estadoCarga';
import formulario from './Formulario.module.css';
import estilos from './ChatNegocio.module.css';

interface Negocio {
  readonly id: number;
  readonly nombre: string;
  readonly foto: string | null;
}

type Vista =
  | { readonly estado: 'CARGANDO' }
  | { readonly estado: 'ERROR'; readonly mensaje: string }
  | { readonly estado: 'ES_DUENO'; readonly negocio: Negocio }
  | { readonly estado: 'SIN_CALIFICAR'; readonly negocio: Negocio }
  | { readonly estado: 'CHAT'; readonly negocio: Negocio; readonly mensajes: readonly Consulta[] };

/** Un 404 es una respuesta («no hay»), no un fallo. */
async function o404<T>(promesa: Promise<T>): Promise<T | null> {
  try {
    return await promesa;
  } catch (error: unknown) {
    if (error instanceof ErrorApi && error.estado === 404) return null;
    throw error;
  }
}

/** El asunto que pide el buzón sale del principio del propio mensaje. */
function asuntoDe(mensaje: string): string {
  const linea = mensaje.trim().split('\n')[0];
  return linea.length > 60 ? `${linea.slice(0, 57)}…` : linea;
}

/**
 * Escribirle al emprendedor, con forma de chat.
 *
 * **Solo puede escribir quien ya calificó el negocio (D4)**: así el buzón
 * recibe mensajes de clientes reales. La regla vive en el backend; aquí solo se
 * explica antes de que alguien escriba para nada.
 *
 * El emprendedor responde cada mensaje desde su buzón (D2), y la respuesta
 * aparece aquí debajo. Mientras el chat está abierto se consulta cada pocos
 * segundos, así llega sin recargar.
 */
export function ChatNegocio() {
  const { id } = useParams();
  const negocioId = Number(id);
  const { sesion } = useSesion();
  const [vista, setVista] = useState<Vista>({ estado: 'CARGANDO' });
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [fallo, setFallo] = useState<string | null>(null);
  const final = useRef<HTMLDivElement>(null);
  useTitulo(vista.estado === 'CARGANDO' || vista.estado === 'ERROR' ? 'Mensajes' : `Mensajes · ${vista.negocio.nombre}`);

  useEffect(() => {
    if (sesion === null) return;
    let vigente = true;

    async function cargar(): Promise<Vista> {
      const perfil = await obtenerPerfil(negocioId);
      const negocio: Negocio = { id: perfil.id, nombre: perfil.nombre, foto: perfil.fotos[0]?.url ?? null };
      if (sesion?.rol === 'EMPRENDEDOR') {
        const mio = await o404(obtenerMiNegocio());
        if (mio?.id === negocioId) return { estado: 'ES_DUENO', negocio };
      }
      if ((await o404(obtenerMiOpinion(negocioId))) === null) return { estado: 'SIN_CALIFICAR', negocio };
      return { estado: 'CHAT', negocio, mensajes: await listarConsultasEnviadas(negocioId) };
    }

    cargar()
      .then((v) => vigente && setVista(v))
      .catch((error: unknown) => {
        if (!vigente) return;
        setVista({ estado: 'ERROR', mensaje: error instanceof Error ? error.message : 'No se pudo abrir el chat' });
      });
    return () => {
      vigente = false;
    };
  }, [negocioId, sesion]);

  const enChat = vista.estado === 'CHAT';
  useEffect(() => {
    if (!enChat) return;
    const reloj = window.setInterval(() => {
      listarConsultasEnviadas(negocioId)
        .then((mensajes) => setVista((v) => (v.estado === 'CHAT' ? { ...v, mensajes } : v)))
        .catch(() => undefined);
    }, 8000);
    return () => window.clearInterval(reloj);
  }, [enChat, negocioId]);

  // Al llegar un mensaje nuevo, la conversación se desplaza hasta él.
  const cuantos =
    vista.estado === 'CHAT'
      ? vista.mensajes.length + vista.mensajes.filter((m) => m.respuesta !== null).length
      : 0;
  useEffect(() => {
    final.current?.scrollIntoView({ block: 'end' });
  }, [cuantos]);

  if (sesion === null) return <Navigate to="/entrar" replace state={{ volverA: `/negocios/${id}/mensajes` }} />;

  async function enviarMensaje(evento?: FormEvent) {
    evento?.preventDefault();
    if (vista.estado !== 'CHAT') return;
    const mensaje = texto.trim();
    if (mensaje === '' || enviando) return;
    setEnviando(true);
    setFallo(null);
    try {
      const nueva = await enviarConsulta(negocioId, { asunto: asuntoDe(mensaje), mensaje });
      setVista({ ...vista, mensajes: [...vista.mensajes, nueva] });
      setTexto('');
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo enviar el mensaje');
    } finally {
      setEnviando(false);
    }
  }

  // Enter envía; Mayúsculas + Enter hace un salto de línea, como en cualquier chat.
  function alTeclear(evento: KeyboardEvent<HTMLTextAreaElement>) {
    if (evento.key === 'Enter' && !evento.shiftKey) {
      evento.preventDefault();
      void enviarMensaje();
    }
  }

  switch (vista.estado) {
    case 'CARGANDO':
      return (
        <div className={`contenedor ${estilos.pagina}`}>
          <p className={formulario.entrada}>Abriendo la conversación…</p>
        </div>
      );
    case 'ERROR':
      return (
        <div className={`contenedor ${estilos.pagina}`}>
          <p className={formulario.fallo} role="alert">
            {vista.mensaje}
          </p>
          <Link to="/directorio">Volver al directorio</Link>
        </div>
      );
    case 'ES_DUENO':
    case 'SIN_CALIFICAR':
    case 'CHAT':
      break;
    default:
      return casoImposible(vista);
  }

  const { negocio } = vista;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <Link className={estilos.volver} to={`/negocios/${negocio.id}`}>
        ← Volver al perfil
      </Link>

      <section className={estilos.chat} aria-label={`Conversación con ${negocio.nombre}`}>
        <header className={estilos.cabecera}>
          {negocio.foto !== null ? (
            <img className={estilos.avatar} src={negocio.foto} alt="" />
          ) : (
            <span className={estilos.avatar} aria-hidden="true">
              {negocio.nombre.charAt(0)}
            </span>
          )}
          <div>
            <h1 className={estilos.nombre}>{negocio.nombre}</h1>
            <p className={estilos.subtitulo}>Te responde por aquí</p>
          </div>
        </header>

        {vista.estado === 'ES_DUENO' && (
          <div className={estilos.aviso}>
            <p>Este es tu negocio. Los mensajes de tus clientes llegan a tu buzón.</p>
            <Link className={`${formulario.primario} ${estilos.boton}`} to="/buzon">
              Ir a mi buzón
            </Link>
          </div>
        )}

        {vista.estado === 'SIN_CALIFICAR' && (
          <div className={estilos.aviso}>
            <IconoMensaje tamano={56} />
            <p>
              <strong>Primero califica el negocio.</strong> Solo pueden escribirle al emprendedor
              quienes ya le dieron su valoración.
            </p>
            <Link className={`${formulario.primario} ${estilos.boton}`} to={`/negocios/${negocio.id}#opiniones`}>
              Calificar el negocio
            </Link>
          </div>
        )}

        {vista.estado === 'CHAT' && (
          <>
            <div className={estilos.mensajes} aria-live="polite">
              {vista.mensajes.length === 0 ? (
                <p className={estilos.vacio}>
                  Escríbele a {negocio.nombre}: lo recibe en su buzón y su respuesta aparece aquí.
                </p>
              ) : (
                vista.mensajes.map((m) => (
                  <Fragment key={m.id}>
                    <div className={estilos.burbuja}>
                      <p>{m.mensaje}</p>
                      <span className={estilos.hora}>
                        {fechaYHora(m.fechaEnvio)} · {m.leida ? 'Leído ✓✓' : 'Enviado ✓'}
                      </span>
                    </div>
                    {m.respuesta !== null && m.fechaRespuesta !== null && (
                      <div className={estilos.respuesta}>
                        <span className={estilos.autor}>{negocio.nombre}</span>
                        <p>{m.respuesta}</p>
                        <span className={estilos.hora}>{fechaYHora(m.fechaRespuesta)}</span>
                      </div>
                    )}
                  </Fragment>
                ))
              )}
              <div ref={final} />
            </div>

            <form className={estilos.redactor} onSubmit={enviarMensaje}>
              {fallo !== null && (
                <p className={formulario.fallo} role="alert">
                  {fallo}
                </p>
              )}
              <label className={estilos.oculto} htmlFor="mensaje-chat">
                Mensaje
              </label>
              <textarea
                id="mensaje-chat"
                rows={2}
                maxLength={MAXIMO_MENSAJE}
                placeholder="Escribe tu mensaje…"
                value={texto}
                onChange={(e) => setTexto(e.target.value)}
                onKeyDown={alTeclear}
              />
              <div className={estilos.pie}>
                <small>
                  {texto.length}/{MAXIMO_MENSAJE}
                </small>
                <button className={formulario.primario} type="submit" disabled={enviando || texto.trim() === ''}>
                  {enviando ? 'Enviando…' : 'Enviar'}
                </button>
              </div>
            </form>
          </>
        )}
      </section>
    </div>
  );
}
