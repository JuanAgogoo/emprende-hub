# Arquitectura

Capas Spring clásicas, **las mismas que enseña el curso**: Controller, Service,
Repository y Entity.

Stack: **Java 25 + Spring Boot 4.1.x + Gradle + PostgreSQL + Spring Data JPA**.

Este documento explica **cómo está diseñado** el sistema. Cómo se prueba, y qué
trampas tiene hacerlo, está en [pruebas.md](pruebas.md). El contrato de los
endpoints, en [api.md](api.md).

## Por qué este stack

Se deriva del material de `plataformas_progamacion_empresarial/2026_2`, revisado
fichero por fichero. No es una elección de gusto: es la del curso.

- **Java 25 y Spring Boot 4.1.x** — versiones exactas del taller calificable.
- **Gradle** — es lo que usan los talleres (`./gradlew test`).
- **PostgreSQL** — el enunciado del proyecto prohíbe bases de datos en memoria.
  Los talleres usan H2, pero el proyecto es explícito en lo contrario.
- **Capas Controller/Service/Repository/Entity** — la rúbrica del taller calificable
  las nombra una por una y les asigna el 30%.
- **Lombok solo en entidades, records en DTOs.** La presentación desaconseja
  Lombok para `@Data` y `@Value`, pero su taller de CRUD sí lo usa en las
  entidades JPA con anotaciones granulares: `@Getter`, `@Setter` y
  `@NoArgsConstructor`. No es contradicción: un record no puede ser entidad JPA
  (necesita constructor vacío y mutabilidad), así que la regla real es **records
  donde se puede, Lombok granular donde no, `@Data` nunca**.

Regla que gobierna el resto de decisiones técnicas: **ante dos opciones, gana la
que el equipo pueda explicar.** La sustentación vale la mitad de la nota y la da
el equipo completo, así que parecerse al material del curso vale más que cualquier
elegancia arquitectónica.

## Estructura de paquetes

```
com.emprendehub
├── controller/     @RestController — rutas /api/<recurso>
├── service/        @Service — LÓGICA DE NEGOCIO (aquí se mide el 80%)
├── repository/     interfaces JpaRepository
├── model/          @Entity — entidades JPA
├── dto/            records: CreateXRequest, UpdateXRequest, XResponse
├── exception/      ResourceNotFoundException y GlobalExceptionHandler
├── security/       SecurityConfig, JwtService, JwtAuthenticationFilter
├── config/         CommandLineRunner de datos iniciales y beans de infraestructura
└── EmprendeHubApplication.java
```

El controlador no habla nunca con el repositorio: siempre a través del servicio.
El servicio no devuelve entidades hacia fuera, devuelve records de `dto`.

## Features de Java 25 que aplicamos

El curso dedica la semana 2 entera a ellos. Usarlos es la forma más directa de
demostrar el contenido visto:

- **`record`** para todos los DTOs, con *compact constructors* para validar.
- **`sealed interface` + pattern matching** para conjuntos cerrados **que no se
  persisten** y cuyas ramas llevan datos distintos. El caso vivo es
  `DecisionModeracion`: aprobar no lleva información y rechazar exige un motivo.
  El `switch` sobre ella es exhaustivo, así que añadir una tercera decisión
  rompería la compilación en todos los sitios que la tratan, en vez de fallar en
  ejecución.

  > **Lo que va a una columna se queda como enum.** JPA necesita `@Enumerated`
  > para persistir y una interfaz sellada no lo es, así que `EstadoNegocio`,
  > `EstadoCurso` y `Rol` son enums. La interfaz sellada es para el dominio que
  > solo vive en memoria.
- **Virtual threads**, con `spring.threads.virtual.enabled=true`.

## Seguridad

El curso sí cubre autenticación, en `procode/autenticacion/` del repositorio de
contenidos: una presentación de teoría y un taller guiado con código. Seguimos su
implementación al pie de la letra, porque es lo que el equipo podrá defender.

