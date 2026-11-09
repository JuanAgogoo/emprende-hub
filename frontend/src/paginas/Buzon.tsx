import { BuzonDeConsultas } from '../componentes/BuzonDeConsultas';
import { IconoCarta } from '../componentes/IconoCarta';
import { useTitulo } from '../titulo';
import estilos from './Buzon.module.css';

/**
 * El buzón del emprendedor, con su propia entrada en la cabecera.
 *
 * Solo llegan aquí los mensajes de clientes que ya calificaron el negocio
 * (D4): el chat del perfil no deja escribir a nadie más.
 */
export function Buzon() {
  useTitulo('Buzón');

  return (
    <div className={`contenedor ${estilos.pagina}`}>
      <header className={estilos.cabecera}>
        <span className={estilos.sobre}>
          <IconoCarta tamano={40} />
        </span>
        <div>
          <h1>Mensajes de tus clientes</h1>
          <p className={estilos.entrada}>
            Los mensajes que te envían tus clientes desde tu perfil. Solo pueden escribirte quienes
            ya calificaron tu negocio. Respóndeles desde aquí: lo verán en su chat.
          </p>
        </div>
      </header>

      <BuzonDeConsultas />
    </div>
  );
}
