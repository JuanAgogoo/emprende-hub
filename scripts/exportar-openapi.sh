#!/usr/bin/env bash
#
# Exporta la especificación de la API a openapi.json, en la raíz del repositorio.
#
# La genera springdoc a partir de los controladores, así que el fichero se
# vuelve a exportar en el mismo PR que cambia un endpoint. Necesita la API
# levantada (docker compose up -d, o bootRun) y python3 para darle formato.
#
#   ./scripts/exportar-openapi.sh                    # contra http://localhost:8080
#   API=http://localhost:8081 ./scripts/exportar-openapi.sh
#
# Después, validarlo contra el estándar, sin instalar nada:
#
#   npx --yes @redocly/cli@2.63.0 lint openapi.json
#
set -euo pipefail

API="${API:-http://localhost:8080}"
cd "$(dirname "$0")/.."

curl -fsS "$API/v3/api-docs" > openapi.json.tmp
python3 - openapi.json.tmp <<'PY'
import json, sys
ruta = sys.argv[1]
with open(ruta, encoding='utf-8') as f:
    documento = json.load(f)
with open('openapi.json', 'w', encoding='utf-8') as f:
    json.dump(documento, f, ensure_ascii=False, indent=2)
    f.write('\n')
operaciones = sum(len(item) for item in documento['paths'].values())
print(f"openapi.json: OpenAPI {documento['openapi']}, {len(documento['paths'])} rutas "
      f"y {operaciones} operaciones")
PY
rm openapi.json.tmp
