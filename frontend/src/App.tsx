import { Outlet, Route, Routes } from 'react-router-dom';
import { Cabecera } from './componentes/Cabecera';
import { RutaDeEmprendedor } from './componentes/RutaDeEmprendedor';
import { Pie } from './componentes/Pie';
import { Directorio } from './paginas/Directorio';
import { InformacionPersonal } from './paginas/InformacionPersonal';
import { Inicio } from './paginas/Inicio';
import { Login } from './paginas/Login';
import { MiNegocio } from './paginas/MiNegocio';
import { NoEncontrada } from './paginas/NoEncontrada';
import { PerfilNegocio } from './paginas/PerfilNegocio';
import { RecuperarContrasena } from './paginas/RecuperarContrasena';
import { RegistroCliente } from './paginas/RegistroCliente';
import { RegistroEmprendedor } from './paginas/RegistroEmprendedor';
import { RestablecerContrasena } from './paginas/RestablecerContrasena';
import { TratamientoDeDatos } from './paginas/TratamientoDeDatos';

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
        <Route path="/negocios/:id" element={<PerfilNegocio />} />
        <Route path="/entrar" element={<Login />} />
        <Route path="/registro" element={<RegistroCliente />} />
        <Route path="/recuperar" element={<RecuperarContrasena />} />
        <Route path="/recuperar/:token" element={<RestablecerContrasena />} />
        <Route path="/registro-emprendedor" element={<RegistroEmprendedor />} />
        <Route
          path="/mi-negocio"
          element={
            <RutaDeEmprendedor>
              <MiNegocio />
            </RutaDeEmprendedor>
          }
        />
        <Route path="/tratamiento-de-datos" element={<TratamientoDeDatos />} />
        <Route path="/informacion-personal" element={<InformacionPersonal />} />
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  );
}
