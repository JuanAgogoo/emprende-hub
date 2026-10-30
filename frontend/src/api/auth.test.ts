import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarSesion } from '../almacenSesion';
import { json, primeraLlamada, SESION, simularFetch } from '../pruebas/respuestas';
import { iniciarSesion, registrarCliente, solicitarRecuperacion } from './auth';
import { ErrorApi, registrarCaducidadDeSesion } from './cliente';

describe('api/auth: el servicio de autenticación', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('iniciarSesion manda el correo y la contraseña por POST y devuelve la sesión', async () => {
    //arrange
    const respuesta = { ...SESION, tipo: 'Bearer', expiraEnMillis: 3600000 };
    const fetchSimulado = simularFetch(json(respuesta));

    //act
    const sesion = await iniciarSesion('lucia@emprendehub.co', 'emprende12345');

    //assert
    const { url, opciones, cabeceras } = primeraLlamada(fetchSimulado);
    expect(url).toBe('/api/v1/auth/login');
    expect(opciones.method).toBe('POST');
    expect(cabeceras['Content-Type']).toBe('application/json');
    expect(JSON.parse(String(opciones.body))).toEqual({
      correo: 'lucia@emprendehub.co',
      contrasena: 'emprende12345',
    });
    expect(sesion).toEqual(respuesta);
  });

  it('el login nunca lleva token, aunque haya una sesión guardada', async () => {
    //arrange
    guardarSesion(SESION);
    const fetchSimulado = simularFetch(json({ ...SESION, tipo: 'Bearer', expiraEnMillis: 1 }));

    //act
    await iniciarSesion('lucia@emprendehub.co', 'emprende12345');

    //assert
    expect(primeraLlamada(fetchSimulado).cabeceras.Authorization).toBeUndefined();
  });

  it('con credenciales malas rechaza con el 401 y no da la sesión por caducada', async () => {
    //arrange
    const alCaducar = vi.fn();
    registrarCaducidadDeSesion(alCaducar);
    simularFetch(json({ status: 401, message: 'Credenciales incorrectas' }, 401));

    //act
    const intento = iniciarSesion('lucia@emprendehub.co', 'mala');

    //assert
    await expect(intento).rejects.toBeInstanceOf(ErrorApi);
    await expect(intento).rejects.toMatchObject({
      estado: 401,
      message: 'Credenciales incorrectas',
    });
    //verify
    expect(alCaducar).not.toHaveBeenCalled();
  });

  it('registrarCliente da de alta por POST /auth/registro', async () => {
    //arrange
    const fetchSimulado = simularFetch(json({ ...SESION, rol: 'CLIENTE' }, 201));

    //act
    await registrarCliente('María', 'maria@gmail.com', 'contrasena123');

    //assert
    const { url, opciones } = primeraLlamada(fetchSimulado);
    expect(url).toBe('/api/v1/auth/registro');
    expect(opciones.method).toBe('POST');
    expect(JSON.parse(String(opciones.body))).toEqual({
      nombre: 'María',
      correo: 'maria@gmail.com',
      contrasena: 'contrasena123',
    });
  });

  it('solicitarRecuperacion acepta un 200 sin cuerpo sin romper al leerlo', async () => {
    //arrange
    simularFetch(new Response(null, { status: 200 }));

    //act
    const resultado = solicitarRecuperacion('lucia@emprendehub.co');

    //assert
    await expect(resultado).resolves.toBeUndefined();
  });
});
