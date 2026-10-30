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

**Las tres de `@SpringBootTest` heredan de `AplicacionTestBase`**
(`EmprendeHubApplicationTests`, `SeguridadAccesoTest` y `SeguridadHttpRealTest`),
con el mismo patrón y **otro contenedor**. Hasta la entrega final conectaban con la
base de `application.yml`, en el 5433: en un clon limpio, sin esa base levantada,
caían 38 pruebas con *Unable to determine Dialect*. Ahora `./gradlew check` solo
necesita Docker.

No comparten contenedor con las de repositorio a propósito, y se probó: la
aplicación entera ejecuta sus cargas iniciales al arrancar —catálogos, admin y
cursos—, y las pruebas de repositorio, que crean sus propias ciudades, chocaban
con «Medellín ya existe». Con una sola base caían 80.

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

### `@InjectMocks` no sabe rellenar un primitivo

Añadir un parámetro primitivo al constructor de un servicio —pasó con el
interruptor `moderacionAutomatica`, un `boolean` leído de la configuración—
**tumba su clase de prueba entera**, no una prueba:

```
MockitoException: Cannot instantiate @InjectMocks field named 'service'!
Cause: the type 'FotoService' has no default constructor
```

Mockito solo sabe inyectar lo que puede simular, y un `boolean` no se simula. La
salida es dejar de usar `@InjectMocks` en esa clase y construir el servicio a
mano:

```java
private FotoService service;

@BeforeEach
void prepararConModeracion() {
    service = servicioCon(false);
}

private FotoService servicioCon(boolean moderacionAutomatica) {
    return new FotoService(fotoRepository, negocioRepository, cambioRepository,
            almacenamiento, moderacionAutomatica);
}
```

Sale más a cuenta de lo que parece: cada prueba puede elegir el valor, así que el
interruptor se prueba en sus dos posiciones sin duplicar la clase.

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

## Las pruebas del frontend

**Vitest con jsdom**, desde la entrega final. Hasta entonces el frontend no tenía
pruebas a propósito: la rúbrica de la entrega del frontend no las pedía. La de la
final sí, y dice qué tienen que cubrir: «el servicio de autenticación, el
interceptor que adjunta el token y al menos un servicio que consume la API en el
CRUD», con un comando que se ejecute «sin interfaz gráfica» y «sin quedarse
esperando cambios».

```bash
cd frontend
npm test           # vitest run: una pasada, sin modo vigilancia, y termina
```

**Por qué Vitest y jsdom.** El material del curso no trata las pruebas de
frontend. Vitest es el runner que acompaña a Vite —lee su misma configuración,
así que no hay que enseñarle TypeScript ni los alias— y es también el que usa
Angular por defecto desde su versión 21 en `ng test`. jsdom es el navegador
simulado que nombra el enunciado: da `localStorage`, `FormData` y `File` sin
abrir ninguna ventana. Son las dos únicas dependencias nuevas, y solo de
desarrollo. **Testing Library no entra**: lo exigido son servicios, no
componentes, y no hace falta pintar nada.

| Fichero | Qué prueba |
|---|---|
| `src/almacenSesion.test.ts` | Guardar, leer y borrar la sesión; descartar una corrupta o con otra forma |
| `src/api/auth.test.ts` | El servicio de autenticación: login, registro y el 200 sin cuerpo de la recuperación |
| `src/api/cliente.test.ts` | **El interceptor**: la cabecera `Authorization` solo con sesión y en peticiones con sesión, el 401 con token que cierra la sesión (y el 401 sin token, o el 403, que no), la red caída, los 502–504 y los errores por campo |
| `src/api/negocios.test.ts` | El CRUD del escaparate: listar, crear con su foto, editar y borrar, con su método, URL, cuerpo y cabecera |

Mismas convenciones que en el backend: nombres en español y el patrón AAA con
los comentarios `//arrange`, `//act` y `//assert` escritos tal cual. **`fetch` se
sustituye en cada prueba** por una respuesta preparada (`src/pruebas/respuestas.ts`),
así que no hace falta backend: lo que se prueba es lo que el frontend manda y cómo
lee lo que le devuelven. Que el backend responda de verdad lo comprueban `curl` y
el navegador.

Se comprobó que las pruebas vigilan algo: quitando la línea que pone la cabecera
`Authorization` en `cliente.ts` caen seis, justo las que dependen del token.

### El cierre de cada rebanada

Las pruebas no sustituyen el cierre que ya había:

```bash
cd frontend
npm test           # las pruebas, sin modo vigilancia
npm run build      # tipos y compilación, pruebas incluidas: 0 errores y 0 advertencias
npm run contraste  # las 21 combinaciones de los tokens contra WCAG 2.2
```

Más tres comprobaciones por `grep`, que son las reglas que un build en verde no
detecta:

```bash
grep -rn ': any\|as any' src/                                        # vacío
grep -rn 'fetch(' src --include=*.tsx | grep -v '^src/api/'          # vacío
grep -rnE '#[0-9a-fA-F]{3,8}|rgb\(' src --include=*.css | grep -v tokens.css   # una excepción conocida
```

La del color tiene **una excepción conocida**: el fondo del diálogo de
`ConfirmarBorrado.module.css` se escribe con `rgb()`. Los diálogos posteriores
sacan ese velo de un token con `color-mix()`.

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

