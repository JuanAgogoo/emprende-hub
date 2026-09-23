# EmprendeHub

Portal de emprendimiento local. Proyecto de la asignatura Plataformas de
Programación Empresarial.

| Mitad | Dónde | Stack |
|---|---|---|
| **API REST** | [`backend/`](backend) | Java 25 · Spring Boot 4.1.1 · Gradle · PostgreSQL · Spring Data JPA · Spring Security |
| **Interfaz web** | [`frontend/`](frontend) | React · Vite · TypeScript |

## Arrancar

Hace falta **solo Docker**. Ni Gradle, ni el JDK 25, ni Node: van dentro de las
imágenes.

```bash
docker compose up -d
```

Eso levanta las tres piezas y deja la web en <http://localhost:5173> y la API en
<http://localhost:8080>. **La primera vez tarda varios minutos**: Gradle se
descarga sus dependencias. Las siguientes son segundos, porque la caché vive en
un volumen.

```bash
docker compose logs -f backend   # para ver cuándo termina de arrancar
```

> El backend tarda unos segundos más que la web. Hasta que responde, la portada
> enseña su mensaje de error: es lo esperado, no un fallo.

Las dos mitades corren **en modo desarrollo, con el código montado desde el
host**, no copiado a la imagen:

| Mitad | Al cambiar el código |
|---|---|
| Frontend | **Recarga en caliente.** Guardar el fichero y ya |
| Backend | `docker compose restart backend`. Java no tiene recarga en caliente |

### Sin Docker para las dos mitades

Si prefieres el bucle de siempre —`bootRun` y `npm run dev` en el host—, arranca
solo la base:

```bash
docker compose up -d postgres     # solo PostgreSQL, en el 5433
cd backend && ./gradlew bootRun   # la API en el 8080
cd frontend && npm install        # solo la primera vez
npm run dev                       # la web en el 5173
```

**No mezcles los dos modos a la vez**: los dos quieren el 8080 y el 5173, y el
segundo en arrancar falla. Para cambiar de uno a otro, `docker compose stop
backend frontend` primero.

### Parar y limpiar

```bash
docker compose stop           # para todo, sin borrar nada
docker compose down           # además retira los contenedores; los datos siguen
docker compose down -v        # borra también la base y las cachés
```

> `down -v` se lleva la base entera, y con ella los negocios creados a mano. Las
> fotos **no**: viven en `backend/uploads/`, en el disco. Después de un `down -v`
> la base referencia ficheros que ya no tienen dueño y sobran ahí.

### Puertos y usuario

| Variable | Por defecto | Para qué |
|---|---|---|
| `DB_PORT` | `5433` | El 5432 suele estar ocupado por otro PostgreSQL |
| `API_PORT` | `8080` | Cambiar si ya tienes un backend corriendo |
| `WEB_PORT` | `5173` | |
| `HOST_UID` / `HOST_GID` | `1000` | El usuario con el que corren los contenedores |
| `MODERACION_AUTOMATICA` | `false` | **Provisional.** A `true`, los negocios nacen publicados y sus fotos aprobadas |

`HOST_UID` existe para que las fotos que suba el backend **no queden siendo de
root** dentro del repositorio. Con un usuario 1000 —lo normal en Linux— no hay
que tocar nada; si el tuyo es otro:

```bash
HOST_UID=$(id -u) HOST_GID=$(id -g) docker compose up -d
```

No uses `UID` a secas: en bash es una variable de solo lectura y la asignación
falla. Para no repetirlo, `echo "HOST_UID=$(id -u)" >> .env`.

`MODERACION_AUTOMATICA` está puesta a `true` en `docker-compose.yml` **mientras se
construye**, para no tener que aprobar cada negocio a mano. Se quita esa línea
para volver a la moderación de verdad, que es lo que hay que enseñar el día de la
sustentación: el negocio nace `PENDIENTE` (B6) y el administrador lo publica.

Al arrancar por primera vez se siembra **lo que no tiene sentido escribir a
mano**: los catálogos —12 categorías, las ciudades del Valle de Aburrá con sus
barrios y los catálogos de formación—, la cuenta de administrador y los 8 cursos.
La carga es idempotente: si ya hay datos no toca nada, así que reiniciar no
duplica. Para empezar de cero, `docker compose down -v`.

**Los negocios no se siembran.** Antes había doce generados por código y se
retiraron: la vitrina es parte del entregable y un catálogo escrito por un bucle
se nota. Se crean uno a uno, con sus fotos y sus precios, desde la colección de
Postman o desde el registro de emprendedor del frontend.

Consecuencia al arrancar de cero: el directorio sale vacío y la portada muestra
ceros con la calificación media nula. **Es lo correcto, no un fallo** — las
cifras se calculan (H4) y todavía no hay nada que contar.

### La única cuenta sembrada

| Cuenta | Correo | Contraseña | Para qué sirve |
|---|---|---|---|
| Administrador | `admin@emprendehub.co` | `admin12345` | Moderación y cursos |

