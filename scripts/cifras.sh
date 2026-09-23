#!/usr/bin/env bash
#
# Cuenta las cifras que los documentos entregables repiten, y avisa si alguno
# se quedó atrás.
#
# Existe porque ya pasó dos veces: `plan-de-entrega.md` decía 27 endpoints
# donde había 28, y el README llegó a arrastrar cuatro números falsos a la vez.
# El propio plan lo dejó escrito: «un número en un documento entregable envejece
# peor que no ponerlo». Esto es la alternativa a confiar.
#
#   ./scripts/cifras.sh
#
# Las pruebas y la cobertura salen del último `./gradlew build`. Si no se ha
# ejecutado nunca, esas dos filas avisan en vez de mentir.

set -uo pipefail
cd "$(dirname "$0")/.."

rojo=$'\e[31m'; verde=$'\e[32m'; gris=$'\e[90m'; fin=$'\e[0m'
problemas=0

# ── Lo que hay de verdad ─────────────────────────────────────────────────

# Endpoints: pares método+ruta distintos de las tablas de api.md.
endpoints=$(python3 - <<'PY'
import re, pathlib
pares = set()
for linea in pathlib.Path('docs/api.md').read_text().splitlines():
    if not linea.startswith('| `'):
        continue
    celdas = [c.strip() for c in linea.strip('|').split('|')]
    if len(celdas) < 2:
        continue
    ruta = re.search(r'`([^`]+)`', celdas[1])
    if not ruta:
        continue
    for metodo in re.findall(r'`(GET|POST|PUT|PATCH|DELETE)`', celdas[0]):
        pares.add(f'{metodo} {ruta.group(1)}')
print(len(pares))
PY
)

peticiones=$(python3 - <<'PY'
import json
coleccion = json.load(open('backend/postman/EmprendeHub.postman_collection.json'))
def contar(items):
    return sum(contar(i['item']) if 'item' in i else ('request' in i) for i in items)
print(contar(coleccion['item']))
PY
)

pruebas=$(python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
ficheros = glob.glob('backend/build/test-results/test/*.xml')
if not ficheros:
    print('?')
else:
    print(sum(int(ET.parse(f).getroot().get('tests', 0)) for f in ficheros))
PY
)

cobertura=$(python3 - <<'PY'
import glob, xml.etree.ElementTree as ET
for fichero in glob.glob('backend/build/reports/jacoco/**/*.xml', recursive=True):
    for paquete in ET.parse(fichero).getroot().iter('package'):
        if paquete.get('name', '').endswith('service'):
            for contador in paquete.findall('counter'):
                if contador.get('type') == 'INSTRUCTION':
                    cubierto = int(contador.get('covered'))
                    perdido = int(contador.get('missed'))
                    # Con coma decimal, que es como lo escriben los documentos.
                    print(f'{cubierto / (cubierto + perdido) * 100:.1f}'.replace('.', ','))
                    raise SystemExit
print('?')
PY
)

echo
echo "  endpoints en api.md ......... $endpoints"
echo "  peticiones en Postman ....... $peticiones"
echo "  pruebas del backend ......... $pruebas"
echo "  cobertura de service/** ..... $cobertura%"
echo

if [ "$pruebas" = "?" ] || [ "$cobertura" = "?" ]; then
  echo "  ${gris}Sin informes de build: 'cd backend && ./gradlew build' para medirlas.${fin}"
  echo
fi

# ── Dónde se repiten, y si coinciden ─────────────────────────────────────
#
# Solo los documentos que afirman el estado ACTUAL. Los planes de entrega
# guardan cifras dentro de sus notas de PR —«verificado: 429 pruebas»— y esas
# son un registro de lo que era cierto ese día: no se tocan.

comprobar() { # <fichero> <etiqueta> <valor esperado> <patrón grep>
  local fichero=$1 etiqueta=$2 esperado=$3 patron=$4
  [ "$esperado" = "?" ] && return 0

  local encontrado
  encontrado=$(grep -oP "$patron" "$fichero" 2>/dev/null | head -1)

  if [ -z "$encontrado" ]; then
    printf '  %-34s %s\n' "$fichero · $etiqueta" "${gris}no lo menciona${fin}"
  elif [ "$encontrado" = "$esperado" ]; then
    printf '  %-34s %s\n' "$fichero · $etiqueta" "${verde}$encontrado ✓${fin}"
  else
    printf '  %-34s %s\n' "$fichero · $etiqueta" "${rojo}dice $encontrado, son $esperado${fin}"
    problemas=$((problemas + 1))
  fi
}

comprobar README.md      "endpoints"  "$endpoints"  '\d+(?= endpoints)'
comprobar README.md      "peticiones" "$peticiones" '\d+(?= peticiones)'
comprobar README.md      "pruebas"    "$pruebas"    '\d+(?= pruebas en verde)'
comprobar README.md      "cobertura"  "$cobertura"  '\d+,\d+(?=% de cobertura)'
comprobar docs/api.md    "endpoints"  "$endpoints"  '\d+(?= endpoints\*\* en)'
comprobar docs/api.md    "peticiones" "$peticiones" '(?<=endpoints\*\* en )\d+'

echo
if [ "$problemas" -eq 0 ]; then
  echo "  ${verde}Las cifras cuadran.${fin}"
else
  echo "  ${rojo}$problemas cifra(s) desfasada(s).${fin} Corregirlas donde lo dice cada línea."
fi
echo

exit "$problemas"
