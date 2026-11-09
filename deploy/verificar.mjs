#!/usr/bin/env node
// Prueba de punta a punta contra un sitio desplegado. Sin dependencias.
//
//   BASE=https://emprendehub.tech CREDENCIALES=~/emprendehub-credenciales.txt node deploy/verificar.mjs
//
// El fichero de credenciales lleva líneas CLAVE=valor; se usan ADMIN_CORREO y
// ADMIN_CONTRASENA y nunca se imprimen. Crea un cliente y un emprendedor de
// prueba con sufijo aleatorio y al terminar los suspende, así su negocio
// desaparece del directorio sin borrar nada.
import fs from 'node:fs';
import os from 'node:os';
import { randomBytes } from 'node:crypto';

const BASE = (process.env.BASE ?? 'https://emprendehub.tech').replace(/\/$/, '');
const API = `${BASE}/api/v1`;
const ruta = (process.env.CREDENCIALES ?? '~/emprendehub-credenciales.txt').replace(/^~/, os.homedir());
const cred = Object.fromEntries(
  fs.readFileSync(ruta, 'utf8').split('\n').map((l) => l.match(/^([A-Z_]+)=(.*)$/)).filter(Boolean).map((m) => [m[1], m[2]]),
);

let bien = 0, mal = 0;
const ok = (desc, cond, extra = '') => {
  cond ? bien++ : mal++;
  console.log(`  ${cond ? 'OK   ' : 'FALLA'} ${desc}${cond ? '' : `  ${extra}`}`);
};
const seccion = (t) => console.log(`\n${t}`);

async function pedir(metodo, camino, { token, json, form } = {}) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  let body;
  if (json !== undefined) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(json); }
  if (form) body = form;
  const inicio = Date.now();
  const r = await fetch(`${API}${camino}`, { method: metodo, headers, body, redirect: 'manual' });
  const texto = await r.text();
  let datos = null;
  try { datos = JSON.parse(texto); } catch { /* no era JSON */ }
  return { status: r.status, datos, ms: Date.now() - inicio };
}
const login = async (correo, contrasena) => (await pedir('POST', '/auth/login', { json: { correo, contrasena } })).datos?.token ?? null;

// Un PNG real de 1×1: lo mínimo que acepta la subida de fotos.
const PNG = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==', 'base64');
const conFoto = (campo, extra = {}) => {
  const f = new FormData();
  for (const [k, v] of Object.entries(extra)) f.append(k, v);
  f.append(campo, new Blob([PNG], { type: 'image/png' }), 'prueba.png');
  return f;
};

const sufijo = randomBytes(4).toString('hex');
const clave = randomBytes(12).toString('hex');
const correoCli = `verifica.cliente.${sufijo}@ejemplo.test`;
const correoEmp = `verifica.emprende.${sufijo}@ejemplo.test`;

seccion('1 · Sitio público');
const inicio = await fetch(`${BASE}/`);
ok('la portada responde 200', inicio.status === 200, inicio.status);
const spa = await fetch(`${BASE}/directorio`);
ok('una ruta de React Router responde 200', spa.status === 200, spa.status);
if (BASE.startsWith('https://')) {
  const http = await fetch(BASE.replace('https://', 'http://'), { redirect: 'manual' });
  ok('http redirige a https', [301, 302, 308].includes(http.status) && (http.headers.get('location') ?? '').startsWith('https://'), http.status);
}
const dir = await pedir('GET', '/directorio?size=50');
ok('el directorio tiene negocios', dir.status === 200 && dir.datos.totalElements > 0, dir.status);
const conFotoPrincipal = dir.datos?.content?.find((n) => n.fotoPrincipal);
if (conFotoPrincipal) {
  const f = await fetch(`${BASE}${conFotoPrincipal.fotoPrincipal}`);
  ok('las fotos cargan por /fotos', f.status === 200 && (f.headers.get('content-type') ?? '').startsWith('image/'), f.headers.get('content-type'));
}
const cursos = await pedir('GET', '/cursos');
ok('el catálogo de cursos carga', cursos.status === 200 && cursos.datos.totalElements > 0, cursos.status);

seccion('2 · Credenciales públicas del repositorio');
ok('admin@emprendehub.co / admin12345 ya NO entra', (await login('admin@emprendehub.co', 'admin12345')) === null);
ok('las cuentas de demo no entran con la contraseña pública', (await login('cliente@emprendehub.co', 'cliente12345')) === null);
const admin = await login(cred.ADMIN_CORREO, cred.ADMIN_CONTRASENA);
ok('el administrador nuevo entra', admin !== null);

