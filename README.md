# EmprendeHub

Portal de emprendimiento local. Proyecto de la asignatura Plataformas de
Programación Empresarial.

| Mitad | Dónde | Stack |
|---|---|---|
| **API REST** | [`backend/`](backend) | Java 25 · Spring Boot 4.1.1 · Gradle · PostgreSQL · Spring Data JPA · Spring Security |
| **Interfaz web** | [`frontend/`](frontend) | React · Vite · TypeScript |

## Arrancar

Hace falta Docker. **No hace falta tener instalado ni Gradle ni el JDK 25**: el
wrapper descarga Gradle y el toolchain descarga el JDK.

```bash
docker compose up -d              # levanta PostgreSQL en el puerto 5433
cd backend && ./gradlew bootRun   # arranca la API en http://localhost:8080
cd frontend && npm install        # solo la primera vez
npm run dev                       # la web en http://localhost:5173
```

El servidor de Vite hace de proxy hacia el 8080, así que **el backend tiene que
estar arriba** para que la web muestre algo. El detalle está en el
[README del frontend](frontend/README.md).

Para parar la base de datos:

```bash
docker compose down           # conserva los datos
docker compose down -v        # los borra
```

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

**El contrato completo, con los 59 endpoints y un recorrido de demostración de
punta a punta, está en [docs/api.md](docs/api.md).**

## Colección de Postman

`backend/postman/EmprendeHub.postman_collection.json`, con **71 peticiones que cubren los
59 endpoints**.

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

**429 pruebas en verde y 98,1% de cobertura sobre `service/**`**, muy por encima
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
