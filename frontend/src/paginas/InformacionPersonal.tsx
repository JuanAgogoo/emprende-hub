import { Link } from 'react-router-dom';
import estilos from './PaginaTexto.module.css';
import { useTitulo } from '../titulo';

/**
 * Qué se ve de ti y qué no, según con qué cuenta entres.
 *
 * Complementa al [tratamiento de datos]: aquella página dice qué se guarda,
 * esta dice quién lo ve y qué control tienes sobre ello.
 */
export function InformacionPersonal() {
  useTitulo('Información personal');
  return (
    <article className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.encabezado}>
        <h1>Información personal</h1>
        <p className={estilos.entradilla}>
          Qué se ve de ti en EmprendeHub y qué queda entre tú y la plataforma.
        </p>
        <p className={estilos.actualizado}>Proyecto académico · Universidad Pontificia Bolivariana</p>
      </header>

      <section className={estilos.seccion}>
        <h2>Público y privado</h2>
        <div className={estilos.envoltorioTabla}>
          <table className={estilos.tabla}>
            <thead>
              <tr>
                <th scope="col">Dato</th>
                <th scope="col">Quién lo ve</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Tu nombre</td>
                <td>Cualquiera, junto a las opiniones que escribas</td>
              </tr>
              <tr>
                <td>Tu correo</td>
                <td>Solo tú, y el dueño del negocio al que escribas</td>
              </tr>
              <tr>
                <td>Tu contraseña</td>
                <td>Nadie. Se guarda cifrada y no se puede recuperar</td>
              </tr>
              <tr>
                <td>Los datos de tu negocio</td>
                <td>Cualquiera, una vez aprobado</td>
              </tr>
              <tr>
                <td>Las visitas a tu negocio</td>
                <td>Solo tú, en tu panel, y como una cifra sin nombres</td>
              </tr>
              <tr>
                <td>El motivo de un rechazo</td>
                <td>Solo tú y quien administra. Nunca se muestra en público</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section className={estilos.seccion}>
        <h2>Tu cuenta según lo que hagas</h2>
        <ul className={estilos.lista}>
          <li>
            <strong>Si vienes a mirar</strong>, no necesitas cuenta: el directorio y las fichas de
            los negocios se ven sin registrarse.
          </li>
          <li>
            <strong>Si quieres opinar o escribir a un negocio</strong>, necesitas una cuenta con tu
            nombre y tu correo.
          </li>
          <li>
            <strong>Si registras un negocio</strong>, tu misma cuenta pasa a ser de emprendedor. No
            se abre una segunda y no pierdes lo que ya habías escrito.
          </li>
        </ul>
      </section>

      <section className={estilos.seccion}>
        <h2>Tu contraseña</h2>
        <p>
          Se guarda cifrada, así que ni quien administra la plataforma puede leerla. Pide al menos
          ocho caracteres.
        </p>
        <p className={estilos.aviso}>
          No hay forma automática de recuperarla: EmprendeHub no envía correos. Si la olvidas,
          tendrás que pedir ayuda a quien administra la plataforma.
        </p>
      </section>

      <section className={estilos.seccion}>
        <h2>Lo que se publica pasa por revisión</h2>
        <p>
          Un negocio no aparece en el directorio hasta que alguien lo revisa, y lo mismo ocurre con
          las fotos y con los cambios de nombre o descripción. Si se rechaza algo, verás el motivo
          y podrás corregirlo.
        </p>
        <p>
          Las opiniones se publican al momento, pero cualquiera puede denunciarlas y quien
          administra decide si se quedan.
        </p>
        <p className={estilos.destacado}>
          Para saber qué datos se guardan y para qué, mira{' '}
          <Link to="/tratamiento-de-datos">Tratamiento de datos</Link>.
        </p>
      </section>
    </article>
  );
}
