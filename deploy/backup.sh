#!/usr/bin/env bash
# Copia de seguridad: se ejecuta EN EL SERVIDOR, desde la carpeta del proyecto.
# La base y las fotos van por separado; hacen falta las dos para restaurar.
#
#   ./deploy/backup.sh
set -euo pipefail

cd "$(dirname "$0")/.."
DESTINO="${BACKUP_DIR:-$PWD/backups}"
CONSERVAR="${BACKUP_CONSERVAR:-7}"
FECHA="$(date +%Y-%m-%d_%H%M%S)"
COMPOSE=(docker compose -f docker-compose.prod.yml)

mkdir -p "$DESTINO"
chmod 700 "$DESTINO"

"${COMPOSE[@]}" exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  | gzip > "$DESTINO/base_$FECHA.sql.gz.tmp"
mv "$DESTINO/base_$FECHA.sql.gz.tmp" "$DESTINO/base_$FECHA.sql.gz"

docker run --rm -v emprendehub-prod_fotos:/fotos:ro alpine:3.22 tar czf - -C /fotos . \
  > "$DESTINO/fotos_$FECHA.tar.gz.tmp"
mv "$DESTINO/fotos_$FECHA.tar.gz.tmp" "$DESTINO/fotos_$FECHA.tar.gz"

# Rotación: solo toca los ficheros con este nombre, de más nuevo a más viejo.
for tipo in base_ fotos_; do
  ls -1t "$DESTINO/$tipo"* | tail -n "+$((CONSERVAR + 1))" | xargs -r rm -f --
done

ls -lh "$DESTINO" | tail -n +2
echo "Copia terminada: $FECHA"
