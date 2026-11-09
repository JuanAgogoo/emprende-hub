import { useEffect, useState, type FormEvent, type InputHTMLAttributes } from 'react';
import { Link, useParams } from 'react-router-dom';
import { obtenerCurso } from '../api/cursos';
import { visualDelCurso } from '../cursosVisual';
import { precio } from '../formato';
import { useTitulo } from '../titulo';
import type { Curso } from '../types/curso';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import formulario from './Formulario.module.css';
import estilos from './PagoCurso.module.css';

interface Tarjeta {
  readonly titular: string;
  readonly numero: string;
  readonly vencimiento: string;
  readonly cvv: string;
}

type Errores = Partial<Record<keyof Tarjeta, string>>;

const VACIA: Tarjeta = { titular: '', numero: '', vencimiento: '', cvv: '' };

function soloDigitos(texto: string): string {
  return texto.replace(/\D/g, '');
}

/** El dígito de control que llevan todas las tarjetas (algoritmo de Luhn). */
function pasaLuhn(digitos: string): boolean {
  let suma = 0;
  for (let i = 0; i < digitos.length; i++) {
    let d = Number(digitos[digitos.length - 1 - i]);
    if (i % 2 === 1) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    suma += d;
  }
  return suma % 10 === 0;
}

/** La franquicia se deduce de los primeros dígitos, como en cualquier pasarela. */
function franquicia(digitos: string): string | null {
  if (/^4/.test(digitos)) return 'Visa';
  if (/^(5[1-5]|2[2-7])/.test(digitos)) return 'Mastercard';
  if (/^3[47]/.test(digitos)) return 'American Express';
  return null;
}

/** «4111111111111111» → «4111 1111 1111 1111». */
function agrupar(digitos: string): string {
  return digitos.replace(/(\d{4})(?=\d)/g, '$1 ');
}

/** «1228» → «12/28», según se escribe. */
function formatearVencimiento(texto: string): string {
  const d = soloDigitos(texto).slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
}

function validar(t: Tarjeta, hoy: Date): Errores {
  const errores: Errores = {};
  if (t.titular.trim().length < 3) errores.titular = 'Escribe el nombre como aparece en la tarjeta';

  const numero = soloDigitos(t.numero);
  if (numero.length < 13 || numero.length > 19) errores.numero = 'El número debe tener entre 13 y 19 dígitos';
  else if (!pasaLuhn(numero)) errores.numero = 'El número de la tarjeta no es válido';

  const venc = /^(\d{2})\/(\d{2})$/.exec(t.vencimiento);
  if (venc === null) errores.vencimiento = 'Usa el formato MM/AA';
  else {
    const mes = Number(venc[1]);
    const anio = 2000 + Number(venc[2]);
    // Vale hasta el último día de su mes: se compara con el mes siguiente.
    if (mes < 1 || mes > 12) errores.vencimiento = 'El mes no existe';
    else if (new Date(anio, mes, 1) <= hoy) errores.vencimiento = 'La tarjeta está vencida';
  }

  const largoCvv = franquicia(numero) === 'American Express' ? 4 : 3;
  if (soloDigitos(t.cvv).length !== largoCvv) errores.cvv = `El código tiene ${largoCvv} dígitos`;
  return errores;
}

/**
 * Pago de un curso de pago. **Es una simulación**: la plataforma no cobra (E1)
 * y no hay pasarela integrada, así que los datos de la tarjeta se validan en el
 * navegador y no salen de él. Ni se envían al backend ni se guardan: al aprobar
 * el pago se borran del estado y solo queda una referencia inventada.
 */
