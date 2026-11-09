#!/usr/bin/env python3
"""Descarga y prepara las fotos de los negocios de demostración.

Lee deploy/contenido-demo.json, baja cada foto de su URL directa de Pexels y la
deja en deploy/fotos-demo/ (ignorada por git): máximo 1200 px, JPEG y 150 KB o
menos. Después escribe docs/creditos-fotos.md. Se puede correr varias veces.

    python3 deploy/preparar-fotos-demo.py
"""
import io
import json
import pathlib
import time
import urllib.request

from PIL import Image

RAIZ = pathlib.Path(__file__).resolve().parent.parent
CONTENIDO = json.loads((RAIZ / 'deploy/contenido-demo.json').read_text(encoding='utf-8'))
SALIDA = RAIZ / 'deploy/fotos-demo'
LIMITE = 150 * 1024


def bajar(pexels):
    url = (f'https://images.pexels.com/photos/{pexels}/pexels-photo-{pexels}.jpeg'
           '?auto=compress&cs=tinysrgb&w=1600')
    peticion = urllib.request.Request(url, headers={'User-Agent': 'EmprendeHub-demo/1.0'})
    with urllib.request.urlopen(peticion, timeout=40) as respuesta:
        return respuesta.read()


def comprimir(datos, lado):
    """Calidad 80 a 75; si aún pesa más de 150 KB, reduce el tamaño."""
    original = Image.open(io.BytesIO(datos)).convert('RGB')
    while True:
        imagen = original.copy()
        imagen.thumbnail((lado, lado), Image.LANCZOS)
        for calidad in (80, 78, 75):
            salida = io.BytesIO()
            imagen.save(salida, 'JPEG', quality=calidad, optimize=True, progressive=True)
            if salida.tell() <= LIMITE:
                return salida.getvalue()
        lado -= 100


def preparar(nombre, pexels, lado, cache):
    destino = SALIDA / nombre
    if not destino.exists():
        if pexels not in cache:
            cache[pexels] = bajar(pexels)
            time.sleep(0.15)
        destino.write_bytes(comprimir(cache[pexels], lado))
    return destino


def main():
    SALIDA.mkdir(exist_ok=True)
    autores = CONTENIDO['autores']
    cache = {}
    filas = []
    for negocio in CONTENIDO['negocios']:
        for orden, pexels in enumerate(negocio['fotos'], 1):
            archivo = f"demo-n{negocio['id']:02d}-{orden}-{pexels}.jpg"
            preparar(archivo, pexels, 1200, cache)
            filas.append((negocio['nombre'], archivo, pexels))
    for producto, pexels in CONTENIDO['productos'].items():
        archivo = f'demo-p-{pexels}.jpg'
        preparar(archivo, pexels, 800, cache)
        filas.append((f'Producto: {producto}', archivo, pexels))

    lineas = [
        '# Créditos de las fotos de demostración', '',
        'Fotos de [Pexels](https://www.pexels.com/license/), de uso libre y sin atribución '
        'obligatoria. Los archivos no están en git: los genera '
        '`deploy/preparar-fotos-demo.py`. El autor es el nombre de usuario en Pexels.', '',
        '| Negocio | Archivo | Fuente | Autor | Foto original |', '|---|---|---|---|---|',
    ]
    for negocio, archivo, pexels in filas:
        lineas.append(f'| {negocio} | `{archivo}` | Pexels | {autores[str(pexels)]} '
                      f'| <https://www.pexels.com/photo/{pexels}/> |')
    (RAIZ / 'docs/creditos-fotos.md').write_text('\n'.join(lineas) + '\n', encoding='utf-8')
    pesos = [p.stat().st_size for p in SALIDA.glob('demo-*.jpg')]
    print(f'{len(pesos)} fotos, máximo {max(pesos) // 1024} KB, total {sum(pesos) // 1024 // 1024} MB')


if __name__ == '__main__':
    main()
