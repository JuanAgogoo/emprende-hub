import { useEffect, useState } from 'react';
import { listarUsuarios, reactivarUsuario, suspenderUsuario } from '../api/moderacion';
import { ConfirmarBorrado } from '../componentes/ConfirmarBorrado';
import { EsqueletoLista } from '../componentes/Esqueleto';
import { Paginacion } from '../componentes/Paginacion';
import { fecha } from '../formato';
import { casoImposible, type EstadoCarga } from '../types/estadoCarga';
import type { CuentaUsuario } from '../types/moderacion';
import type { Pagina } from '../types/pagina';
import type { Rol } from '../types/sesion';
import { useTitulo } from '../titulo';
import estilos from './Admin.module.css';

/** El rol como se dice, no como se guarda. */
function nombreDelRol(rol: Rol): string {
  switch (rol) {
    case 'CLIENTE':
      return 'Cliente';
    case 'EMPRENDEDOR':
      return 'Emprendedor';
    case 'ADMIN':
      return 'Administrador';
  }
}

/**
 * Las cuentas registradas y su estado, para suspenderlas o reactivarlas
 * (HU-038). Cada cambio llega a la persona por correo (I1-ter): suspendida no
 * puede entrar, así que no se enteraría de otra forma.
 */
export function AdminUsuarios() {
  useTitulo('Usuarios');
  const [pagina, setPagina] = useState(0);
  const [carga, setCarga] = useState<EstadoCarga<Pagina<CuentaUsuario>>>({ estado: 'CARGANDO' });
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [porSuspender, setPorSuspender] = useState<CuentaUsuario | null>(null);
  const [aviso, setAviso] = useState<string | null>(null);
  const [fallo, setFallo] = useState<string | null>(null);

  useEffect(() => {
    let vigente = true;
    setCarga({ estado: 'CARGANDO' });
    listarUsuarios(pagina)
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

  /** Cambia el estado de una cuenta en la lista, sin volver a pedirla entera. */
  function marcar(id: number, activo: boolean) {
    setCarga((actual) =>
      actual.estado === 'EXITO'
        ? {
            ...actual,
            datos: {
              ...actual.datos,
              content: actual.datos.content.map((c) => (c.id === id ? { ...c, activo } : c)),
            },
          }
        : actual,
    );
  }

  async function suspender() {
    if (porSuspender === null) return;
    const cuenta = porSuspender;
    setOcupado(cuenta.id);
    setFallo(null);
    setAviso(null);
    try {
      await suspenderUsuario(cuenta.id);
      marcar(cuenta.id, false);
      setAviso(`La cuenta de ${cuenta.nombre} está suspendida. Se le avisó por correo.`);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo suspender la cuenta');
    } finally {
      setOcupado(null);
      setPorSuspender(null);
    }
  }

  async function reactivar(cuenta: CuentaUsuario) {
    setOcupado(cuenta.id);
    setFallo(null);
    setAviso(null);
    try {
      await reactivarUsuario(cuenta.id);
      marcar(cuenta.id, true);
      setAviso(`La cuenta de ${cuenta.nombre} vuelve a estar activa. Se le avisó por correo.`);
    } catch (error: unknown) {
      setFallo(error instanceof Error ? error.message : 'No se pudo reactivar la cuenta');
    } finally {
      setOcupado(null);
    }
  }

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-usuarios">
      <h2 id="titulo-usuarios" className={estilos.tituloSeccion}>
        Usuarios
      </h2>

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

      <Cuentas
        carga={carga}
        ocupado={ocupado}
        alSuspender={setPorSuspender}
        alReactivar={reactivar}
        alPaginar={setPagina}
      />

      {porSuspender !== null && (
        <ConfirmarBorrado
          titulo={`¿Suspender la cuenta de ${porSuspender.nombre}?`}
          texto={
            porSuspender.rol === 'EMPRENDEDOR'
              ? 'No podrá entrar y su negocio dejará de verse en el directorio hasta que la reactives. Sus opiniones se quedan. Le llegará un correo avisándole.'
              : 'No podrá entrar hasta que la reactives. Sus opiniones se quedan. Le llegará un correo avisándole.'
          }
          ocupado={ocupado !== null}
          etiquetaConfirmar="Sí, suspender"
          etiquetaOcupado="Suspendiendo…"
          alConfirmar={suspender}
          alCancelar={() => setPorSuspender(null)}
        />
      )}
    </section>
  );
}

interface PropsCuentas {
  readonly carga: EstadoCarga<Pagina<CuentaUsuario>>;
  readonly ocupado: number | null;
  readonly alSuspender: (cuenta: CuentaUsuario) => void;
  readonly alReactivar: (cuenta: CuentaUsuario) => void;
  readonly alPaginar: (pagina: number) => void;
}

function Cuentas({ carga, ocupado, alSuspender, alReactivar, alPaginar }: PropsCuentas) {
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
          No se pudieron cargar las cuentas: {carga.mensaje}. Comprueba que la API esté
          funcionando y vuelve a intentarlo.
        </p>
      );

    case 'EXITO': {
      const { content: cuentas, totalElements: total } = carga.datos;

      return (
        <>
          <p className={estilos.recuento}>
            {total === 1 ? '1 cuenta registrada' : `${total} cuentas registradas`}
          </p>
          <ul className={estilos.lista}>
            {cuentas.map((cuenta) => (
              <li key={cuenta.id} className={estilos.fila}>
                <div className={estilos.filaCuerpo}>
                  <h3 className={estilos.nombre}>{cuenta.nombre}</h3>
                  <dl className={estilos.datos}>
                    <div>
                      <dt>Correo</dt>
                      <dd className={estilos.correo}>{cuenta.correo}</dd>
                    </div>
                    <div>
                      <dt>Rol</dt>
                      <dd>{nombreDelRol(cuenta.rol)}</dd>
                    </div>
                    <div>
                      <dt>Registro</dt>
                      <dd>
                        <time dateTime={cuenta.fechaRegistro}>{fecha(cuenta.fechaRegistro)}</time>
                      </dd>
                    </div>
                    <div>
                      <dt>Estado</dt>
                      <dd>
                        {/* La palabra y la señal dicen lo mismo que el color. */}
                        {cuenta.activo ? (
                          <span className={`${estilos.insignia} ${estilos.insigniaExito}`}>
                            <span aria-hidden="true">●</span> Activa
                          </span>
                        ) : (
                          <span className={`${estilos.insignia} ${estilos.insigniaError}`}>
                            <span aria-hidden="true">■</span> Suspendida
                          </span>
                        )}
                      </dd>
                    </div>
                  </dl>
                </div>

                {/* El administrador no se puede suspender a sí mismo: no se le
                    ofrece un botón que el backend rechazaría. */}
                {cuenta.rol !== 'ADMIN' &&
                  (cuenta.activo ? (
                    <button
                      type="button"
                      className={estilos.rechazar}
                      disabled={ocupado !== null}
                      onClick={() => alSuspender(cuenta)}
                      aria-label={`Suspender la cuenta de ${cuenta.nombre}`}
                    >
                      Suspender
                    </button>
                  ) : (
                    <button
                      type="button"
                      className={estilos.secundario}
                      disabled={ocupado !== null}
                      onClick={() => alReactivar(cuenta)}
                      aria-label={`Reactivar la cuenta de ${cuenta.nombre}`}
                    >
                      {ocupado === cuenta.id ? 'Reactivando…' : 'Reactivar'}
                    </button>
                  ))}
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