export function PagoCurso() {
  useTitulo('Pagar el curso');
  const { id } = useParams();

  const [curso, setCurso] = useState<EstadoCarga<Curso>>({ estado: 'CARGANDO' });
  const [tarjeta, setTarjeta] = useState<Tarjeta>(VACIA);
  const [enviado, setEnviado] = useState(false);
  const [procesando, setProcesando] = useState(false);
  const [referencia, setReferencia] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    obtenerCurso(Number(id))
      .then((datos) => vigente && setCurso({ estado: 'EXITO', datos }))
      .catch((fallo: unknown) => {
        if (!vigente) return;
        const mensaje = fallo instanceof Error ? fallo.message : 'No se pudo cargar el curso';
        setCurso({ estado: 'ERROR', mensaje });
      });
    return () => {
      vigente = false;
    };
  }, [id]);

  const errores = validar(tarjeta, new Date());
  const hayErrores = Object.keys(errores).length > 0;
  const digitos = soloDigitos(tarjeta.numero);
  const marca = franquicia(digitos);

  function cambiar(campo: keyof Tarjeta, valor: string) {
    setTarjeta((actual) => ({ ...actual, [campo]: valor }));
  }

  function alPagar(evento: FormEvent) {
    evento.preventDefault();
    setEnviado(true);
    if (hayErrores) {
      // El foco salta al primer campo que falla, como en el resto de formularios.
      const primero = (['titular', 'numero', 'vencimiento', 'cvv'] as const).find((c) => errores[c]);
      if (primero) document.getElementById(`pago-${primero}`)?.focus();
      return;
    }
    setProcesando(true);
    window.setTimeout(() => {
      setReferencia(`EH-${Date.now().toString(36).toUpperCase()}`);
      setTarjeta(VACIA);
      setProcesando(false);
    }, 1400);
  }

  function campo(nombre: keyof Tarjeta, etiqueta: string, props: InputHTMLAttributes<HTMLInputElement>) {
    const conError = enviado && errores[nombre] !== undefined;
    return (
      <div className={formulario.campo}>
        <label htmlFor={`pago-${nombre}`}>{etiqueta}</label>
        <input
          id={`pago-${nombre}`}
          name={nombre}
          value={tarjeta[nombre]}
          aria-invalid={conError}
          aria-describedby={conError ? `error-${nombre}` : undefined}
          {...props}
        />
        {conError && (
          <small id={`error-${nombre}`} className={formulario.error}>
            {errores[nombre]}
          </small>
        )}
      </div>
    );
  }

  switch (curso.estado) {
    case 'CARGANDO':
      return (
        <div className={`contenedor ${formulario.pagina}`}>
          <p className={formulario.entrada}>Cargando el curso…</p>
        </div>
      );
    case 'ERROR':
      return (
        <div className={`contenedor ${formulario.pagina}`}>
          <div className={formulario.tarjeta}>
            <h1 className={formulario.titulo}>No encontramos el curso</h1>
            <p className={formulario.fallo} role="alert">
              {curso.mensaje}
            </p>
            <Link to="/cursos">Volver a los cursos</Link>
          </div>
        </div>
      );
    case 'EXITO':
      break;
    default:
      return casoImposible(curso);
  }

  const c = curso.datos;
  const visual = visualDelCurso(c.titulo);
  const total = c.gratuito || c.precio === null ? 0 : c.precio;

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <Link className={estilos.volver} to="/cursos">
        ← Volver a los cursos
      </Link>

      <div className={estilos.rejilla}>
        <aside className={estilos.resumen} aria-label="Resumen de la compra">
          {visual !== null ? (
            <img className={estilos.portada} src={visual.imagen} alt="" />
          ) : (
            <div className={estilos.emoji} aria-hidden="true">
              {c.emoji}
            </div>
          )}
          <h2 className={estilos.cursoTitulo}>{c.titulo}</h2>
          <p className={estilos.cursoDescripcion}>{c.descripcion}</p>
          <dl className={estilos.lineas}>
            <div>
              <dt>Duración</dt>
              <dd>{c.duracion}</dd>
            </div>
            <div>
              <dt>Precio</dt>
              <dd>{precio(total)}</dd>
            </div>
            <div className={estilos.total}>
              <dt>Total a pagar</dt>
              <dd>{precio(total)}</dd>
            </div>
          </dl>
        </aside>

        <section className={formulario.tarjeta}>
          {referencia !== null ? (
            <>
              <h1 className={formulario.titulo}>¡Pago aprobado!</h1>
              <p className={formulario.aviso} role="status">
                Ya tienes acceso a <strong>{c.titulo}</strong>. Referencia de la compra:{' '}
                <strong>{referencia}</strong>.
              </p>
              <p className={formulario.ayuda}>
                Pago de demostración: no se realizó ningún cobro y los datos de la tarjeta no se
                guardaron.
              </p>
              <Link className={`${formulario.primario} ${estilos.botonEnlace}`} to={`/cursos/${c.id}`}>
                Ir al curso
              </Link>
            </>
          ) : (
            <>
              <h1 className={formulario.titulo}>Pago con tarjeta</h1>
              <p className={estilos.demo}>
                <strong>Modo demostración.</strong> No se cobra nada: los datos se validan en tu
                navegador y no se envían ni se guardan. Prueba con la tarjeta 4111 1111 1111 1111.
              </p>

              {/* La tarjeta dibujada se rellena según se escribe. */}
              <div className={estilos.plastico} aria-hidden="true">
                <span className={estilos.marca}>{marca ?? 'Tarjeta'}</span>
                <span className={estilos.numero}>
                  {digitos.length > 0 ? agrupar(digitos) : '•••• •••• •••• ••••'}
                </span>
                <span className={estilos.pieTarjeta}>
                  <span>{tarjeta.titular.trim().toUpperCase() || 'NOMBRE DEL TITULAR'}</span>
                  <span>{tarjeta.vencimiento || 'MM/AA'}</span>
                </span>
              </div>

              <form className={formulario.formulario} onSubmit={alPagar} noValidate>
                {campo('titular', 'Nombre en la tarjeta', {
                  autoComplete: 'off',
                  onChange: (e) => cambiar('titular', e.target.value),
                })}
                {campo('numero', 'Número de la tarjeta', {
                  inputMode: 'numeric',
                  autoComplete: 'off',
                  placeholder: '0000 0000 0000 0000',
                  value: agrupar(digitos),
                  onChange: (e) => cambiar('numero', soloDigitos(e.target.value).slice(0, 19)),
                })}
                <div className={estilos.fila}>
                  {campo('vencimiento', 'Vencimiento', {
                    inputMode: 'numeric',
                    autoComplete: 'off',
                    placeholder: 'MM/AA',
                    onChange: (e) => cambiar('vencimiento', formatearVencimiento(e.target.value)),
                  })}
                  {campo('cvv', 'CVV', {
                    inputMode: 'numeric',
                    autoComplete: 'off',
                    type: 'password',
                    placeholder: marca === 'American Express' ? '0000' : '000',
                    onChange: (e) => cambiar('cvv', soloDigitos(e.target.value).slice(0, 4)),
                  })}
                </div>

                <button className={formulario.primario} type="submit" disabled={procesando}>
                  {procesando ? 'Procesando el pago…' : `Pagar ${precio(total)}`}
                </button>
              </form>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
