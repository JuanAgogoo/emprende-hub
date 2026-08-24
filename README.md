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

Al arrancar por primera vez se cargan los catálogos y la cuenta de administrador.
La carga es idempotente: reiniciar no duplica nada.

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
curl $A/estadisticas/portada
```

Subir una foto del negocio va por `multipart`, no por JSON:

```bash
curl -X POST $A/negocios/mio/fotos -H "Authorization: Bearer $TOKEN" \
  -F "archivo=@mi-local.jpg"
```

Las imágenes se guardan en `./uploads` —configurable con `FOTOS_DIR`— y se
descargan de `/fotos/{archivo}`, sin token.

**El contrato completo, con los 42 endpoints y un recorrido de demostración de
punta a punta, está en [docs/api.md](docs/api.md).**

## Pruebas

```bash
./gradlew build               # compila, prueba y verifica la cobertura
```

Las pruebas de repositorio levantan un PostgreSQL real con Testcontainers, así
que Docker tiene que estar corriendo.

- Informe de pruebas: `build/reports/tests/test/index.html`
- Informe de cobertura: `build/reports/jacoco/test/html/index.html`

**La cobertura de `service/**` tiene que quedarse en el 80% o más**: por debajo,
el build falla. Ejecutar una sola clase con `--tests` también lo hace fallar,
porque la cobertura se mide sobre el paquete entero; para eso usa `./gradlew build`.

## Configuración

Valores por defecto en `src/main/resources/application.yml`, todos sustituibles
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
