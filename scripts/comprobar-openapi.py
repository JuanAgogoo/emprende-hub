#!/usr/bin/env python3
"""
Contrasta openapi.json con la API en marcha, operación por operación.

    python3 scripts/comprobar-openapi.py                 # contra http://localhost:8080
    API=http://localhost:8081 python3 scripts/comprobar-openapi.py

Para cada ruta y método del fichero hace dos peticiones, las dos inofensivas:
identificadores que no existen (999999), un enlace de recuperación inventado y
cuerpos vacíos, así que nada se crea, se aprueba ni se borra.

Comprueba tres cosas:

1. **Que la operación existe**: con un token de administrador no responde 405,
   ni 404 por ruta inexistente. Un 404 de recurso —«No se encontró Negocio con
   identificador 999999»— sí vale: lo distingue que trae `message`, mientras
   que el de ruta inexistente lo construye Spring y trae `path`.
2. **Que el candado es el declarado**: sin token, las operaciones protegidas
   responden 401 y las públicas no. Hace falta el token en la primera
   comprobación porque, sin él, una ruta inexistente también responde 401.
3. **Que el código obtenido está documentado** en esa operación.

Necesita la API levantada y solo la biblioteca estándar de Python. Termina con
error si algo falla.
"""
import json
import os
import re
import sys
import urllib.error
import urllib.request

API = os.environ.get('API', 'http://localhost:8080')
CORREO = os.environ.get('ADMIN_CORREO', 'admin@emprendehub.co')
CONTRASENA = os.environ.get('ADMIN_CONTRASENA', 'admin12345')
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')


def pedir(metodo, ruta, token=None, cuerpo=None, tipo=None):
    """Devuelve el código y el cuerpo ya leído como JSON, o None si no lo es."""
    cabeceras = {}
    if token:
        cabeceras['Authorization'] = 'Bearer ' + token
    if tipo:
        cabeceras['Content-Type'] = tipo
    peticion = urllib.request.Request(API + ruta, data=cuerpo, method=metodo, headers=cabeceras)
    try:
        with urllib.request.urlopen(peticion) as respuesta:
            codigo, texto = respuesta.status, respuesta.read()
    except urllib.error.HTTPError as error:
        codigo, texto = error.code, error.read()
    try:
        return codigo, json.loads(texto) if texto else None
    except ValueError:
        return codigo, None


def cuerpo_vacio(operacion):
    """Un cuerpo vacío del tipo que espera la operación, o nada si no lleva."""
    contenido = (operacion.get('requestBody') or {}).get('content', {})
    if 'multipart/form-data' in contenido:
        return b'--vacio--\r\n', 'multipart/form-data; boundary=vacio'
    if 'application/json' in contenido:
        return b'{}', 'application/json'
    return None, None


def main():
    with open(os.path.join(RAIZ, 'openapi.json'), encoding='utf-8') as fichero:
        documento = json.load(fichero)

    codigo, sesion = pedir('POST', '/api/v1/auth/login', cuerpo=json.dumps(
        {'correo': CORREO, 'contrasena': CONTRASENA}).encode(), tipo='application/json')
    if codigo != 200 or not sesion:
        sys.exit(f'No se pudo entrar como administrador ({codigo}). ¿Está la API levantada?')
    token = sesion['token']

    fallos, avisos, total = [], [], 0
    for plantilla, item in documento['paths'].items():
        ruta = re.sub(r'\{token\}', 'enlace-inventado', plantilla)
        ruta = re.sub(r'\{[^}]+\}', '999999', ruta)
        for metodo, operacion in item.items():
            total += 1
            nombre = f'{metodo.upper():6} {plantilla}'
            documentados = set(operacion['responses'])
            protegida = bool(operacion.get('security'))
            cuerpo, tipo = cuerpo_vacio(operacion)

            con, respuesta = pedir(metodo.upper(), ruta, token, cuerpo, tipo)
            ruta_inexistente = con == 404 and not (respuesta or {}).get('message')
            if con == 405 or ruta_inexistente:
                fallos.append(f'{nombre}: {con} {"por ruta inexistente" if con == 404 else ""}')
            elif str(con) not in documentados:
                avisos.append(f'{nombre}: respondió {con}, que no está documentado')

            sin, _ = pedir(metodo.upper(), ruta, None, cuerpo, tipo)
            if protegida and sin != 401:
                fallos.append(f'{nombre}: declarada protegida y sin token respondió {sin}')
            if not protegida and sin == 401:
                fallos.append(f'{nombre}: declarada pública y sin token respondió 401')

            print(f'{nombre:68} con token {con} · sin token {sin}')

    print(f'\n{total} operaciones comprobadas contra {API}.')
    for aviso in avisos:
        print('  aviso:', aviso)
    if fallos:
        for fallo in fallos:
            print('  FALLO:', fallo)
        sys.exit(1)
    print('Ninguna responde 404 por ruta inexistente ni 405, y el candado de cada una es el '
          'declarado.')


if __name__ == '__main__':
    main()
