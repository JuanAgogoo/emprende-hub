import { Outlet, Route, Routes } from 'react-router-dom';
import { Cabecera } from './componentes/Cabecera';
import { Pie } from './componentes/Pie';
import { Directorio } from './paginas/Directorio';
import { Inicio } from './paginas/Inicio';
import { NoEncontrada } from './paginas/NoEncontrada';

/** La estructura que comparten todas las rutas. */
function Estructura() {
  return (
    <>
      <a className="saltar-al-contenido" href="#contenido">
        Saltar al contenido
      </a>
      <Cabecera />
      <main id="contenido">
        <Outlet />
      </main>
      <Pie />
    </>
  );
}

export function App() {
  return (
    <Routes>
      <Route element={<Estructura />}>
        <Route path="/" element={<Inicio />} />
        <Route path="/directorio" element={<Directorio />} />
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  );
}
