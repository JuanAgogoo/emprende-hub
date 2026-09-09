# Pruebas

Cómo se prueba este proyecto y qué muerde al hacerlo.

Las dos mitades se verifican de forma distinta: el backend con **tres niveles de
prueba automática y un umbral de cobertura**, y el frontend **comprobando la
pantalla**, porque ahí los fallos que importan no los ve un compilador. Las dos
secciones están abajo.

Los tres niveles y sus convenciones salen del taller de la semana 4 del curso.
Las trampas son cicatrices: cada una costó tiempo real en esta entrega.

El diseño del sistema está en [arquitectura.md](arquitectura.md); aquí solo se
habla de cómo verificarlo.

## Pruebas

Los tres niveles del taller de la semana 4, con sus mismas convenciones: patrón
**AAA** (Arrange-Act-Assert) y `@DisplayName` descriptivo en español.

| Nivel | Herramienta | Qué prueba |
|---|---|---|
| Unitario | `@ExtendWith(MockitoExtension)`, `@Mock`, `@InjectMocks` | El servicio, con el repositorio mockeado |
| Slice de repositorio | `@DataJpaTest` con Testcontainers | Que el mapeo JPA y las consultas funcionan |
| Slice de controlador | `@WebMvcTest`, `MockMvc`, `@MockitoBean` | Rutas, códigos HTTP y forma del JSON |

### Diferencias con el material del curso al escribir pruebas

El material está escrito para Spring Boot 3 y estas tres cosas cambiaron en la 4.
Copiar los imports del taller tal cual **no compila**:

| Anotación | Paquete en el curso (Boot 3) | Paquete real (Boot 4) |
|---|---|---|
| `@WebMvcTest` | `...test.autoconfigure.web.servlet` | `org.springframework.boot.webmvc.test.autoconfigure` |
| `@DataJpaTest` | `...test.autoconfigure.orm.jpa` | `org.springframework.boot.data.jpa.test.autoconfigure` |
| `TestEntityManager` | `...test.autoconfigure.orm.jpa` | `org.springframework.boot.jpa.test.autoconfigure` |
| `@AutoConfigureTestDatabase` | `...test.autoconfigure.jdbc` | `org.springframework.boot.jdbc.test.autoconfigure` |

`MockMvc` y `@MockitoBean` no se movieron: siguen en `spring-test`.

**Testcontainers 2.x renombró todos sus módulos** con el prefijo
`testcontainers-`: `org.testcontainers:testcontainers-postgresql`, no
`org.testcontainers:postgresql`. La versión la gestiona Spring Boot; fijar el BOM
de Testcontainers a mano lo degrada a la rama 1.x y provoca el error *«client
version 1.32 is too old»* contra Docker 29.

**`@AutoConfigureTestDatabase(replace = NONE)` es obligatorio** en las pruebas de
repositorio: sin él, Spring sustituye la base de datos por una en memoria, que es
justo lo que el enunciado prohíbe.

**El contenedor se arranca a mano, no con `@Container`.** Esa anotación detiene el
contenedor al terminar cada clase de prueba, así que la segunda clase que herede
de `PostgresTestBase` lo encontraría parado. Se usa el patrón de contenedor único:
un bloque `static` que llama a `start()` una vez para toda la ejecución.

**Los parámetros de texto opcionales necesitan `CAST` en las consultas.** Cuando un
parámetro llega nulo, PostgreSQL lo infiere como `bytea` y revienta con
*function lower(bytea) does not exist*. Hay que escribir
`LOWER(CONCAT('%', CAST(:texto AS string), '%'))`. Los parámetros de tipo enum no
tienen este problema.

Sobre el nivel unitario, la teoría del curso es tajante: **no debe tocar ninguna
base de datos, ni siquiera H2**. Los argumentos que da son velocidad (milisegundos,
no minutos), aislamiento (si falla, el fallo está en el servicio y en ningún otro
sitio) y acoplamiento. Una prueba de servicio que necesite una base de datos está
mal escrita.

**Primera desviación: base de datos de pruebas.** El taller usa H2 para `@DataJpaTest`; aquí
va PostgreSQL sobre Testcontainers, porque el enunciado del proyecto prohíbe bases
de datos en memoria. El equipo no lo habrá visto en clase, así que hay que
explicarlo aparte de cara a la sustentación.

## Cobertura

El enunciado exige 80% «en la lógica de negocio» sin definir qué es. Nuestra
definición, escrita aquí para poder defenderla:

> Lógica de negocio = el paquete `service/**`.

Es exactamente lo que la semana 4 prueba con Mockito, así que la definición no es
nuestra: es la del curso. Se mide con **JaCoCo**, que el material no cubre — el
taller solo mira `build/reports/tests/test/index.html`. Es una adición nuestra y
conviene saberlo al sustentar.

El umbral está **activo desde el PR 4** y colgado de la tarea `check`: un
`cd backend && ./gradlew build` que baje del 80% en `service/**` falla. Es más barato escribir
la prueba en su incremento que recuperar cobertura al final.

## El frontend no tiene pruebas automáticas, y es a propósito

Nadie las pidió, y añadir Vitest y Testing Library significaría dos dependencias,
una configuración y un patrón más que defender, para cubrir vistas que cambian en
cada incremento. Lo que sí hay es un cierre obligatorio en cada rebanada:

```bash
cd frontend
npm run build      # tipos y compilación: 0 errores y 0 advertencias
npm run contraste  # las 21 combinaciones de la paleta contra WCAG 2.2
```

