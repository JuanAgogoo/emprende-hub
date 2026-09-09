import { Link } from 'react-router-dom';
import estilos from './PaginaTexto.module.css';

/**
 * El aviso que se acepta al crear una cuenta.
 *
 * Describe lo que la aplicación hace de verdad, no un texto de relleno: cada
 * dato de la tabla corresponde a un campo que el backend guarda.
 */
export function TratamientoDeDatos() {
  return (
    <article className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.encabezado}>
        <h1>Tratamiento de datos</h1>
        <p className={estilos.entradilla}>
          Qué datos guarda EmprendeHub, para qué los usa y quién puede verlos.
        </p>
        <p className={estilos.actualizado}>Proyecto académico · Universidad Pontificia Bolivariana</p>
      </header>

      <p className={estilos.aviso}>
        EmprendeHub es un trabajo de la asignatura Plataformas de Programación Empresarial. No es
        un servicio comercial y este texto no sustituye a un aviso legal.
      </p>

      <section className={estilos.seccion}>
        <h2>Qué se guarda</h2>
        <div className={estilos.envoltorioTabla}>
          <table className={estilos.tabla}>
            <thead>
              <tr>
                <th scope="col">Dato</th>
                <th scope="col">Cuándo se recoge</th>
                <th scope="col">Para qué</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Nombre y correo</td>
                <td>Al crear la cuenta</td>
                <td>Identificar quién entra y firmar sus opiniones</td>
              </tr>
              <tr>
                <td>Contraseña</td>
                <td>Al crear la cuenta</td>
                <td>Entrar. Se guarda cifrada y nadie puede leerla, tampoco quien administra</td>
              </tr>
              <tr>
                <td>Datos del negocio</td>
                <td>Al registrar un negocio</td>
                <td>Publicarlo en el directorio: nombre, descripción, teléfono y ubicación</td>
              </tr>
              <tr>
                <td>Fotos y productos</td>
                <td>Al montar el perfil</td>
                <td>Enseñar el negocio a quien lo visita</td>
              </tr>
              <tr>
                <td>Opiniones</td>
                <td>Al calificar un negocio</td>
                <td>Calcular su nota y ayudar a quien busca después</td>
              </tr>
              <tr>
                <td>Consultas</td>
                <td>Al escribir a un negocio</td>
                <td>Que su dueño pueda responderte</td>
              </tr>
              <tr>
                <td>Visitas a un perfil</td>
                <td>Al abrir un negocio</td>
                <td>Contar cuánta gente lo ve. Se cuenta una vez por día, sin identificar a nadie</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section className={estilos.seccion}>
        <h2>Quién ve cada cosa</h2>
        <ul className={estilos.lista}>
          <li>
            <strong>Tu correo no aparece en el directorio.</strong> La única excepción es cuando
            escribes a un negocio: su dueño lo ve para poder responderte.
          </li>
          <li>
            <strong>El teléfono de un negocio sí es público.</strong> Es la forma de que le
            contacten, y por eso se pide al registrarlo.
          </li>
          <li>
            <strong>Tu nombre acompaña a tus opiniones.</strong> Quien lea la ficha de un negocio
            verá quién opinó.
          </li>
          <li>
            Quien administra la plataforma revisa los negocios antes de publicarlos y atiende las
            denuncias sobre opiniones.
          </li>
        </ul>
      </section>

      <section className={estilos.seccion}>
        <h2>Qué no se hace</h2>
        <ul className={estilos.lista}>
          <li>No se envía publicidad ni correos de ningún tipo.</li>
          <li>No se comparten datos con terceros ni hay servicios de analítica externos.</li>
          <li>No se usan cookies de seguimiento. La sesión se guarda en tu propio navegador.</li>
          <li>No se pide ningún dato de pago: EmprendeHub no cobra nada.</li>
        </ul>
      </section>

      <section className={estilos.seccion}>
        <h2>Qué puedes hacer con tus datos</h2>
        <ul className={estilos.lista}>
          <li>Editar tu opinión sobre un negocio, o borrarla.</li>
          <li>Cambiar los datos de tu negocio, sus fotos y sus productos.</li>
          <li>Pedir a quien administra que dé de baja tu cuenta.</li>
        </ul>
        <p className={estilos.destacado}>
          Al crear una cuenta aceptas lo que dice esta página. Puedes leer también qué es público y
          qué no en <Link to="/informacion-personal">Información personal</Link>.
        </p>
      </section>
    </article>
  );
}