Las demás se crean al registrarse. La carpeta **1 · Acceso y datos de partida**
de la colección de Postman las crea en orden —una clienta, una emprendedora y su
negocio— y guarda cada token en su variable.

## El recorrido de la sustentación

El guion de punta a punta, con las dos mitades arriba. Son diez minutos y se
puede ensayar entero antes del día.

### Antes: preparar la vitrina

**No hay siembra de negocios**, así que una base recién creada enseña la portada
a ceros y el directorio vacío. Eso es correcto —las cifras se calculan (H4)—,
pero no es lo que se quiere enseñar. Hay que crear los negocios a mano, y
conviene hacerlo el día antes y no el mismo día.

Con **siete u ocho negocios** ya se ve un portal lleno. Lo que tienen que cubrir
entre todos, porque es lo que la interfaz enseña:

| Hace falta | Para que se vea |
|---|---|
| Categorías distintas | La rejilla «Explora por categoría» de la portada lleva a alguna parte |
| Al menos dos ciudades, y una con barrio | El filtro de barrio se repuebla al cambiar de ciudad |
| Los tres niveles de precio | El filtro de precio deja fuera a alguien |
| **Uno con 5 opiniones o más** | Sin eso no hay destacados: es la regla C7 |
| Uno sin ninguna opinión | Se ve «Sin opiniones» y no «0,0», que no es lo mismo (C5) |
| Uno sin fotos | Sale el marcador con la inicial, no una imagen rota |
| Uno dejado en `PENDIENTE` | Hay algo que aprobar en directo el día de la sustentación |

La forma rápida de crearlos es la carpeta **1 · Acceso y datos de partida** de la
colección de Postman, y después repetir su petición de alta cambiando los datos.

> **El orden importa, y esta es la trampa que más tiempo ha costado.** Hay que
> **crear el negocio, subirle las fotos y aprobarlo al final**. Al revés no: una
> foto subida a un negocio **ya aprobado** entra pendiente de revisión (B2), el
> público no la ve, y el directorio devuelve `fotoPrincipal` nula. Las fichas
> salen sin imagen y parece un fallo del frontend cuando es la regla funcionando.

### Durante: el guion

**1. Levantar el proyecto.** Un comando y una terminal:

```bash
docker compose up -d
docker compose logs -f backend    # esperar a «Started EmprendeHubApplication»
```

> Hacerlo **antes** de que empiece la sustentación, no delante de nadie: aunque
> las imágenes ya estén construidas, el backend tarda unos segundos en responder
> y la portada enseña su mensaje de error mientras tanto.

**2. La parte pública, sin iniciar sesión.** En <http://localhost:5173>:

- La portada, con las cuatro cifras **calculadas** y los destacados.
- Buscar desde la portada: lleva al directorio con el texto ya puesto.
- En el directorio, filtrar por categoría, cambiar de ciudad —el selector de
  barrio se repuebla solo— y ordenar por calificación.
- Entrar a un negocio: la foto de la tarjeta crece hasta ser la del detalle.
  Galería, precios en pesos y enlaces a las redes.

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

**4. Aprobarlo, con el administrador.** Desde la carpeta **5 · Administración**
de Postman, o con `curl`:

```bash
A=http://localhost:8080/api/v1

TA=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"admin@emprendehub.co","contrasena":"admin12345"}' | jq -r .token)

curl -s -H "Authorization: Bearer $TA" $A/admin/moderacion/negocios-pendientes
curl -X PATCH $A/admin/moderacion/negocios/<id>/aprobar -H "Authorization: Bearer $TA"
```

Como el negocio todavía no estaba aprobado, **sus fotos se publican con él**: no
hay que aprobar nada más.

**5. Volver a la web y recargar el directorio.** El negocio recién creado ya
aparece, con su foto de portada, y las cifras de la portada han subido. Con eso
el círculo se cierra: se registró desde el navegador, se moderó desde la API y se
publicó.

### Si algo falla en directo

| Síntoma | Qué es | Qué hacer |
|---|---|---|
| La web carga pero no hay datos | El backend todavía no responde | `docker compose logs -f backend` y esperar |
| Las fichas salen sin foto | Se subieron **después** de aprobar y esperan revisión | Aprobar el cambio pendiente |
| El negocio nuevo no sale | Está `PENDIENTE`. Es lo correcto | Aprobarlo con el administrador |
| Un puerto ya está ocupado | Hay otro backend o otro Vite corriendo en el equipo | Pararlo, o `API_PORT=8081 docker compose up -d` |
| Tras un fallo de puerto, sigue sin ir | El contenedor quedó creado **sin red**: ni publica puertos ni resuelve `postgres` | `docker compose up -d --force-recreate backend`. Un `up -d` a secas solo lo arranca |
| Todas las fotos rotas | La base apunta a ficheros que no están en `backend/uploads/` | Recuperar el directorio; la base y el disco van por separado |
| Caen ~30 pruebas del backend | Se paró el contenedor de la base | `docker compose up -d postgres` y repetir |

