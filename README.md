# EmprendeHub — API REST

Backend del portal de emprendimiento local EmprendeHub. Proyecto de la asignatura
Plataformas de Programación Empresarial.

**Java 25 · Spring Boot 4.1.1 · Gradle · PostgreSQL · Spring Data JPA · Spring Security**

## Arrancar

Hace falta Docker. **No hace falta tener instalado ni Gradle ni el JDK 25**: el
wrapper descarga Gradle y el toolchain descarga el JDK.

```bash
docker compose up -d          # levanta PostgreSQL en el puerto 5433
./gradlew bootRun             # arranca la API en http://localhost:8080
```

Para parar la base de datos:

```bash
docker compose down           # conserva los datos
docker compose down -v        # los borra
```

Al arrancar por primera vez se siembran los datos de la demostración: los
catálogos, la cuenta de administrador, los 8 cursos y los 12 negocios del
prototipo con sus opiniones, su escaparate, su buzón y **dos meses de histórico
de visitas**. Sin ellos el directorio sale vacío y la gráfica del panel, plana.

**La carga es idempotente**: si ya hay datos no toca nada, así que reiniciar no
duplica. Para empezar de cero, `docker compose down -v`.

### Cuentas sembradas

| Cuenta | Correo | Contraseña | Para qué sirve |
|---|---|---|---|
| Administrador | `admin@emprendehub.co` | `admin12345` | Moderación y cursos |
| Emprendedora | `napolitana@emprendehub.co` | `contrasena123` | Panel con visitas, buzón y avisos |
| Emprendedora | `handmade@emprendehub.co` | `contrasena123` | Negocio **rechazado**: lee su motivo |
| Emprendedora | `yogaintegral@emprendehub.co` | `contrasena123` | Negocio **pendiente** de revisión |
| Clienta | `maria.garcia@gmail.com` | `contrasena123` | Opina, denuncia y escribe al buzón |

Los otros nueve emprendedores siguen el mismo patrón (`<negocio>@emprendehub.co`)
y los otros siete clientes son `nombre.apellido@gmail.com`. Todos con
`contrasena123`.

### Qué queda listo para enseñar

- **10 negocios publicados**, uno pendiente y uno rechazado con su motivo.
- **Destacados** con cuatro negocios: los que pasan de cinco opiniones (C7).
- Dos negocios **sin ninguna opinión**, que salen como «Nuevo» y sin calificación (C5).
- **Dos meses de visitas** por negocio, con la variación semanal y mensual en positivo.
- Consultas sin leer y avisos pendientes en los primeros buzones.

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
./gradlew build               # compila, prueba y verifica la cobertura
```

Las pruebas de repositorio levantan un PostgreSQL real con Testcontainers, así
que Docker tiene que estar corriendo.

**427 pruebas en verde y 98,4% de cobertura sobre `service/**`**, muy por encima
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
src/main/java/com/emprendehub
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
| [docs/plan-de-entrega.md](docs/plan-de-entrega.md) | Los 14 incrementos de la entrega |