### Las clases de CSS Modules hashean por fichero

Una regla que nombre una clase de **otro** módulo no falla: simplemente no se
aplica nunca, y eso no lo detecta ni el build ni el navegador señalando nada.

`.campo` vive en `paginas/Formulario.module.css`. Escribir en
`RegistroEmprendedor.module.css` una regla como `.campo .zonaFoto input { … }`
parece razonable y **no casa jamás**, porque ahí `.campo` es una clase local
distinta que nadie usa. Dentro de un mismo módulo sí funciona, y ahí lo que
muerde es la especificidad: `.campo label` le gana a una clase suelta, así que un
botón dibujado con una etiqueta hay que anidarlo —`.zonaFoto .botonArchivo`— o
sale con el tamaño y el color de las etiquetas de campo.

Los dos casos se comprueban igual, mirando el CSS compilado en vez de suponer:

```bash
npm run build
grep -o '\.[A-Za-z0-9_-]*zonaFoto[^{]*{[^}]*}' dist/assets/*.css
```

Si el selector no aparece con las dos clases hasheadas juntas, la regla no está
casando con lo que se cree.

### El contraste de los tintes no lo mide ningún script

`npm run contraste` comprueba los tokens puros. Una insignia con su color sobre
**su propio tinte** —`color-mix(in srgb, var(--color-aviso) 14%, var(--color-lienzo))`
de fondo y `--color-aviso` de texto— es otra combinación, y nadie la calcula. Con
texto del mismo color, cada señal aguanta el 4,5:1 hasta este tinte:

| Señal | Tinte máximo | Al 8 % | Al 14 % |
|---|---|---|---|
| `--color-exito` | 8 % | 4,51 | 4,14 |
| `--color-aviso` | 6 % | 4,42 | 4,09 |
| `--color-error` | 20 % | 5,66 | 5,08 |
| `--color-acento` | 14 % | 4,90 | 4,50 |

Lo nuevo usa el 6 %. **Cinco combinaciones anteriores incumplen el mínimo**, entre
4,03 y 4,27:1, y siguen pendientes:

| Fichero | Regla | Combinación |
|---|---|---|
| `componentes/EscaparateEditable.module.css` | `.agotado` | aviso sobre aviso al 15 % |
| `paginas/PerfilNegocio.module.css` | `.etiquetaAgotado` | aviso sobre aviso al 14 % |
| `componentes/Notificaciones.module.css` | `.opinion` | aviso sobre aviso al 14 % |
| `componentes/Notificaciones.module.css` | `.aprobado` | éxito sobre éxito al 12 % |
| `paginas/MiNegocio.module.css` | `.pendientes` | aviso sobre aviso al 14 % |

### Las fechas sueltas son medianoche UTC

`new Date('2026-08-30')` no es el 30 de agosto en Colombia: es medianoche UTC,
que aquí cae el día anterior desde las siete de la tarde. Una serie de días se
dibuja corrida uno, y un filtro «hasta el 6» se come las últimas horas del 6. Un
día suelto se construye por partes —`new Date(anio, mes - 1, dia)`—, como hacen
`formato.ts` y el historial de moderación. Las fechas con hora y zona, como
`fechaCreacion`, no tienen el problema.

### Una imagen con `width` y `height` necesita `height: auto`

Los atributos de un `<img>` reservan su hueco antes de que cargue, pero el alto
cuenta como un alto fijo: con `width: 100%` y `aspect-ratio` en el CSS, sin
`height: auto` la proporción se ignora. La foto principal del perfil midió 900 px
de alto durante semanas, y el build no tenía forma de verlo.

### El estado de error tarda en verse

Para ver el error de una vista se para el backend, pero el proxy de Vite tarda
unos 5 s por petición en rendirse con un 502. Una página que pide los catálogos y
luego los datos encadena dos esperas: **unos 10 s** antes de que aparezca el
mensaje. No es que falte el estado de error, es que todavía no ha llegado.

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

## Una columna `NOT NULL` no se añade sola a una tabla con filas

`ddl-auto: update` crea y amplía el esquema, pero **no migra datos**. Al añadir
`producto.foto` como obligatoria, Hibernate emite un `ALTER TABLE … ADD COLUMN …
NOT NULL` que PostgreSQL rechaza en cuanto la tabla tiene una sola fila, porque
no sabe qué poner en las que ya existen.

Con la base de desarrollo viva —y con negocios creados a mano que no se quieren
perder— la migración va aparte y en tres pasos:

```sql
ALTER TABLE producto ADD COLUMN IF NOT EXISTS foto VARCHAR(255);   -- permisiva
UPDATE producto p SET foto = (…)                     WHERE p.foto IS NULL;
DELETE FROM producto                                 WHERE foto IS NULL;
ALTER TABLE producto ALTER COLUMN foto SET NOT NULL;               -- y ahora sí
```

El tercer paso no es opcional: **las filas que no se puedan rellenar son datos
que la regla nueva ya no admite**, y hay que decidir qué pasa con ellas antes de
arrancar la aplicación, no después. Al hacerlo de verdad se perdieron seis
productos de seis negocios que no tenían ninguna foto que heredar.

Todo dentro de una transacción. Aquí falló una vez por el nombre de una columna
y el `ROLLBACK` evitó dejar la tabla a medias.

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
