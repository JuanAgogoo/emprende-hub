import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { Cabecera } from './componentes/Cabecera';
import { RutaDeAdmin } from './componentes/RutaDeAdmin';
import { RutaDeEmprendedor } from './componentes/RutaDeEmprendedor';
import { Pie } from './componentes/Pie';
import { AdminCambios } from './paginas/AdminCambios';
import { AdminCursos } from './paginas/AdminCursos';
import { AdminDenuncias } from './paginas/AdminDenuncias';
import { AdminFormularioCurso } from './paginas/AdminFormularioCurso';
import { AdminHistorial } from './paginas/AdminHistorial';
import { AdminNegocios } from './paginas/AdminNegocios';
import { AdminRevisarNegocio } from './paginas/AdminRevisarNegocio';
import { AdminUsuarios } from './paginas/AdminUsuarios';
import { Cursos } from './paginas/Cursos';
import { Directorio } from './paginas/Directorio';
import { InformacionPersonal } from './paginas/InformacionPersonal';
import { Inicio } from './paginas/Inicio';
import { Login } from './paginas/Login';
import { MiNegocio } from './paginas/MiNegocio';
import { NoEncontrada } from './paginas/NoEncontrada';
import { PanelAdmin } from './paginas/PanelAdmin';
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
        <Route path="/cursos" element={<Cursos />} />
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
        <Route
          path="/admin"
          element={
            <RutaDeAdmin>
              <PanelAdmin />
            </RutaDeAdmin>
          }
        >
          {/* `/admin` a secas abre la primera cola, la de negocios. */}
          <Route index element={<Navigate to="/admin/negocios" replace />} />
          <Route path="negocios" element={<AdminNegocios />} />
          <Route path="negocios/:id" element={<AdminRevisarNegocio />} />
          <Route path="cambios" element={<AdminCambios />} />
          <Route path="denuncias" element={<AdminDenuncias />} />
          <Route path="usuarios" element={<AdminUsuarios />} />
          <Route path="cursos" element={<AdminCursos />} />
          <Route path="cursos/nuevo" element={<AdminFormularioCurso />} />
          <Route path="cursos/:id" element={<AdminFormularioCurso />} />
          <Route path="historial" element={<AdminHistorial />} />
        </Route>
        <Route path="/tratamiento-de-datos" element={<TratamientoDeDatos />} />
        <Route path="/informacion-personal" element={<InformacionPersonal />} />
        <Route path="*" element={<NoEncontrada />} />
      </Route>
    </Routes>
  );
}
