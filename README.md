# EmprendeHub

**Un directorio de los emprendimientos del Valle de Aburrá, con su catálogo de
cursos para emprendedores.** Cualquiera explora sin registrarse los negocios, con
su galería, su escaparate y sus opiniones. Quien tiene cuenta califica, opina y
les escribe. Cada emprendedor gestiona su negocio desde un panel con buzón,
métricas de visitas y avisos, y un administrador modera lo que se publica antes
de que llegue al público. Proyecto final de la asignatura Plataformas de
Programación Empresarial (UPB): una API REST en Spring Boot y una aplicación de
una sola página en React, en un solo repositorio.

## Integrantes

- Juan José Arango
- David Vallejo

## Tecnologías

Las versiones salen de los ficheros del repositorio: `backend/build.gradle`, el
wrapper de Gradle, `docker-compose.yml` y `frontend/package-lock.json`.

| Pieza | Tecnología | Versión |
|---|---|---|
| API REST | Java (toolchain de Gradle) | 25 |
| | Spring Boot (Web MVC, Data JPA, Security, Validation, Mail) | 4.1.1 |
| | Gradle (wrapper) | 9.5.1 |
| | jjwt, para el JWT | 0.12.5 |
| | springdoc-openapi, para Swagger y `openapi.json` | 3.1.1 |
| | JUnit 5, Mockito y Testcontainers, para las pruebas | Los que fija Spring Boot 4.1.1 |
| Base de datos | PostgreSQL (imagen `postgres:17-alpine`) | 17 |
| Interfaz web | React | 19.2.8 |
| | React Router (`react-router-dom`) | 7.18.3 |
| | Vite | 8.3.5 |
| | TypeScript | 6.0.3 |
| | Vitest, con jsdom, para las pruebas | 5.0.5 · 30.1.3 |
| Correo en local | Mailpit (imagen `axllent/mailpit`) | La última de la imagen |

## Qué hace

Cuatro perfiles, cada uno con lo suyo:

| Perfil | Qué puede hacer |
|---|---|
| **Visitante**, sin cuenta | Buscar y filtrar el directorio, ver perfiles con galería y escaparate, leer opiniones y recorrer el catálogo de cursos |
| **Cliente** | Además: calificar y opinar, denunciar una opinión ajena y escribirle a un negocio |
| **Emprendedor** | Registrar su negocio con un asistente de cuatro pasos y gestionarlo: datos, fotos, escaparate, buzón de consultas, visitas y avisos |
| **Administrador** | Un panel con sus colas: aprobar negocios y cambios con vista previa, resolver denuncias, suspender cuentas, publicar cursos y consultar el historial de moderación |

Un negocio nuevo no se ve hasta que el administrador lo aprueba, y sus cambios
posteriores esperan revisión sin sacarlo del directorio. Las reglas, con su
porqué, están en [docs/decisiones-dominio.md](docs/decisiones-dominio.md).

## Requisitos previos

| Para qué | Qué hace falta | Verificado con |
|---|---|---|
| **Levantar el sistema** | Docker Engine o Docker Desktop, con Docker Compose y su orden `docker compose`, no el antiguo `docker-compose`. **Nada más**: el JDK, Gradle y Node van dentro de las imágenes | Docker 29.9.1 · Compose 5.6.0 |
| **Pruebas del backend** | Docker en marcha, porque las pruebas levantan su propio PostgreSQL con Testcontainers. Y un **JDK 17 o superior** para lanzar `./gradlew`: el JDK 25 que pide el proyecto lo descarga Gradle la primera vez, con el plugin foojay | JDK 17.0.20 y JDK 21.0.5 |
| **Pruebas del frontend** | Node.js **22.22.2 o superior** en la rama 22, o **24.15 o superior**, con npm. Es lo que piden Vitest y jsdom | Node 24.19.0 · npm 11.17.0 |
| Los scripts de comprobación, opcionales | bash y Python 3, para `scripts/cifras.sh`, `scripts/exportar-openapi.sh` y `scripts/comprobar-openapi.py` | Python 3.14.4 |

## Levantar el sistema desde un clon limpio

```bash
git clone https://github.com/JuanAgogoo/emprende-hub.git
cd emprende-hub
cp .env.example .env
```

**Preparar la base de datos es elegir sus credenciales.** No están en el
repositorio: abre el `.env` que acabas de copiar y escribe, después del `=`, un
usuario en `DB_USER` y una contraseña en `DB_PASSWORD`, los que quieras.
PostgreSQL crea la base `emprendehub` y ese usuario la primera vez que arranca, y
el backend los usa para conectar. No hay que crear nada más: el esquema lo genera
Hibernate al arrancar y los datos de prueba se siembran solos.

```bash
docker compose up -d
docker compose logs -f backend   # esperar a «Sembrados 40 negocios …» y salir con Ctrl+C
```

Sin el `.env`, o con las credenciales vacías, Compose no arranca y dice cuál
falta: `Falta DB_USER. Copia .env.example a .env y elige un usuario`.

