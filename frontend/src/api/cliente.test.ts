import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarSesion } from '../almacenSesion';
import { json, primeraLlamada, SESION, simularFetch } from '../pruebas/respuestas';
import { consulta, enviar, ErrorApi, obtener, registrarCaducidadDeSesion } from './cliente';

/**
 * El cliente HTTP es el interceptor del proyecto: el único sitio que pone el
 * token en la cabecera y el único que lee los errores del backend.
 */
describe('api/cliente: el interceptor que adjunta el token', () => {
  const alCaducar = vi.fn();

  beforeEach(() => {
    localStorage.clear();
    alCaducar.mockReset();
    registrarCaducidadDeSesion(alCaducar);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('la cabecera Authorization', () => {
    it('la pone con el token guardado cuando la petición va con sesión', async () => {
      //arrange
      guardarSesion(SESION);
      const fetchSimulado = simularFetch(json({}));

      //act
      await obtener('/negocios/mio', true);

      //assert
      expect(primeraLlamada(fetchSimulado).cabeceras.Authorization).toBe(
        'Bearer token-de-prueba',
      );
    });

    it('no la pone si la petición va con sesión pero no hay ninguna guardada', async () => {
      //arrange
      const fetchSimulado = simularFetch(json({}));

      //act
      await obtener('/negocios/mio', true);

      //assert
      expect(primeraLlamada(fetchSimulado).cabeceras.Authorization).toBeUndefined();
    });

    it('no la pone en una petición pública, aunque haya sesión', async () => {
      //arrange
      guardarSesion(SESION);
      const fetchSimulado = simularFetch(json([]));

      //act
      await obtener('/directorio');

      //assert
      const { url, cabeceras } = primeraLlamada(fetchSimulado);
      expect(url).toBe('/api/v1/directorio');
      expect(cabeceras.Authorization).toBeUndefined();
    });
  });

  describe('el 401: sesión caducada', () => {
    it('con token, cierra la sesión llamando a la acción de caducidad', async () => {
      //arrange
      guardarSesion(SESION);
      simularFetch(json({ status: 401, message: 'Se requiere autenticación' }, 401));

      //act
      const intento = obtener('/negocios/mio', true);

      //assert
      await expect(intento).rejects.toMatchObject({ estado: 401 });
      //verify
      expect(alCaducar).toHaveBeenCalledOnce();
    });

    it('sin token no hay sesión que caducar', async () => {
      //arrange
      simularFetch(json({ status: 401, message: 'Credenciales incorrectas' }, 401));

      //act
      const intento = enviar('/auth/login', { correo: 'a@b.co', contrasena: 'x' });

      //assert
      await expect(intento).rejects.toMatchObject({ estado: 401 });
      //verify
      expect(alCaducar).not.toHaveBeenCalled();
    });

    it('un 403 con token no cierra la sesión: falta permiso, no autenticarse', async () => {
      //arrange
      guardarSesion(SESION);
      simularFetch(json({ status: 403, message: 'No tienes permiso para esta operación' }, 403));

      //act
      const intento = obtener('/admin/cursos', true);

      //assert
      await expect(intento).rejects.toMatchObject({ estado: 403 });
      //verify
      expect(alCaducar).not.toHaveBeenCalled();
    });
  });

  describe('los errores, ya legibles', () => {
    it('sin red, rechaza con estado 0 y un mensaje para personas', async () => {
      //arrange
      const sinRed = vi.fn<typeof fetch>().mockRejectedValue(new TypeError('Failed to fetch'));
      vi.stubGlobal('fetch', sinRed);

      //act
      const intento = obtener('/directorio');

      //assert
      await expect(intento).rejects.toMatchObject({
        estado: 0,
        message: 'No se pudo conectar con el servidor',
      });
    });

    it.each([502, 503, 504])('un %i de la pasarela es falta de conexión', async (estado) => {
      //arrange
      simularFetch(new Response('Bad Gateway', { status: estado }));

      //act
      const intento = obtener('/directorio');

      //assert
      await expect(intento).rejects.toMatchObject({
        estado,
        message: 'No se pudo conectar con el servidor',
      });
    });

    it('lee el mensaje y los errores por campo del GlobalExceptionHandler', async () => {
      //arrange
      simularFetch(
        json(
          {
            timestamp: '2026-10-29T12:00:00Z',
            status: 400,
            error: 'Bad Request',
            message: 'La petición no es válida',
            nombre: 'El nombre del producto es obligatorio',
            precio: 'El precio no puede ser negativo',
          },
          400,
        ),
      );

      //act
      const error = await obtener('/negocios/mio/productos', true).catch((e: unknown) => e);

      //assert
      expect(error).toBeInstanceOf(ErrorApi);
      const errorApi = error as ErrorApi;
      expect(errorApi.estado).toBe(400);
      expect(errorApi.message).toBe('La petición no es válida');
      expect(errorApi.porCampo).toEqual({
        nombre: 'El nombre del producto es obligatorio',
        precio: 'El precio no puede ser negativo',
      });
    });

    it('un error sin cuerpo se queda con un mensaje genérico con su código', async () => {
      //arrange
      simularFetch(new Response(null, { status: 500 }));

      //act
      const intento = obtener('/directorio');

      //assert
      await expect(intento).rejects.toMatchObject({
        estado: 500,
        message: 'Error 500 al llamar a la API',
      });
    });
  });

  it('consulta codifica los acentos y omite los filtros vacíos', () => {
    //act
    const cadena = consulta({ texto: 'café', ciudadId: 2, barrioId: undefined, orden: '' });

    //assert
    expect(cadena).toBe('?texto=caf%C3%A9&ciudadId=2');
  });
});