seccion('3 · Emprendedor');
const cats = (await pedir('GET', '/catalogos/categorias-negocio')).datos;
const ciudades = (await pedir('GET', '/catalogos/ciudades')).datos;
const alta = await pedir('POST', '/auth/registro-emprendedor', { json: {
  nombre: 'Verificación Emprendedora', correo: correoEmp, contrasena: clave,
  negocio: { nombre: `Negocio Verificación ${sufijo}`, descripcion: 'Negocio creado por la verificación automática del despliegue; se oculta al terminar la prueba.', telefono: '3000000000', categoriaId: cats[0].id, ciudadId: ciudades[0].id, nivelPrecio: 'MEDIO' },
} });
ok('registro de emprendedor con su negocio', alta.status === 201, `${alta.status} ${JSON.stringify(alta.datos)}`);
const emp = await login(correoEmp, clave);
ok('login del emprendedor', emp !== null);
const foto = await pedir('POST', '/negocios/mio/fotos', { token: emp, form: conFoto('archivo') });
ok('subir una foto', foto.status === 201, foto.status);
const producto = await pedir('POST', '/negocios/mio/productos', { token: emp, form: conFoto('foto', { nombre: 'Producto de verificación', precio: '1000', disponible: 'true' }) });
ok('añadir un producto', producto.status === 201, producto.status);
const mio = (await pedir('GET', '/negocios/mio', { token: emp })).datos;
const perfil = await pedir('GET', `/directorio/${mio?.id}`);
if (perfil.status === 200) {
  const url = perfil.datos.fotos?.[0]?.url;
  const f = url ? await fetch(`${BASE}${url}`) : { status: 0 };
  ok('el negocio sale publicado y su foto se ve', f.status === 200, f.status);
} else {
  ok('con moderación manual el negocio queda pendiente (404)', perfil.status === 404, perfil.status);
}

seccion('4 · Cliente, calificación y buzón');
const reg = await pedir('POST', '/auth/registro', { json: { nombre: 'Verificación Cliente', correo: correoCli, contrasena: clave } });
ok('registro de cliente', reg.status === 201, reg.status);
const cli = await login(correoCli, clave);
if (perfil.status === 404 && admin) await pedir('PATCH', `/admin/moderacion/negocios/${mio.id}/aprobar`, { token: admin });
const sinCalificar = await pedir('POST', `/negocios/${mio.id}/consultas`, { token: cli, json: { asunto: 'Hola', mensaje: 'Prueba' } });
ok('sin calificar no se puede escribir', sinCalificar.status === 400, sinCalificar.status);
const op = await pedir('POST', `/negocios/${mio.id}/opiniones`, { token: cli, json: { calificacion: 5, comentario: 'Verificación' } });
ok('calificar el negocio', op.status === 201, op.status);
const con = await pedir('POST', `/negocios/${mio.id}/consultas`, { token: cli, json: { asunto: 'Verificación', mensaje: '¿Abren el domingo?' } });
ok('después de calificar sí se puede escribir', con.status === 201, con.status);
const buzon = await pedir('GET', '/negocios/mio/consultas', { token: emp });
ok('el mensaje llega al buzón', buzon.datos?.content?.some((c) => c.id === con.datos?.id));
const resp = await pedir('PATCH', `/negocios/mio/consultas/${con.datos?.id}/respuesta`, { token: emp, json: { respuesta: 'Sí abrimos.' } });
ok('el emprendedor responde', resp.status === 200, resp.status);
const hilo = await pedir('GET', `/negocios/${mio.id}/consultas/mias`, { token: cli });
ok('el cliente ve la respuesta', hilo.datos?.[0]?.respuesta === 'Sí abrimos.');

seccion('5 · Correo sin proveedor: la acción termina y no da 500');
const rec = await pedir('POST', '/auth/recuperacion', { json: { correo: correoCli } });
ok(`«olvidé mi contraseña» responde 200 (${rec.ms} ms)`, rec.status === 200, rec.status);
ok('y no se queda colgado (menos de 10 s)', rec.ms < 10000, rec.ms);

seccion('6 · Administrador');
const usuarios = await pedir('GET', '/admin/moderacion/usuarios?size=100&sort=id,desc', { token: admin });
ok('panel de usuarios', usuarios.status === 200, usuarios.status);
const den = await pedir('GET', '/admin/moderacion/denuncias', { token: admin });
ok('panel de denuncias', den.status === 200, den.status);
ok('un cliente no entra al panel (403)', (await pedir('GET', '/admin/moderacion/usuarios', { token: cli })).status === 403);
ok('sin sesión tampoco (401)', (await pedir('GET', '/admin/moderacion/usuarios')).status === 401);

async function idDe(correo) {
  for (let page = 0; page < 20; page++) {
    const r = await pedir('GET', `/admin/moderacion/usuarios?size=100&page=${page}`, { token: admin });
    const u = r.datos?.content?.find((x) => x.correo === correo);
    if (u || r.datos?.last) return u?.id;
  }
  return undefined;
}
const idCli = await idDe(correoCli), idEmp = await idDe(correoEmp);
const sus = await pedir('PATCH', `/admin/moderacion/usuarios/${idCli}/suspender`, { token: admin });
ok(`suspender no da 500 sin correo (${sus.ms} ms)`, sus.status === 204, sus.status);
ok('la cuenta suspendida no entra', (await login(correoCli, clave)) === null);
const rea = await pedir('PATCH', `/admin/moderacion/usuarios/${idCli}/reactivar`, { token: admin });
ok('reactivar no da 500 sin correo', rea.status === 204, rea.status);

seccion('Limpieza');
for (const id of [idCli, idEmp]) await pedir('PATCH', `/admin/moderacion/usuarios/${id}/suspender`, { token: admin });
const visible = await pedir('GET', `/directorio/${mio?.id}`);
ok('el negocio de prueba ya no se ve en el directorio', visible.status === 404, visible.status);

console.log(`\nResultado: ${bien} correctas, ${mal} fallidas`);
process.exit(mal ? 1 : 0);
