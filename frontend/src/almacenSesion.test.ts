import { beforeEach, describe, expect, it } from 'vitest';
import { borrarSesion, guardarSesion, leerSesion, leerToken } from './almacenSesion';
import { SESION } from './pruebas/respuestas';

const CLAVE = 'emprendehub.sesion';

describe('almacenSesion: dónde vive la sesión entre recargas', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('guarda la sesión y la vuelve a leer igual', () => {
    //act
    guardarSesion(SESION);

    //assert
    expect(leerSesion()).toEqual(SESION);
    expect(leerToken()).toBe('token-de-prueba');
  });

  it('sin nada guardado no hay sesión ni token', () => {
    //act y assert
    expect(leerSesion()).toBeNull();
    expect(leerToken()).toBeNull();
  });

  it('descarta y borra lo guardado si no es JSON', () => {
    //arrange
    localStorage.setItem(CLAVE, '{esto no es json');

    //act
    const sesion = leerSesion();

    //assert
    expect(sesion).toBeNull();
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });

  it('descarta y borra una sesión con otra forma, como un rol que no existe', () => {
    //arrange
    localStorage.setItem(CLAVE, JSON.stringify({ ...SESION, rol: 'SUPERUSUARIO' }));

    //act
    const sesion = leerSesion();

    //assert
    expect(sesion).toBeNull();
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });

  it('descarta una sesión a la que le falta el token', () => {
    //arrange
    const { nombre, correo, rol } = SESION;
    localStorage.setItem(CLAVE, JSON.stringify({ nombre, correo, rol }));

    //act y assert
    expect(leerToken()).toBeNull();
  });

  it('borrarSesion la quita del almacén', () => {
    //arrange
    guardarSesion(SESION);

    //act
    borrarSesion();

    //assert
    expect(leerSesion()).toBeNull();
    expect(localStorage.getItem(CLAVE)).toBeNull();
  });
});