Más cuatro comprobaciones por `grep`, que son las reglas que un build en verde no
detecta:

```bash
grep -rn ': any\|as any' src/                                        # vacío
grep -rn 'fetch(' src --include=*.tsx | grep -v '^src/api/'          # vacío
grep -rnE '#[0-9a-fA-F]{3,8}|rgb\(' src --include=*.css | grep -v tokens.css   # vacío
```

Y la que ningún script sustituye: **abrir el navegador**. En el backend la regla
es no fiarse de las pruebas y llamar al endpoint con `curl`; aquí es mirar la
pantalla, a 360 px de ancho y recorriéndola con el tabulador. Los tres estados de
cada vista —cargando, vacío y error— se ven provocándolos: el vacío con un filtro
imposible, el error parando el backend.

### La comprobación de enlaces

Un `<Link>` a una ruta que no existe compila, pasa la CI y solo se nota al
pulsar. Aparecieron **tres en siete incrementos**, dos de ellos vivieron cuatro.
Antes de cerrar una rebanada, cotejar los `to=` del código contra las rutas
declaradas en `App.tsx`.

## Lo que depende del reloj se inyecta

`LocalDate.now()` escrito dentro de un servicio hace que su prueba dependa del
día en que se ejecute. La agregación de visitas del PR 13 compara «los últimos
siete días con los siete anteriores», así que el reloj es un `Clock` inyectado
—`ConfiguracionReloj` lo publica en la zona `America/Bogota`— y las pruebas le
pasan un `Clock.fixed`. La zona no es decorativa: con UTC, el corte de «un día»
de un negocio de Medellín caería a las siete de la tarde.

## Trampas de la cadena de seguridad

Tres cosas que costaron tiempo y que conviene no repetir en los PR siguientes.

**`/error` tiene que estar abierto en la cadena de filtros.** Cuando la
autorización rechaza una petición, Spring hace un reenvío interno a `/error`
para construir el cuerpo de la respuesta. Ese reenvío vuelve a pasar por la
cadena, ya sin la cabecera del token, cae en `anyRequest().authenticated()` y el
401 acaba pisando al 403 que la autorización había decidido. El síntoma es
desconcertante: los registros dicen «Responding with 403» y el cliente recibe un
401.

**MockMvc no ejecuta ese reenvío**, así que el fallo anterior pasó desapercibido
a noventa y tres pruebas y solo apareció llamando a la API con curl. Por eso
existe `SeguridadHttpRealTest`, que levanta el servidor y usa el cliente HTTP del
JDK. Cualquier comportamiento que dependa del ciclo de error necesita una prueba
de ese tipo.

**`@WebMvcTest` carga los `WebMvcConfigurer` pero no los `@Component`.** Una
clase de configuración web que dependa de un componente propio deja sin contexto
a **todas** las pruebas de controlador del proyecto a la vez, con un
`NoSuchBeanDefinitionException` que no menciona el slice por ninguna parte. Pasó
al publicar el directorio de fotos en `/fotos/**`: la solución es que la
configuración tome la propiedad (`@Value`) en lugar del bean.

**`@WebMvcTest` sí carga `SecurityConfig`**, aunque no cargue el resto de
configuraciones. Sin sus dependencias el contexto ni siquiera arranca, y con
ellas la política por defecto de Spring Security haría que hasta los endpoints
públicos respondieran 401. De ahí `ControllerTestBase`, que importa la
configuración real para que cada prueba se ejecute contra las mismas reglas que
producción.

**Autorización y comportamiento del controlador se prueban por separado.** Las
clases de controlador desactivan los filtros con `addFilters = false` y se
centran en códigos de estado y contrato de errores; que cada ruta exija el rol
correcto se verifica en `SeguridadAccesoTest`. Mezclarlo obliga a repetir la
autenticación en cada prueba y esconde lo que cada una comprueba.

**Nota sobre paquetes de Spring Boot 4**: `TestRestTemplate` se movió a
`org.springframework.boot.resttestclient` y su bean no se autoconfigura, por eso
las pruebas de HTTP real usan `java.net.http.HttpClient`.

## Jackson descarta en silencio lo que el DTO no declara

Enviar un campo que el `record` no tiene **no da error**: Jackson lo ignora y la
petición responde `201` como si todo hubiera ido bien. Pasó con las redes
sociales en el alta de emprendedor —`instagram` viajaba dentro del negocio, que
no lo declara— y el dato se perdía sin una sola señal.

No lo detecta ninguna prueba unitaria que simule el servicio, porque el problema
está en el mapeo del JSON. **Al probar un endpoint con `curl`, mirar el cuerpo de
la respuesta y no solo el código de estado**, y comprobar después que el dato se
guardó de verdad.

## Filtros opcionales en PostgreSQL

Ha aparecido dos veces y aparecerá más: **un parámetro que puede llegar a null
necesita un `CAST` explícito en la consulta.** PostgreSQL no infiere su tipo y
responde con errores desconcertantes:

| Tipo del parámetro | Error sin `CAST` |
|---|---|
| `String` | `function lower(bytea) does not exist` |
| `Instant` | `could not determine data type of parameter $1` |

Los parámetros de tipo enum no se ven afectados.

**Toda consulta con filtros opcionales necesita su prueba de repositorio.** El
fallo del log de moderación se coló hasta la API real precisamente porque esa
prueba no existía; las de servicio, con el repositorio simulado, no pueden verlo.
