# Despliegue en producción

EmprendeHub está publicado en <https://emprendehub.tech> en un VPS Ubuntu 24.04
(2 CPU, 8 GB de RAM). El servidor y el dominio **se apagan el 9 de diciembre de
2026**: ver [Apagar todo](#apagar-todo-el-9-de-diciembre).

## Arquitectura

```text
Internet ──443/80──► web (Caddy) ─┬─ archivos del frontend (/srv)
                                  └─ /api/v1/* y /fotos/* ──► backend:8080 ──► postgres:5432
```

- **Mismo origen.** El frontend llama a `/api/v1` con ruta relativa y el backend
  no tiene CORS, así que Caddy sirve las dos cosas bajo el mismo dominio.
- **Solo Caddy publica puertos.** Docker se salta UFW con lo que publica; por eso
  `postgres` y `backend` no publican nada.
- **HTTPS automático** con Let's Encrypt. El certificado vive en el volumen
  `caddy_datos`, para no pedirlo de nuevo en cada despliegue.
- **Volúmenes:** `datos` (base), `fotos` (`/data/uploads`), `caddy_datos` y
  `caddy_config`. Docker les antepone el proyecto: `emprendehub-prod_fotos`.

| Archivo | Para qué |
|---|---|
| `docker-compose.prod.yml` | Los tres servicios de producción |
| `backend/Dockerfile.prod`, `frontend/Dockerfile.prod` | Imágenes de producción. Los `Dockerfile` y el `docker-compose.yml` de desarrollo no cambian |
| `deploy/Caddyfile` | Proxy, caché, compresión y rutas de React Router |
| `deploy/.env.example` | Plantilla del `.env` del servidor |
| `deploy/deploy.sh` | Despliega desde el portátil |
| `deploy/backup.sh` | Copia de seguridad, en el servidor |
| `deploy/endurecer-servidor.sh` | Firewall, fail2ban, usuario `deploy` y SSH solo con llave. Se corre una vez |
| `deploy/verificar.mjs` | Prueba de punta a punta contra el sitio |

## Protección al arrancar

Con el perfil `prod`, `ValidadorProduccion` **impide que el backend arranque** si
detecta el JWT público o uno que no sea Base64 de al menos 256 bits, una
contraseña de base vacía o pública, el administrador por defecto o la semilla de
demostración sin contraseña propia.

## Desplegar y actualizar

Desde el portátil, en la raíz del repositorio. Se puede correr las veces que haga
falta:

```bash
./deploy/deploy.sh
```

Sincroniza el código con rsync a `/opt/emprendehub` y corre
`docker compose -f docker-compose.prod.yml up -d --build`. No toca el `.env` ni la
carpeta `backups` del servidor, y los volúmenes se conservan.

Comprobar que todo responde. Lee las contraseñas del archivo indicado y no las
imprime; las cuentas de prueba que crea se llaman `verifica.*@ejemplo.test`:

```bash
BASE=https://emprendehub.tech CREDENCIALES=~/emprendehub-credenciales.txt node deploy/verificar.mjs
```

### Si algo se cae

```bash
ssh -i ~/.ssh/emprendehub_vps deploy@2.24.113.157
cd /opt/emprendehub
docker compose -f docker-compose.prod.yml ps                  # ¿están sanos?
docker compose -f docker-compose.prod.yml logs --tail=100 backend
docker compose -f docker-compose.prod.yml restart             # reinicia sin borrar datos
```

## Copias de seguridad

En el servidor, desde `/opt/emprendehub`:

```bash
./deploy/backup.sh
```

Deja en `backups/` un `base_FECHA.sql.gz` y un `fotos_FECHA.tar.gz`, y conserva
los últimos 7 de cada uno. La base y las fotos van por separado: para restaurar
hacen falta las dos.

### Restaurar

**Esto reemplaza todos los datos actuales.** Haz antes una copia nueva.

```bash
cd /opt/emprendehub
FECHA=2026-11-09_083854      # la copia que quieras restaurar
C="docker compose -f docker-compose.prod.yml"

$C stop backend

$C exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"'
zcat backups/base_$FECHA.sql.gz | $C exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

docker run --rm -v emprendehub-prod_fotos:/fotos -v "$PWD/backups":/b:ro alpine:3.22 \
  sh -c "rm -rf /fotos/* && tar xzf /b/fotos_$FECHA.tar.gz -C /fotos"

$C start backend
```

## Fotos y textos de los negocios de demostración

`CargaInicialDemo` crea los negocios con imágenes de una letra. Para sustituirlas
por fotos reales y completar los textos, sin borrar negocios, cuentas, opiniones
ni consultas:

```bash
python3 deploy/preparar-fotos-demo.py                     # deja las fotos en deploy/fotos-demo/
python3 deploy/aplicar-demo.py --destino produccion       # se puede repetir
python3 deploy/aplicar-demo.py --destino produccion --limpiar   # al final
```

Haz antes un backup. Los textos y qué foto va en cada negocio están en
`deploy/contenido-demo.json`, y los créditos en
[creditos-fotos.md](creditos-fotos.md). El modelo no tiene campo de horario: va
al final de la descripción. Los archivos de letras solo se borran con `--limpiar`.

## Variables de entorno

Van en `/opt/emprendehub/.env` con permisos 600, nunca en git. Tras cambiarlas:
`docker compose -f docker-compose.prod.yml up -d backend`.

| Variable | Valor en producción | Para qué |
|---|---|---|
| `DOMINIO` | `emprendehub.tech` | Dominio del certificado |
| `APP_URL` | `https://emprendehub.tech` | Enlaces de los correos |
| `DB_NAME`, `DB_USER` | Ver `.env.example` | Base de datos |
| `DB_PASSWORD` | `openssl rand -base64 32` | Contraseña de la base |
| `JWT_SECRET` | `openssl rand -base64 48` | Firma de las sesiones. Base64 de 256 bits o más |
| `JWT_EXPIRATION` | `3600000` | Duración de la sesión, en ms |
| `ADMIN_CORREO`, `ADMIN_CONTRASENA`, `ADMIN_NOMBRE` | Propios | El administrador, creado con la base vacía |
| `MODERACION_AUTOMATICA` | `true` en el evento | `true` publica sin revisión |
| `SEMILLA_DEMO` | `true` | Con la base vacía crea los datos de demostración. Si ya hay negocios no hace nada |
| `SEMILLA_DEMO_CONTRASENA` | `openssl rand -base64 32` | Contraseña de las cuentas de demostración |
| `RECUPERACION_MINUTOS` | `30` | Vigencia del enlace para recuperar la contraseña |
| `MAIL_HOST`, `MAIL_PORT` | `127.0.0.1`, `1` | Servidor SMTP. Estos valores significan «sin correo» |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Vacíos | Credenciales SMTP |
| `MAIL_AUTH`, `MAIL_STARTTLS` | `false` | `true` con un proveedor real |
| `MAIL_FROM` | Remitente | Debe estar verificado en el proveedor |
| `MAIL_TIMEOUT_MS` | `3000` | Tiempo máximo de espera del SMTP |

## Conectar un correo real

Sin SMTP la aplicación funciona: la acción principal se completa y el fallo queda
en el log, pero los correos no llegan. Para conectar un proveedor basta cambiar el
`.env` y recrear el backend. En los dos casos **`MAIL_FROM` debe ser un remitente
o dominio verificado en el proveedor**.

**Brevo** (la clave SMTP y el usuario salen de *SMTP & API*):

```dotenv
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=tu-usuario-smtp@smtp-brevo.com
MAIL_PASSWORD=tu-clave-smtp
MAIL_AUTH=true
MAIL_STARTTLS=true
MAIL_FROM=no-responder@emprendehub.tech
```

**Resend** (el usuario es literalmente `resend` y la contraseña, la API key):

```dotenv
MAIL_HOST=smtp.resend.com
MAIL_PORT=587
MAIL_USERNAME=resend
MAIL_PASSWORD=tu-api-key
MAIL_AUTH=true
MAIL_STARTTLS=true
MAIL_FROM=no-responder@emprendehub.tech
```

## Volver a la moderación manual

En el `.env`, borrar la línea `MODERACION_AUTOMATICA=true` y recrear el backend:

```bash
docker compose -f docker-compose.prod.yml up -d backend
```

Desde entonces, cada negocio y foto nueva espera aprobación del administrador. Lo
que ya estaba publicado sigue publicado.

## Apagar todo el 9 de diciembre

1. Copia final, en el servidor: `./deploy/backup.sh`.
2. Bajarla al portátil, si se quiere conservar:
   `scp -i ~/.ssh/emprendehub_vps -r deploy@2.24.113.157:/opt/emprendehub/backups .`
3. Detener los servicios, sin borrar los volúmenes:
   `docker compose -f docker-compose.prod.yml down`.

Al vencer el plan, Hostinger retira el servidor con todo lo que tenga dentro. El
QR del póster deja de funcionar cuando venza el dominio.

## Estado final (9 de noviembre de 2026)

**Prueba de punta a punta en el sitio real:** `verificar.mjs`, 31 de 31 correctas.
Sus cuentas y su negocio de prueba se borraron después.

**Comprobado además en el sitio real:**

- HTTP/2 con certificado válido de Let's Encrypt; `http` redirige a `https`.
- Los puertos 5432, 8080 y 8025 están cerrados desde fuera.
- 40 negocios publicados con sus fotos, 12 categorías y 8 cursos.
- Sin desborde horizontal a 360, 390, 414 y 768 px en las pantallas públicas.
- Lighthouse móvil: rendimiento 99, accesibilidad entre 96 y 98, buenas prácticas
  100 y SEO entre 91 y 92.

**Servidor:** UFW solo con 22, 80, 443 y 443/udp; fail2ban activo; SSH solo con
llave; usuario `deploy` con sudo y Docker.

**Copias:** `backup.sh` se ejecutó y sus archivos pasan `gzip -t`. La restauración
se ensayó con estos mismos comandos en una copia local.

**Comprobado en una copia local idéntica:** las pantallas con sesión de
emprendedor y de administrador, a tamaño de celular.

**Limitaciones conocidas:**

- Los correos no salen hasta conectar un proveedor SMTP.
- `/robots.txt` devuelve la página de la aplicación, lo que resta unos puntos de
  SEO y no afecta al uso.
