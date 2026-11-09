#!/usr/bin/env bash
# Despliega desde el portátil: copia el código al servidor y reconstruye.
# Se puede correr las veces que haga falta; no toca el .env ni los datos.
#
#   ./deploy/deploy.sh
#   DEPLOY_DESTINO=deploy@2.24.113.157 ./deploy/deploy.sh
set -euo pipefail

DESTINO="${DEPLOY_DESTINO:-deploy@2.24.113.157}"
LLAVE="${DEPLOY_LLAVE:-$HOME/.ssh/emprendehub_vps}"
CARPETA="${DEPLOY_CARPETA:-/opt/emprendehub}"
SSH=(ssh -i "$LLAVE" -o ConnectTimeout=15)

cd "$(dirname "$0")/.."

"${SSH[@]}" "$DESTINO" "mkdir -p '$CARPETA'"
if ! "${SSH[@]}" "$DESTINO" "test -f '$CARPETA/.env'"; then
  echo "Falta $CARPETA/.env en el servidor (plantilla: deploy/.env.example)." >&2
  exit 1
fi

# --delete quita lo que ya no existe en el repo; lo excluido no se toca, y por
# eso el .env y las copias de seguridad del servidor sobreviven.
rsync -az --delete -e "${SSH[*]}" \
  --exclude .git --exclude .github --exclude .claude --exclude .gradle \
  --exclude node_modules --exclude build --exclude dist --exclude uploads \
  --exclude .env --exclude backups --exclude fotos-demo \
  ./ "$DESTINO:$CARPETA/"

"${SSH[@]}" "$DESTINO" "cd '$CARPETA' && docker compose -f docker-compose.prod.yml up -d --build --remove-orphans && docker compose -f docker-compose.prod.yml ps"