**Dependencias** (las mismas del taller):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.5'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.5'
```

El docente elige **jjwt** sobre `com.auth0:java-jwt` de forma explícita. No lo
cambiamos.

**Clases, con los nombres del taller** para que el código le resulte reconocible:

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `SecurityConfig` | `security` | El bean `SecurityFilterChain` con Lambda DSL |
| `JwtService` | `security` | `generateToken`, `extractUsername`, `isTokenValid` |
| `JwtAuthenticationFilter` | `security` | Lee la cabecera y puebla el `SecurityContextHolder` |
| `ApplicationConfig` | `security` | `PasswordEncoder`, `AuthenticationProvider` |
| `AuthController` | `controller` | `POST /api/v1/auth/register` y `/login` |
| `Usuario`, `Rol` | `model` | La entidad implementa `UserDetails` |

**Configuración**: `jwt.secret-key` y `jwt.expiration`, ambas desde variables de
entorno. El taller advierte contra la clave corta en duro y pide **256 bits mínimo
y expiración corta**.

**Reglas de la cadena de filtros**, tal como las plantea:

- `csrf` deshabilitado, porque la API es stateless y CSRF ataca cookies.
- `SessionCreationPolicy.STATELESS`.
- `/api/v1/auth/**` público; el resto autenticado, con `hasRole` por endpoint.

**Adaptaciones a nuestro dominio.** El taller usa los roles `USER` y `ADMIN`;
nosotros necesitamos `CLIENTE`, `EMPRENDEDOR` y `ADMIN` (sección A de las
decisiones de dominio), con el prefijo `ROLE_` que exige Spring Security.

**Fuera de alcance, y a propósito**: el taller recomienda *refresh token* como
buena práctica. No lo implementamos por tiempo. Es una omisión consciente y hay
que saber decirlo así en la sustentación, no fingir que no existe.

### Quién accede a qué

Se deriva de la sección A de las decisiones de dominio. Es lo que se traduce en
`requestMatchers` dentro de `SecurityConfig`:

| Rutas | Acceso |
|---|---|
| `POST /api/v1/auth/**` | Público |
| `GET` de directorio, perfiles, catálogos y cursos | **Público, sin sesión** — el directorio dice «explora sin registrarte» |
| Opiniones y consultas (`POST`, `PUT`, `DELETE`) | Autenticado, cualquier rol |
| Denunciar una opinión | Autenticado |
| Registrar un negocio | Autenticado (asciende a `EMPRENDEDOR`, A1-bis) |
| Panel del emprendedor: perfil, fotos, productos, buzón, métricas | `EMPRENDEDOR`, y solo sobre **su propio** negocio |
| Todo `/api/v1/admin/**` | `ADMIN` |

Dos reglas que el `requestMatchers` no cubre y van en el servicio:

- **Propiedad del recurso.** Que alguien tenga rol `EMPRENDEDOR` no le da acceso al
  negocio de otro. Se comprueba en el servicio, igual que B6.
- **Autenticación opcional.** El perfil público es accesible sin sesión, pero si
  la hay, H1 necesita saber quién es para no contar la visita del dueño. El
  endpoint es `permitAll` y lee el `SecurityContextHolder` solo si está poblado.

### Consecuencias sobre el resto del desarrollo

Tenerlas presentes evita rehacer trabajo:

- **La seguridad llega en el PR 5, pero el PR 4 ya tiene controladores.** En cuanto
  `spring-boot-starter-security` entra en el classpath, los `@WebMvcTest` escritos
  antes empiezan a devolver 401. El PR 5 tendrá que volver sobre las pruebas del
  PR 4: hay que contarlo con ello, no descubrirlo.
- **Hace falta `spring-security-test`** para anotar esas pruebas con
  `@WithMockUser` y fijar el rol esperado en cada caso.
- **Los datos sembrados llevan contraseñas hasheadas.** El admin de A3 nace en el
  `CommandLineRunner`, que debe pasar la contraseña por el `PasswordEncoder` antes
  de guardarla, nunca en texto plano. Lo mismo para los clientes de prueba del PR 14.
- **La colección de Postman necesita el login primero.** La petición de login debe
  guardar el token en una variable de entorno y el resto heredarlo, o el evaluador
  tendrá que copiarlo a mano en cada petición.

## Decisiones técnicas

Tomadas por el equipo de desarrollo. No requieren validación del cliente.

- **Un solo `Usuario` con rol**, no una tabla por perfil. Cliente y emprendedor
  comparten tabla porque ambos pueden opinar (A4) y porque un cliente puede
  registrar un negocio más adelante sin abrir una segunda cuenta.
- **Autenticación con JWT**, no sesión con cookie. La rúbrica exige probar cada
  endpoint con Postman o cURL: pegar un token en una cabecera es trivial, mantener
  una cookie de sesión en cURL no. Sin refresh token.
- **Calificación promedio desnormalizada** en el negocio, junto al número de
  opiniones. El directorio ordena y filtra por calificación. La recalcula el
  servicio al crear, editar, borrar y **moderar** una opinión — este último es el
  que se olvida.
- **Fotos por subida real** (multipart) a un directorio local configurable; en base
  de datos solo la ruta. La validación de tipo y tamaño (JPG/PNG, 5 MB, máximo 6)
  vive en el servicio, donde se prueba sin tocar disco.
- **El registro de negocio es una sola petición**, no cuatro. Los 4 pasos del
  prototipo son una división visual del formulario; el backend recibe un único
  payload y crea usuario y negocio en una transacción.
- **Todos los listados van paginados** (`Pageable` de Spring Data), aunque K1 diga
  que la escala es irrelevante. Devolver la tabla entera no es defendible.
- **El esquema lo genera Hibernate** con `ddl-auto: update`, y los datos iniciales
  se cargan con un `CommandLineRunner`. Es exactamente lo que hace el taller de
  CRUD del curso. Se descartó Flyway pese a ser mejor práctica: con dos días de
  plazo y un equipo que debe defender el código, parecerse al taller vale más que
  versionar el esquema.
- **Todas las rutas bajo `/api/v1`**, como el taller de autenticación (el de
  farmacia usaba `/api/` sin versión; manda el más reciente).
- **Validación de entrada con Bean Validation** en los records de DTO:
  `@NotBlank`, `@Positive`, `@Min`, y `@Valid` en el controlador. El taller de
  pruebas verifica que una petición inválida devuelve **400** y que el cuerpo del
  error tiene **una clave por campo inválido** (`$.precio`), no un mensaje suelto.
  El `GlobalExceptionHandler` debe producir esa forma.
- **Errores** con `ResourceNotFoundException` y un `GlobalExceptionHandler`
  anotado `@RestControllerAdvice`, en el
  mismo formato que el taller: `{"error": "Not Found", ...}`.
- **Zona horaria `America/Bogota` y moneda COP.** Importa para H1: el corte de
  «una visita por sesión y día» depende de dónde cae la medianoche.
- **El consentimiento de datos guarda versión de política** (`1.0`). Sin versión,
  la fecha de aceptación no dice a qué se aceptó.
- **Datos sembrados** como entregable, no como añadido: los 12 negocios y 8 cursos
  del prototipo, el admin, algunos clientes, opiniones e histórico de visitas. Sin
  ellos la demo con Postman no demuestra nada y la gráfica de H1 sale vacía.

### Decisiones tomadas durante la implementación

Las anteriores se tomaron antes de escribir código. Estas cinco salieron de
construirlo, y son las que más fácil se preguntan en una revisión.

- **El reloj es un bean `Clock`**, publicado en la zona `America/Bogota`. Con
  `LocalDate.now()` dentro del servicio, probar «los últimos siete días frente a
  los siete anteriores» dependería del día en que se ejecutara la prueba; con el
  reloj inyectado se le pasa un `Clock.fixed`. La zona no es cosmética: en UTC, el
  día de un negocio de Medellín cortaría a las siete de la tarde.
- **Los cuatro `CommandLineRunner` llevan `@Order`** (catálogos → admin → cursos →
  demostración). Spring no garantiza el orden entre ellos, y sin esto la siembra
  arranca antes que los catálogos y falla buscando una categoría que aún no existe.
- **Unicidad en el esquema para opiniones y denuncias, deliberadamente no para
  visitas.** Una opinión duplicada rompe C2 y hay que impedirla en la base de
  datos, porque el servicio no ve dos peticiones simultáneas. Una visita duplicada
  solo desvía un contador: poner ahí la restricción convertiría una carrera en un
  **500 en el perfil público**, y ninguna métrica justifica romper la página que
  la produce. La deduplicación de H1 es una heurística de conteo, no un invariante.
- **La ordenación del directorio es un conjunto cerrado** (`OrdenDirectorio`), no
  el `sort` de Spring Data. El cliente elige entre tres criterios y no entre
  cualquier columna de la tabla; un valor fuera de la lista devuelve 400. La
  traducción a columnas vive en el servicio, incluido el `NULLS LAST` que exige C5.
- **Cada notificación la crea el servicio donde ocurre el hecho**, no un proceso
  aparte que vigile la base de datos: `OpinionService`, `ConsultaService` y
  `ModeracionService` llaman a `NotificacionService`. Por el mismo motivo, el
  borrado de una opinión por moderación **se delega en `OpinionService`**: allí el
  recálculo del promedio es imposible de saltarse, y es el momento que más fácil
  se olvida porque no lo dispara su autor.

## Primer vertical

Cursos, por ser la entidad más simple: un CRUD sin relaciones, sin aprobación y
sin interacción de usuarios. Sirve para dejar montadas las capas, el arranque de
la base de datos y los tres niveles de prueba antes de entrar en Negocio, donde
está toda la complejidad.

## Entregables

Además del código, la entrega incluye:

- **Colección de Postman** con todas las peticiones y credenciales de prueba
  incluidas. La rúbrica evalúa cada endpoint vía Postman o cURL; sin credenciales
  sembradas y una colección lista, el evaluador prueba a ciegas.
- **Informe de cobertura de JaCoCo**, acotado a `service/**`, como evidencia del 80%.
- **Datos sembrados** por `CommandLineRunner`, condicionado a que la base esté vacía.
- **`docker-compose.yml`** que levanta PostgreSQL con un solo comando.
- **README** con los pasos para arrancar y ejecutar las pruebas.

