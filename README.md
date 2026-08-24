# EmprendeHub — API REST

Backend del portal de emprendimiento local EmprendeHub. Proyecto de la asignatura
Plataformas de Programación Empresarial.

**Java 25 · Spring Boot 4.1.1 · Gradle · PostgreSQL · Spring Data JPA**

## Arrancar

Hace falta Docker. No hace falta tener instalado ni Gradle ni el JDK 25: el
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

## Pruebas

```bash
./gradlew test                # todas las pruebas
./gradlew build               # compila y prueba
```

El informe queda en `build/reports/tests/test/index.html`.

## Configuración

Valores por defecto en `src/main/resources/application.yml`, todos sustituibles
por variables de entorno:

| Variable | Por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/emprendehub` |
| `DB_USER` | `emprendehub` |
| `DB_PASSWORD` | `emprendehub` |
| `PORT` | `8080` |

El esquema lo genera Hibernate a partir de las entidades (`ddl-auto: update`), y
los datos iniciales los carga un `CommandLineRunner` al arrancar.

## Estructura

```
src/main/java/com/emprendehub
├── controller/   @RestController — rutas /api/<recurso>
├── service/      @Service — lógica de negocio (aquí se mide la cobertura)
├── repository/   interfaces JpaRepository
├── model/        @Entity
├── dto/          records Request/Response
├── exception/    excepciones de negocio y @RestControllerAdvice
└── config/       seguridad, JWT y beans de infraestructura
```

Cada paquete lleva un `package-info.java` que explica qué entra y qué no.

## Documentación

| Documento | Contenido |
|---|---|
| [docs/decisiones-dominio.md](docs/decisiones-dominio.md) | Las reglas de negocio y por qué son así |
| [docs/arquitectura.md](docs/arquitectura.md) | Capas, stack y decisiones técnicas |
| [docs/flujo-de-trabajo.md](docs/flujo-de-trabajo.md) | Ramas, commits, PRs y release |
| [docs/plan-de-entrega.md](docs/plan-de-entrega.md) | Los 13 incrementos de la entrega |