**La primera vez tarda unos minutos**: se construyen las imágenes y Gradle
descarga sus dependencias dentro de un volumen. Después, el backend arranca en
segundos y siembra los [datos de prueba](#datos-de-prueba), entre 11 y 18
segundos más según la máquina. Durante la siembra la API ya responde, pero el
directorio sale vacío hasta que termina. Los siguientes arranques no siembran
nada y son segundos.

> **Si cambias la contraseña del `.env` después de haber arrancado**, la base ya
> existe con la anterior y el backend no puede entrar. `docker compose down -v`
> la borra, y el siguiente `up` la crea de nuevo.

### Dónde queda cada cosa

| Qué | URL | Puerto |
|---|---|---|
| **La web** | <http://localhost:5173> | 5173 |
| **La API** | <http://localhost:8080/api/v1> | 8080 |
| Swagger UI | <http://localhost:8080/swagger-ui.html> | 8080 |
| La bandeja de correo (Mailpit) | <http://localhost:8025> | 8025, y 1025 el SMTP |
| PostgreSQL, para un cliente de base de datos | `localhost:5433`, base `emprendehub` | 5433 |

### Cómo llega el navegador a la API

**El navegador nunca llama al 8080.** La web pide la API a
`http://localhost:5173/api/v1`, su mismo origen, y el servidor de Vite la
reenvía a `http://localhost:8080/api/v1` con su proxy (`frontend/vite.config.ts`).
Lo mismo con las fotos, en `/fotos`. Como todo sale del mismo origen, **no hace
falta CORS**, y el backend no lo configura.

La API también responde directamente en el 8080, que es donde se usan Swagger,
Postman y `curl`.

- **`VITE_PROXY_TARGET`** cambia a dónde reenvía el proxy. Por defecto es
  `http://localhost:8080`; dentro de Docker, `docker-compose.yml` lo pone en
  `http://backend:8080`, porque ahí `localhost` sería el propio contenedor de la
  web.
- **`VITE_API_URL`** es la ruta que pide el navegador, `/api/v1` por defecto, y
  tiene que ser **del mismo origen**. Apuntarla a `http://localhost:8080` dejaría
  al navegador llamando a otro origen sin CORS, y el inicio de sesión fallaría.

### Parar y limpiar

```bash
docker compose stop           # para todo, sin borrar nada
docker compose down           # además retira los contenedores; los datos siguen
docker compose down -v        # borra también la base y las cachés: el siguiente arranque vuelve a sembrar
```

Las fotos **no** se borran con `down -v`: viven en `backend/uploads/`, en el
disco. Después de un `down -v` sobran ahí, y la siembra siguiente dibuja otras.

### Sin Docker para las dos mitades

Si prefieres `bootRun` y `npm run dev` en el host, Docker solo pone la base y el
correo. **No mezcles los dos modos**: los dos quieren el 8080 y el 5173, y el
segundo en arrancar falla. Para cambiar, `docker compose stop backend frontend`
primero.

En una terminal, desde la raíz, la base, el correo y la API (necesita un JDK 17
o superior):

```bash
docker compose up -d postgres mailpit   # la base en el 5433 y el correo en el 1025
cd backend
set -a && . ../.env && set +a           # el backend lee las credenciales del entorno
./gradlew bootRun                       # la API en el 8080
```

Sin el `set -a`, el backend arranca sin credenciales y falla con `password
authentication failed for user "${DB_USER}"`: Spring no lee el `.env`, Compose sí.

En otra, desde la raíz, la web (necesita Node):

```bash
cd frontend
npm ci                                  # solo la primera vez
npm run dev                             # la web en el 5173
```

Mailpit entra también aquí: sin un SMTP escuchando, el correo de recuperación no
sale. La aplicación no falla por eso —la petición sigue devolviendo `200`—, pero
el enlace no llega a ninguna parte.

## Variables de entorno

**Con Docker Compose** se ponen en el `.env` de la raíz. Solo las dos primeras
son obligatorias:

| Variable | Por defecto | Para qué |
|---|---|---|
| `DB_USER` / `DB_PASSWORD` | **Ninguno: obligatorias.** Las eliges tú | Las credenciales de la base. Compose se las da a PostgreSQL y al backend |
| `SEMILLA_DEMO` | `true` | A `false`, el primer arranque no siembra los datos de prueba |
| `DB_PORT` | `5433` | El puerto de PostgreSQL en el equipo. El 5432 suele estar ocupado |
| `API_PORT` | `8080` | El puerto de la API en el equipo |
| `WEB_PORT` | `5173` | El puerto de la web en el equipo |
| `SMTP_PORT` | `1025` | El SMTP de Mailpit en el equipo |
| `MAILPIT_PORT` | `8025` | La bandeja de Mailpit en el equipo |
| `HOST_UID` / `HOST_GID` | `1000` | El usuario con el que corren los contenedores, para que las fotos de `backend/uploads/` no queden siendo de root |

**El backend** las lee de `backend/src/main/resources/application.yml`. Con
Docker, Compose le fija `DB_URL`, `MAIL_HOST` y `MAIL_PORT` para que encuentre la
base y el correo dentro de su red, y le pasa `DB_USER`, `DB_PASSWORD` y
`SEMILLA_DEMO`. **El resto solo se cambia con `bootRun`**, exportándolas antes de
arrancar:

| Variable | Por defecto | Para qué |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/emprendehub` | Dónde está la base |
| `DB_USER` / `DB_PASSWORD` | **Ninguno**: llegan del entorno | Las credenciales de la base |
| `PORT` | `8080` | El puerto de la API |
| `SEMILLA_DEMO` | `true` | Sembrar los datos de prueba con la base vacía |
| `MODERACION_AUTOMATICA` | `false` | A `true`, los negocios nacen publicados y las ediciones se publican al momento: las colas del administrador salen vacías |
| `JWT_SECRET` | Una clave de desarrollo | Firma de los tokens. Sustituirla en cualquier despliegue |
| `JWT_EXPIRATION` | `3600000` | Lo que vive un token, en milisegundos: una hora |
| `ADMIN_CORREO` / `ADMIN_CONTRASENA` | `admin@emprendehub.co` / `admin12345` | La cuenta de administrador que se crea al arrancar |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | El SMTP: Mailpit |
| `MAIL_FROM` | `no-responder@emprendehub.co` | El remitente de los correos |
| `APP_URL` | `http://localhost:5173` | La web, que es lo que lleva el enlace del correo de recuperación |
| `RECUPERACION_MINUTOS` | `30` | Lo que vive un enlace de recuperación |
| `FOTOS_DIR` | `./uploads` | Dónde se guardan las imágenes, relativo a `backend/` |

**El frontend** las lee al arrancar Vite:

| Variable | Por defecto | Para qué |
|---|---|---|
| `VITE_PROXY_TARGET` | `http://localhost:8080`; con Docker, `http://backend:8080` | A dónde reenvía el proxy de Vite la API y las fotos |
| `VITE_API_URL` | `/api/v1` | La ruta de la API que pide el navegador. Siempre del mismo origen, como se explica [arriba](#cómo-llega-el-navegador-a-la-api) |
| `VITE_SONDEO` | Sin definir; con Docker, `true` | Que Vite vigile los ficheros sondeando el disco: dentro de Docker, el aviso de cambio no siempre llega |

## Usuarios y datos de prueba

### Datos de prueba

**Al arrancar por primera vez, con la base vacía, ya hay con qué recorrer todo**,
sin crear nada antes. Se siembran los catálogos —12 categorías y las 4 ciudades
del Valle de Aburrá, Medellín con 2 barrios—, la cuenta de administrador, los 8
cursos y **los datos de prueba**, generados por código a partir de una semilla
fija: cada base nueva tiene los mismos negocios, cuentas y cifras. Las fechas se
anclan al día del arranque.

Pasan por los mismos servicios que la API, con sus reglas: una opinión por
persona y negocio (C2), la foto obligatoria de cada producto (F4) y las fotos
subidas **antes** de aprobar (B2). Las imágenes se dibujan al sembrar —el color
de la categoría y la inicial— y se guardan en `FOTOS_DIR`, porque `uploads/` no
está en git.

La siembra es idempotente: si ya hay negocios no toca nada, así que reiniciar no
duplica. Para empezar de cero, `docker compose down -v`. **Tarda entre 11 y 18
segundos, según la máquina, y solo en el primer arranque** (medido: de 11,2 a
12,2 s en el equipo de verificación y 17,7 s en otro). La mayor parte es el hash
de las 71 contraseñas y el dibujo de las fotos. Durante ese rato la API ya responde
pero el directorio sale vacío: esperar en los logs a la línea
`Sembrados 40 negocios`.

| Qué | Cuánto, medido sobre una base nueva |
|---|---|
| Negocios | **40**: 37 publicados, 2 pendientes y 1 rechazado, repartidos por las 12 categorías, las 4 ciudades y los 3 niveles de precio |
| Cuentas | **72**: el administrador, 31 clientes y 40 emprendedores |
| Productos del escaparate | 179, cada uno con su foto |
| Fotos de galería | 96: 90 publicadas y 6 pendientes de revisión |
| Opiniones | 216 |
| Consultas al buzón | 69, las de más de una semana ya leídas |
| Visitas | Unas 5 400 en los últimos 90 días: 5 362 arrancando el 29 de octubre de 2026. Es la única cifra que cambia con el día del arranque, porque depende de dónde caen los fines de semana |
| Avisos | 324 |
| Cola de negocios | 2 pendientes de aprobar |
| Cola de cambios | 2: uno de texto y una foto nueva |
| Cola de denuncias | 2 abiertas, y 1 ya desestimada |
| Historial de moderación | 40 entradas |

Los que tienen un papel en el recorrido:

| Negocio | Para qué está |
|---|---|
| **Café Pergamino** | El de la emprendedora fija: 4 fotos, 6 productos, 10 opiniones, buzón, visitas y avisos sin leer |
| **Estudio Arrayán** y **Tienda deportiva Mandarina** | Pendientes: hay algo que aprobar en directo (B6) |
| **Casa El Mirador** | Rechazado, con su motivo a la vista de su dueño |
| **Academia Aurora** | Propone una descripción nueva (B2-bis) |
| **Fotografía Tierra Viva** | Subió una foto ya publicado, que espera en «Cambios» (B2) |
| **Peluquería canina Trapiche** y **Tienda Buen Viaje** | Tienen una opinión denunciada, en «Denuncias» (C3) |
| **Laboratorio Siete Cueros** | Recién publicado y sin opiniones: se ve «Sin opiniones» y no «0,0» (C5) |
| **Estudio Santa Elena** | Sin fotos de galería: sale el marcador con la inicial |
| **Barbería El Roble** | El primer destacado de la portada: 9 opiniones (C7) |

**Para arrancar sin datos de prueba**, con la base vacía:
`SEMILLA_DEMO=false docker compose up -d`. Entonces el directorio sale vacío y la
portada muestra ceros con la calificación media nula, que es lo correcto: las
cifras se calculan (H4).

### Cuentas de prueba

| Cuenta | Correo | Contraseña | Para qué sirve |
|---|---|---|---|
| Administrador | `admin@emprendehub.co` | `admin12345` | El panel: colas de moderación, usuarios, cursos e historial |
| Clienta | `cliente@emprendehub.co` | `cliente12345` | Opinar, denunciar y escribir a un negocio. Ya opinó en tres, pero no en Café Pergamino |
| Emprendedora | `emprendedora@emprendehub.co` | `emprende12345` | El panel de Café Pergamino |
| Clientes generados | `cliente01@emprendehub.co` … `cliente30@emprendehub.co` | `demo12345` | Autores de las opiniones y las consultas |
| Emprendedores generados | `emprendedor01@emprendehub.co` … `emprendedor39@emprendehub.co` | `demo12345` | Uno por negocio, en el orden en que se crearon |

Los dueños de los negocios con papel en el recorrido: Estudio Arrayán es
`emprendedor38`, Tienda deportiva Mandarina `emprendedor39`, Casa El Mirador
`emprendedor37`, Academia Aurora `emprendedor04` y Fotografía Tierra Viva
`emprendedor05`.

> Con `MODERACION_AUTOMATICA=true` los negocios nacen publicados y **las colas
> del administrador salen vacías**: no hay pendientes, ni rechazado, ni cambios.

## Pruebas

Cada mitad tiene su comando, y los dos terminan solos: no se quedan esperando
cambios.

| Componente | Comando | Necesita | Resultado medido |
|---|---|---|---|
| **Backend** | `cd backend && ./gradlew check` | Docker en marcha y un JDK 17 o superior | **516 pruebas en verde y 98,6% de cobertura** de líneas en `service/**` |
| **Frontend** | `cd frontend && npm ci && npm test` | Node 22.22.2+ o 24.15+ y npm | **29 pruebas** en 4 ficheros |

### Backend

```bash
cd backend
./gradlew check               # compila, prueba y verifica la cobertura
```

Las pruebas que necesitan base de datos levantan **su propio PostgreSQL con
Testcontainers**, así que Docker tiene que estar en marcha. No usan la base de
`docker compose` ni el `.env`: pasan sin nada escuchando en el 5433.

**La cobertura de `service/**` tiene que quedarse en el 80% o más**: JaCoCo
tiene ese umbral sobre las líneas de esa capa, y por debajo `./gradlew check`
falla. Hoy está en el 98,6% (794 de 805 líneas). Las pruebas siguen los tres
niveles del taller:

| Nivel | Herramienta | Qué prueba |
|---|---|---|
| Unitario | Mockito | Los 16 servicios, con el repositorio simulado |
| Repositorio | `@DataJpaTest` + Testcontainers | Las consultas contra PostgreSQL real |
| Controlador | `@WebMvcTest` + MockMvc | Rutas, códigos y forma del JSON |

Además, `SeguridadAccesoTest` y `SeguridadHttpRealTest` verifican la matriz de
acceso completa, esta última con el servidor levantado.

- Informe de pruebas: `backend/build/reports/tests/test/index.html`
- Informe de cobertura: `backend/build/reports/jacoco/test/html/index.html`

Ejecutar una sola clase con `--tests` hace fallar el comando, porque la
cobertura se mide sobre el paquete entero: para eso, el `check` completo.

### Frontend

**Vitest con jsdom**, un navegador simulado: no abren ninguna ventana ni
necesitan el backend, porque `fetch` se sustituye por respuestas preparadas.
Cubren el servicio de autenticación, el interceptor que adjunta el token y el
CRUD del escaparate.

```bash
cd frontend
npm ci             # instala las dependencias exactas del package-lock
npm test           # vitest run: las pruebas, en una sola pasada
npm run build      # tipos y compilación: 0 errores y 0 advertencias
npm run contraste  # las 21 combinaciones de la paleta contra WCAG 2.2
```

`npm run contraste` lee los colores de `src/estilos/tokens.css` y falla si alguna
combinación baja de su mínimo: 4,5:1 en texto y 3:1 en bordes de control. **No
mide las mezclas con `color-mix()`**, y ahí hay cinco insignias pendientes que no
llegan al mínimo: están listadas en
[docs/pruebas.md](docs/pruebas.md#el-contraste-de-los-tintes-no-lo-mide-ningún-script).

Y lo que ningún script sustituye: **abrirlo en el navegador**, mirarlo a 360 px
de ancho y recorrerlo con el tabulador. El detalle está en
[docs/pruebas.md](docs/pruebas.md#las-pruebas-del-frontend).

`./scripts/cifras.sh` comprueba que las cifras de este README y de
`docs/api.md` —operaciones, peticiones de Postman, pruebas del backend y
cobertura— coinciden con lo medido en el último `./gradlew check`.

## OpenAPI y Swagger

**La especificación es [`openapi.json`](openapi.json), en la raíz del
repositorio**: OpenAPI 3.1, con las 67 operaciones, sus parámetros, sus cuerpos y
todos sus códigos de respuesta, los de error incluidos, y el JWT declarado en
`components/securitySchemes` y aplicado solo en las operaciones protegidas.

Con el backend levantado, la API se explora y se prueba desde el navegador:

| Dirección | Qué es |
|---|---|
| <http://localhost:8080/swagger-ui.html> | La interfaz, con los 67 endpoints agrupados |
| <http://localhost:8080/v3/api-docs> | El mismo documento, en vivo |

Las dos son **públicas**: describen el contrato, que ya es público, y no
devuelven ni un dato. Para probar lo protegido, `POST /api/v1/auth/login` y
pegar el `token` de la respuesta en **Authorize**, arriba a la derecha; no hace
falta escribir «Bearer».

`openapi.json` lo genera springdoc desde los controladores y se exporta con un
script. Validarlo y contrastarlo con la API no instala nada en el proyecto:

```bash
./scripts/exportar-openapi.sh                        # vuelve a escribir openapi.json (API levantada)
npx --yes @redocly/cli@2.63.0 lint openapi.json      # válido según el estándar
python3 scripts/comprobar-openapi.py                 # ni 404 de ruta ni 405; el candado, el declarado
```

`redocly lint` da por válido el fichero y deja ocho avisos a sabiendas: seis
operaciones sin ningún 4xx —los catálogos y las cifras de la portada, que no
fallan—, el proyecto sin licencia y el servidor en `localhost`, que es donde
corre. Qué se anota y dónde está en
[docs/arquitectura.md](docs/arquitectura.md).

## Probar la API

Casi todo exige un token. Se obtiene entrando (el ejemplo usa `jq` para sacarlo
de la respuesta):

```bash
A=http://localhost:8080/api/v1

TOKEN=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"admin@emprendehub.co","contrasena":"admin12345"}' | jq -r .token)

curl -H "Authorization: Bearer $TOKEN" $A/admin/cursos
```

Lo que **no** necesita token, porque el directorio se explora sin registrarse:

```bash
curl $A/catalogos/categorias-negocio
curl $A/catalogos/ciudades
curl "$A/cursos?categoria=MARKETING&gratuito=true"

curl "$A/directorio?texto=pan&ciudadId=2&orden=RECIENTES"
curl $A/directorio/destacados
curl $A/directorio/1          # perfil con galería y escaparate
curl $A/negocios/1/opiniones  # leerlas es público; escribirlas exige sesión
curl $A/estadisticas/portada
```

Subir una foto del negocio va por `multipart`, no por JSON:

```bash
curl -X POST $A/negocios/mio/fotos -H "Authorization: Bearer $TOKEN" \
  -F "archivo=@mi-local.jpg"
```

Las imágenes se guardan en `./uploads` —configurable con `FOTOS_DIR`— y se
descargan de `/fotos/{archivo}`, sin token.

**El contrato completo, con los 67 endpoints y un recorrido de demostración de
punta a punta, está en [docs/api.md](docs/api.md).**

## Colección de Postman

`backend/postman/EmprendeHub.postman_collection.json`, con **79 peticiones que
cubren los 67 endpoints**. La variable `base` apunta a
`http://localhost:8080/api/v1`.

1. Con el sistema levantado, importarla en Postman (*Import → File*).
2. Ejecutar **en orden** la carpeta **1 · Acceso y datos de partida**: crea su
   propia clienta, su emprendedora y su negocio, lo aprueba con el
   administrador y guarda los tokens en variables de la colección.
3. El resto de carpetas heredan esos tokens: no hay que copiar nada a mano.

Trabaja con sus propias cuentas y con algunos datos de prueba que conoce: los
negocios pendientes 39 y 40, los cambios propuestos de los negocios 5 y 6 y las
denuncias 1 y 2. Recorrida entera sobre una base recién sembrada, **69 de las 79
responden bien a la primera**. Las otras diez son a propósito o necesitan un paso
a mano, y cada descripción lo explica:

| Cuántas | Cuáles | Por qué |
|---|---|---|
| 6 | La carpeta **6 · Seguridad** | Son los casos que tienen que fallar: 401, 403 y 400 |
| 2 | «Registrar un negocio» y «Corregir y reenviar un rechazado» | Enseñan las reglas A2 y B1 con un 400. La descripción dice con qué cuenta sembrada se ve el éxito |
| 2 | «Comprobar que el enlace vale» y «Restablecer la contraseña» | El token sale por correo: hay que copiarlo de Mailpit a la variable `tokenRecuperacion` |

Las tres que suben ficheros —una foto de la galería, el alta de un producto y su
cambio de foto— piden **elegir la imagen a mano** en el campo de fichero, porque
Postman no guarda la ruta al exportar.

> **La colección cambia datos, así que no es idempotente.** Una segunda pasada
> devuelve errores —«ya existe una cuenta con ese correo», «solo se resuelven
> negocios pendientes»— que son la regla de negocio haciendo su trabajo. Para
> volver al estado inicial, en el modo de Docker:
>
> ```bash
> docker compose down -v && docker compose up -d
> ```
>
> Y esperar a la línea `Sembrados 40 negocios` en `docker compose logs -f backend`.

| Carpeta | Qué contiene |
|---|---|
| 1 · Acceso y datos de partida | Cuentas, negocio y tokens de la colección, y la recuperación de contraseña |
| 2 · Público | Todo lo que se explora sin registrarse |
| 3 · Cliente | Opinar, denunciar y escribir al buzón |
| 4 · Panel del emprendedor | Negocio, galería, escaparate, buzón, visitas y avisos |
| 5 · Administración | Moderación, denuncias, cuentas y cursos |
| 6 · Seguridad | Los casos que tienen que fallar: 401, 403 y 400 |

## Ver las peticiones según llegan

`docker compose logs -f backend` enseña el arranque y los errores, pero **no las
peticiones**: Spring Boot no las registra por defecto, y eso despista. Para que
salgan, añade al servicio `backend`:

```yaml
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB: DEBUG
```

Y filtra, porque son unas seis líneas por petición:

```bash
docker compose logs -f backend | grep DispatcherServlet
```

Quedan dos por petición: `GET "/api/v1/directorio"` y `Completed 200 OK`. En
PowerShell el filtro es `| Select-String DispatcherServlet`.

## El recorrido de una demostración

El guion de punta a punta, con las dos mitades arriba. Son unos veinte minutos.

> Parte del que se usó en la sustentación del **29 de septiembre de 2026**. Desde
> entonces la moderación ya no se hace desde Postman sino desde el **panel de
> administración**, y el guion lo enseña.

### Antes

Nada que preparar: los [datos de prueba](#datos-de-prueba) traen un negocio
pendiente que aprobar, un cambio y una denuncia por resolver, destacados en la
portada y un panel de emprendedora con métricas. Basta con arrancar el día antes,
para que la primera compilación y la siembra no ocurran delante de nadie.

> **El orden de las fotos importa**, por si se crea un negocio en directo: hay que
> **crearlo, subirle las fotos y aprobarlo al final**. Una foto subida a un
> negocio **ya aprobado** entra pendiente de revisión (B2), el público no la ve y
> el directorio devuelve `fotoPrincipal` nula. Parece un fallo del frontend y es
> la regla funcionando.

### Durante: el guion

**1. Levantar el proyecto.** Un comando y una terminal:

```bash
docker compose up -d
docker compose logs -f backend    # esperar a «Started EmprendeHubApplication»
```

> Hacerlo **antes** de empezar, no delante de nadie: aunque
> las imágenes ya estén construidas, el backend tarda unos segundos en responder
> y la portada enseña su mensaje de error mientras tanto.

**2. La parte pública, sin iniciar sesión.** En <http://localhost:5173>:

- La portada, con las cuatro cifras **calculadas** y los destacados.
- Buscar desde la portada: lleva al directorio con el texto ya puesto.
- En el directorio, filtrar por categoría, cambiar de ciudad —el selector de
  barrio se repuebla solo— y ordenar por calificación.
- Entrar a un negocio: la foto de la tarjeta crece hasta ser la del detalle.
  Galería, precios en pesos y enlaces a las redes.
- **Cursos**, en la cabecera: escribir en el buscador filtra según se escribe,
  sin pulsar nada, y «Solo gratuitos» se combina con la categoría y el nivel.
  Cada tarjeta dice «Gratis» o su precio antes de abrirla.

**3. El alta de emprendedor, de punta a punta.** «Publicar mi negocio», en la
portada:

| Paso | Qué enseñar |
|---|---|
| 1 · Cuenta | Enviar con la casilla sin marcar: el foco salta al campo que falla |
| 2 · Negocio | El contador de la descripción; a 79 caracteres no deja avanzar |
| 3 · Escaparate | Añadir dos productos, con el precio en pesos |
| 4 · Fotos | Subir dos o tres. La primera queda marcada como portada (B9) |

Al terminar aterriza en `/mi-negocio` **con el aviso de que está en revisión**.
El negocio nace `PENDIENTE` (B6): buscarlo en el directorio no lo encuentra, y su
identificador responde `404`. **No es un fallo, es la regla**, y conviene decirlo
antes de que lo pregunten.

**4. Aprobarlo, con el administrador.** Cerrar sesión y entrar en `/entrar` con
la cuenta del administrador: **aterriza directamente en el panel**, y la cabecera
enseña «Admin».

| Qué hacer | Qué se ve |
|---|---|
| Pestaña «Negocios» | La cola, la más antigua primero, con cuántos esperan |
| «Revisar» el negocio recién creado | **La vista previa**: su perfil tal como se publicará, con las fotos que aún nadie ha aprobado |
| «Rechazar» sin escribir nada | El motivo es obligatorio: el diálogo no se cierra |
| Cancelar y «Aprobar y publicar» | Vuelve a la cola con el aviso, y hay uno menos |
| Abrir Mailpit en <http://localhost:8025> | **El correo de aviso a la dueña** ya está ahí (I1-ter) |

Como el negocio todavía no estaba aprobado, **sus fotos se publican con él**: no
hay que aprobar nada más.

**5. Volver a la web y recargar el directorio.** El negocio recién creado ya
aparece, con su foto de portada, y las cifras de la portada han subido. Con eso
el círculo se cierra: se registró, se moderó y se publicó, todo desde el
navegador.

**6. Opinar sobre un negocio, con la cuenta de clienta.** Entrar en `/entrar` y
abrir un negocio que **no** sea el suyo:

| Qué hacer | Qué se ve |
|---|---|
| Mirar las opiniones sin sesión | Se leen igual: la lista es pública (C1) |
| Calificar con tres estrellas y escribir la reseña | Aparece arriba de la lista **sin recargar**, y la nota de la ficha cambia en la misma pantalla (C4) |
| Cambiarla a cinco estrellas | La fila se actualiza y queda marcada como «Editada» |
| Borrarla | Pide confirmación, y al aceptar vuelve el formulario de publicar |
| Abrir el negocio propio con la cuenta de la emprendedora | **No hay formulario**: el dueño no opina sobre lo suyo (A4) |
| «Denunciar» la opinión de otra persona | Se elige el motivo de una lista cerrada (C6). La opinión **sigue publicada** hasta que decida el administrador |

> Si era la única opinión del negocio, al borrarla la ficha vuelve a decir «Sin
> opiniones» y **no «0,0»**. Es C5, y merece nombrarlo: son dos cosas distintas.

**7. Recuperar una contraseña, viendo llegar el correo.** Es el recorrido que
cruza las dos mitades y el servidor de correo:

1. En `/entrar`, **«¿Olvidaste tu contraseña?»**.
2. Escribir el correo de la clienta. Sale siempre el mismo mensaje —«si ese
   correo tiene una cuenta, le acabamos de enviar un enlace»— **exista o no**:
   decir lo contrario convertiría el formulario en una lista de qué direcciones
   están registradas.
3. Abrir **Mailpit en <http://localhost:8025>**. El correo está ahí, con su
   enlace. Conviene enseñarlo en pantalla: es la prueba de que el correo sale de
   verdad y no es un `log`.
4. Abrir el enlace, escribir la contraseña nueva dos veces y guardar. Lleva al
   login con el aviso, y entra con ella.
5. **Volver a abrir el mismo enlace**: dice que ya no vale, sin enseñar el
   formulario. Sirve una sola vez y caduca a los 30 minutos.

> Probarlo con la cuenta de la clienta y no con la del administrador: si algo
> sale mal, la moderación del paso 4 sigue estando disponible.

**8. El resto del panel, con el administrador.** Cada pestaña es una cola de
trabajo, y todo lo que se decide queda escrito:

| Pestaña | Qué enseñar |
|---|---|
| Cambios | La foto subida al negocio ya aprobado espera aquí, con lo de ahora al lado de lo propuesto. Aprobarla la publica |
| Denuncias | La opinión denunciada en el paso 6, entera. Borrarla pide un motivo y **recalcula el promedio** del negocio |
| Usuarios | Suspender una cuenta pide confirmación; su dueño ya no puede entrar y le llega un correo. Reactivarla |
| Cursos | Crear uno de pago y publicado: sale al momento en el catálogo público. Pasarlo a borrador lo retira |
| Historial | Todo lo anterior, con quién lo hizo y cuándo, filtrable por fechas. Es de solo lectura (L) |

### Si algo falla en directo

| Síntoma | Qué es | Qué hacer |
|---|---|---|
| La web carga pero no hay datos | El backend todavía no responde | `docker compose logs -f backend` y esperar |
| Las fichas salen sin foto | Se subieron **después** de aprobar y esperan revisión | Aprobarlas en el panel, pestaña «Cambios» |
| El negocio nuevo no sale | Está `PENDIENTE`. Es lo correcto | Aprobarlo en el panel, pestaña «Negocios» |
| Un puerto ya está ocupado | Hay otro backend o otro Vite corriendo en el equipo | Pararlo, o `API_PORT=8081 docker compose up -d` |
| Tras un fallo de puerto, sigue sin ir | El contenedor quedó creado **sin red**: ni publica puertos ni resuelve `postgres` | `docker compose up -d --force-recreate backend`. Un `up -d` a secas solo lo arranca |
| Todas las fotos rotas | La base apunta a ficheros que no están en `backend/uploads/` | Recuperar el directorio; la base y el disco van por separado |
| El correo no llega a Mailpit | El backend arrancó sin el servicio de correo delante | `docker compose up -d mailpit` y repetir. La petición sigue devolviendo `200`: el fallo solo está en el log |

## Problemas al levantarlo

### En Windows

Funciona con **Docker Desktop** y no hace falta nada más: el JDK 25 va dentro de
la imagen del backend y Node dentro de la del frontend.

Lo único que hay que respetar es **no forzar `core.autocrlf`**. El repositorio
trae un `.gitattributes` que obliga a LF, porque `gradlew` se ejecuta dentro de
un contenedor Linux y con CRLF su intérprete pasa a ser `/bin/sh\r`: el backend
no arranca y el error no dice por qué. Con clonar normalmente basta; si el repo
ya estaba clonado de antes, `git rm -r --cached . && git checkout .` lo recoloca.

Los comandos del README valen en PowerShell, `cp` incluido; en `cmd`, el `.env` se
copia con `copy .env.example .env`. `HOST_UID` no hace falta: Docker Desktop no
traslada la propiedad de los ficheros del anfitrión. Y `./scripts/cifras.sh` pide bash, que viene con Git
Bash; no es parte de levantar el proyecto.

### Dos cosas que muerden al levantarlo

- **Los `container_name` son fijos**, así que el proyecto **no se puede levantar
  dos veces en la misma máquina**: el segundo falla con «container name already
  in use» y el mensaje no dice que ese sea el problema. Si hay otro checkout del
  repositorio, solo uno puede estar arriba.
- **Si un puerto está ocupado, el servicio no arranca** y el resto queda a
  medias. Los cinco son variables (`DB_PORT`, `API_PORT`, `WEB_PORT`,
  `MAILPIT_PORT`, `SMTP_PORT`): se cambian en un `.env` junto a
  `docker-compose.yml`. Y si tras el choque el contenedor sigue sin ir, quedó
  creado sin red: hace falta `docker compose up -d --force-recreate <servicio>`,
  porque `up -d` a secas solo lo arranca.

`MODERACION_AUTOMATICA` **ya no está puesta**: el proyecto modera de verdad. El
negocio nace `PENDIENTE` (B6), el administrador lo publica, y lo que su dueño
edite después espera en la cola de cambios (B2-bis) mientras el público sigue
viendo la versión aprobada.

Durante el desarrollo estuvo a `true` en `docker-compose.yml`, para no aprobar
cada negocio a mano. Si vuelve a hacer falta, se añade esa variable al servicio
`backend`: con ella el negocio nace publicado, sus fotos aprobadas y lo que el
dueño edita se publica al momento. **Pero entonces no hay nada que moderar**, y
el recorrido de la demostración pierde su paso más vistoso.

### Otro usuario en Linux

`HOST_UID` existe para que las fotos que suba el backend **no queden siendo de
root** dentro del repositorio. Con un usuario 1000 —lo normal en Linux— no hay
que tocar nada; si el tuyo es otro:

```bash
HOST_UID=$(id -u) HOST_GID=$(id -g) docker compose up -d
```

No uses `UID` a secas: en bash es una variable de solo lectura y la asignación
falla. Para no repetirlo, `echo "HOST_UID=$(id -u)" >> .env`.

## Estructura

```
backend/     API REST. Gradle, código, pruebas y colección de Postman
frontend/    Interfaz web. React + Vite + TypeScript
docs/        Documentación del proyecto
```

Dentro del backend:

```
backend/src/main/java/com/emprendehub
├── controller/   @RestController — rutas /api/v1/<recurso>
├── service/      @Service — lógica de negocio (aquí se mide la cobertura)
├── repository/   interfaces JpaRepository
├── model/        @Entity y enums del dominio
├── dto/          records Request/Response
├── exception/    excepciones de negocio y GlobalExceptionHandler
├── security/     SecurityConfig, JwtService, JwtAuthenticationFilter
└── config/       carga inicial de datos y beans de infraestructura
```

Cada paquete lleva un `package-info.java` que explica qué entra y qué no.

Y dentro del frontend:

```
frontend/src
├── types/        interfaces y uniones del dominio
├── api/          lo que habla con el backend — el único sitio con fetch
├── estado/       un contexto por dominio
├── componentes/  piezas reutilizables, con su módulo CSS al lado
├── paginas/      una por ruta
└── estilos/      tokens.css manda: ningún color se escribe fuera
```

La única dependencia de ejecución añadida a la plantilla de Vite es
`react-router-dom`; las de desarrollo, además de las de la plantilla, son Vitest y
jsdom, para las pruebas. El porqué está en
[docs/arquitectura.md](docs/arquitectura.md#el-frontend).

## Documentación

| Documento | Contenido |
|---|---|
| [docs/decisiones-dominio.md](docs/decisiones-dominio.md) | Las reglas de negocio y por qué son así |
| [docs/api.md](docs/api.md) | Contrato de los endpoints |
| [docs/arquitectura.md](docs/arquitectura.md) | Capas, stack, seguridad y decisiones técnicas |
| [docs/pruebas.md](docs/pruebas.md) | Los tres niveles de prueba y sus trampas |
| [docs/flujo-de-trabajo.md](docs/flujo-de-trabajo.md) | Ramas, commits, PRs y release |
| [docs/plan-de-entrega.md](docs/plan-de-entrega.md) | Los 14 incrementos del backend |
| [docs/diseno.md](docs/diseno.md) | Paleta, tipografía y accesibilidad del frontend |
| [docs/plan-de-entrega-frontend-fase-1.md](docs/plan-de-entrega-frontend-fase-1.md) | Los 13 incrementos de la fase 1 del frontend |
| [docs/plan-de-entrega-frontend-fase-2.md](docs/plan-de-entrega-frontend-fase-2.md) | Los 6 de la fase 2: opiniones y recuperar la contraseña |
