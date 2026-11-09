import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { guardarSesion } from '../almacenSesion';
import { json, primeraLlamada, SESION, simularFetch } from '../pruebas/respuestas';
import type { Producto } from '../types/negocio';
import { borrarProducto, crearProducto, editarProducto, obtenerMisProductos } from './negocios';

const PRODUCTO: Producto = {
  id: 7,
  nombre: 'Pan de masa madre',
  precio: 12000,
  descripcion: 'Fermentación lenta',
  disponible: true,
  foto: 'pan.png',
};

/** El CRUD del escaparate: el emprendedor crea, lista, edita y borra sus productos. */
describe('api/negocios: el escaparate del emprendedor', () => {
  beforeEach(() => {
    localStorage.clear();
    guardarSesion(SESION);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('obtenerMisProductos lista con GET y con el token', async () => {
    //arrange
    const fetchSimulado = simularFetch(json([PRODUCTO]));

    //act
    const productos = await obtenerMisProductos();

    //assert
    const { url, opciones, cabeceras } = primeraLlamada(fetchSimulado);
    expect(url).toBe('/api/v1/negocios/mio/productos');
    expect(opciones.method).toBe('GET');
    expect(cabeceras.Authorization).toBe('Bearer token-de-prueba');
    expect(productos).toEqual([PRODUCTO]);
  });

  it('crearProducto sube un multipart con los campos y la foto', async () => {
    //arrange
    const fetchSimulado = simularFetch(json(PRODUCTO, 201));
    const foto = new File(['imagen'], 'pan.png', { type: 'image/png' });

    //act
    const creado = await crearProducto(
      { nombre: 'Pan de masa madre', precio: 12000, disponible: true },
      foto,
    );

    //assert
    const { url, opciones, cabeceras } = primeraLlamada(fetchSimulado);
    expect(url).toBe('/api/v1/negocios/mio/productos');
    expect(opciones.method).toBe('POST');
    expect(cabeceras.Authorization).toBe('Bearer token-de-prueba');
    // El Content-Type del multipart lo pone el navegador, con su boundary.
    expect(cabeceras['Content-Type']).toBeUndefined();
    const cuerpo = opciones.body as FormData;
    expect(cuerpo.get('nombre')).toBe('Pan de masa madre');
    expect(cuerpo.get('precio')).toBe('12000');
    expect(cuerpo.get('disponible')).toBe('true');
    expect(cuerpo.has('descripcion')).toBe(false);
    expect((cuerpo.get('foto') as File).name).toBe('pan.png');
    expect(creado).toEqual(PRODUCTO);
  });

  it('editarProducto sustituye con PUT y el cuerpo en JSON', async () => {
    //arrange
    const fetchSimulado = simularFetch(json({ ...PRODUCTO, precio: 13000 }));
    const datos = { nombre: 'Pan de masa madre', precio: 13000, disponible: false };

    //act
    const editado = await editarProducto(7, datos);

    //assert
    const { url, opciones, cabeceras } = primeraLlamada(fetchSimulado);
    expect(url).toBe('/api/v1/negocios/mio/productos/7');
    expect(opciones.method).toBe('PUT');
    expect(cabeceras.Authorization).toBe('Bearer token-de-prueba');
    expect(cabeceras['Content-Type']).toBe('application/json');
    expect(JSON.parse(String(opciones.body))).toEqual(datos);
    expect(editado.precio).toBe(13000);
  });

  it('borrarProducto manda DELETE y acepta el 204 sin cuerpo', async () => {
    //arrange
    const fetchSimulado = simularFetch(new Response(null, { status: 204 }));

    //act
    const resultado = borrarProducto(7);

    //assert
    await expect(resultado).resolves.toBeUndefined();
    const { url, opciones, cabeceras } = primeraLlamada(fetchSimulado);
    expect(url).toBe('/api/v1/negocios/mio/productos/7');
    expect(opciones.method).toBe('DELETE');
    expect(cabeceras.Authorization).toBe('Bearer token-de-prueba');
  });

  it('un error de validación al crear llega con su mensaje por campo', async () => {
    //arrange
    simularFetch(
      json({ status: 400, message: 'No es válida', precio: 'El precio es obligatorio' }, 400),
    );
    const foto = new File(['imagen'], 'pan.png', { type: 'image/png' });

    //act
    const intento = crearProducto({ nombre: 'Pan', precio: Number.NaN, disponible: true }, foto);

    //assert
    await expect(intento).rejects.toMatchObject({
      estado: 400,
      porCampo: { precio: 'El precio es obligatorio' },
    });
  });
});