## Probar la API

Casi todo exige un token. Se obtiene entrando:

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

**El contrato completo, con los 61 endpoints y un recorrido de demostración de
punta a punta, está en [docs/api.md](docs/api.md).**

## Colección de Postman

`backend/postman/EmprendeHub.postman_collection.json`, con **72 peticiones que cubren los
61 endpoints**.

1. Importarla en Postman (*Import → File*).
2. Ejecutar las cuatro primeras peticiones de **1 · Acceso**. Cada una guarda su
   token en una variable de la colección.
3. El resto de carpetas ya heredan el token que les toca: no hay que copiar nada
   a mano.

> **La colección cambia datos, así que no es idempotente.** Aprueba el negocio
> pendiente, publica el curso en borrador y renombra un negocio. Recorrerla dos
> veces seguidas devuelve errores en la segunda pasada —«ya estaba publicado»,
> «solo se resuelven negocios pendientes»— que son la regla de negocio haciendo
> su trabajo, no un fallo. Para volver al estado inicial:
>
> ```bash
> docker compose down -v && docker compose up -d && ./gradlew bootRun
> ```

| Carpeta | Qué contiene |
|---|---|
| 1 · Acceso | Los cuatro logins que dejan los tokens listos |
| 2 · Público | Todo lo que se explora sin registrarse |
| 3 · Cliente | Opinar, denunciar y escribir al buzón |
| 4 · Panel del emprendedor | Negocio, galería, escaparate, buzón, visitas y avisos |
| 5 · Administración | Moderación, denuncias y cursos |
| 6 · Seguridad | Los casos que tienen que fallar: 401, 403 y 400 |

La variable `base` apunta a `http://localhost:8080/api/v1`.

## Pruebas

```bash
cd backend
./gradlew build               # compila, prueba y verifica la cobertura
```

Las pruebas de repositorio levantan un PostgreSQL real con Testcontainers, así
que Docker tiene que estar corriendo.

**453 pruebas en verde y 98,2% de cobertura sobre `service/**`**, muy por encima
del 80% que exige la rúbrica. Repartidas en los tres niveles del taller:

| Nivel | Herramienta | Qué prueba |
|---|---|---|
| Unitario | Mockito | Los 15 servicios, con el repositorio simulado |
| Repositorio | `@DataJpaTest` + Testcontainers | Las consultas contra PostgreSQL real |
| Controlador | `@WebMvcTest` + MockMvc | Rutas, códigos y forma del JSON |

Además, `SeguridadAccesoTest` y `SeguridadHttpRealTest` verifican la matriz de
acceso completa, esta última con el servidor levantado.

- Informe de pruebas: `build/reports/tests/test/index.html`
- Informe de cobertura: `build/reports/jacoco/test/html/index.html`

**La cobertura de `service/**` tiene que quedarse en el 80% o más**: por debajo,
el build falla. Ejecutar una sola clase con `--tests` también lo hace fallar,
porque la cobertura se mide sobre el paquete entero; para eso usa `cd backend && ./gradlew build`.

### Verificar el frontend

No tiene pruebas automáticas a propósito: nadie las pidió, y las vistas cambian
en cada incremento. Lo que sí tiene es un cierre obligatorio.

```bash
cd frontend
npm run build      # tipos y compilación: 0 errores y 0 advertencias
npm run contraste  # las 21 combinaciones de la paleta contra WCAG 2.2
```

`npm run contraste` lee los colores de `src/estilos/tokens.css` y falla si alguna
combinación baja de su mínimo: 4,5:1 en texto y 3:1 en bordes de control. Si se
añade un token, se añade su fila.

Y lo que ningún script sustituye: **abrirlo en el navegador**, mirarlo a 360 px
de ancho y recorrerlo con el tabulador. El detalle está en
[docs/pruebas.md](docs/pruebas.md#el-frontend-no-tiene-pruebas-automáticas-y-es-a-propósito).

## Configuración

Valores por defecto en `backend/src/main/resources/application.yml`, todos sustituibles
por variables de entorno:

| Variable | Por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/emprendehub` |
| `DB_USER` / `DB_PASSWORD` | `emprendehub` |
| `PORT` | `8080` |
| `JWT_SECRET` | Clave de desarrollo, sustituir en cualquier despliegue |
| `JWT_EXPIRATION` | `3600000` (una hora) |
| `ADMIN_CORREO` / `ADMIN_CONTRASENA` | `admin@emprendehub.co` / `admin12345` |

El esquema lo genera Hibernate a partir de las entidades (`ddl-auto: update`), y
los datos iniciales los carga un `CommandLineRunner` al arrancar.

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

La única dependencia añadida a la plantilla de Vite es `react-router-dom`. El
porqué está en [docs/arquitectura.md](docs/arquitectura.md#el-frontend).

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
