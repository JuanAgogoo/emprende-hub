# Plan de entrega — Frontend, fase 1

Trece incrementos, cada uno una rama y un Pull Request, igual que en
[plan-de-entrega.md](plan-de-entrega.md). El orden es de dependencia: cada rama
sale de `main` con el anterior ya fusionado.

La numeración es del frontend y empieza de nuevo en 1. Los números de PR en
GitHub siguen la cuenta del repositorio, que va por el #18.

Once incrementos son de frontend y **dos son de backend**: el PR 1 retira la
siembra de demostración y el PR 10 añade el registro de emprendedor, que no
existía. El resto consume el contrato ya entregado sin tocarlo.

Cada incremento de frontend es una **rebanada vertical**: tipo, llamada a la API,
estado y vista, algo que se abre en el navegador y se ve funcionando. Por eso
cada uno lleva su **hito de verificación** escrito como algo observable, y no se
da por terminado sin comprobarlo con los ojos. Las decisiones técnicas del
frontend están en [arquitectura.md](arquitectura.md#el-frontend), cómo se
verifica en [pruebas.md](pruebas.md), y las reglas visuales —paleta, tipografía,
mobile first y accesibilidad— en [diseno.md](diseno.md).

Cada incremento indica el mensaje de commit exacto, para pegarlo sin redactarlo.

---

## Alcance de la fase 1

Lo que se construye:

| | |
|---|---|
| Portada | Las cuatro cifras, destacados y buscador |
| Directorio | Búsqueda, filtros, ordenación y paginación |
| Perfil público | Galería, escaparate con precios y redes |
| Acceso | Un único login que deriva por rol |
| Registro de cliente | Formulario corto |
| Registro de emprendedor | Endpoint nuevo y asistente con la **carga inicial** |
| Páginas informativas | Tratamiento de datos e información personal |

Lo que **no** entra, y conviene tenerlo por escrito antes de que alguien lo dé
por supuesto:

- **El dashboard de gestión.** Editar el negocio, el buzón de consultas, las
  métricas de visitas y las notificaciones son fase 2. Aquí solo hay una vista
  de **solo lectura** del negocio recién creado.
- **El panel de administración**: moderación, denuncias, gestión de cursos.
- **Los cursos**, en cualquier forma.
- **Las opiniones.** El perfil público muestra la calificación y el número de
  opiniones porque vienen en la misma respuesta, pero no se listan, ni se
  escriben, ni se denuncian. **Leerlas, escribirlas y gestionarlas entraron
  después**, en los PR 1–3 de
  [la fase 2](plan-de-entrega-frontend-fase-2.md); denunciarlas sigue fuera.
- **Recuperar la contraseña.** No existe en el backend (I1) y no se dibuja un
  enlace que no lleve a ninguna parte. **Entró después**, en los PR 4–5 de la
  fase 2, que reabren I1 con I1-bis y devuelven el enlace al login.
- **Ninguna dependencia más allá de `react-router-dom`**: ni Redux, ni
  react-query, ni axios, ni Zod, ni React Hook Form, ni Tailwind, ni i18n, ni
  Storybook, ni pruebas de extremo a extremo. El porqué, en
  [arquitectura.md](arquitectura.md#qué-no-entra-y-por-qué).

La fase consume **15 endpoints**: 14 de los 59 entregados, más el que añade el
PR 10.

---

## Decisiones que gobiernan el plan

Ocho cosas se resolvieron antes de escribir los incrementos. Están aquí porque
cambian lo que hay que construir, y porque son las preguntas que va a hacer quien
lea el plan.

### 1. Los datos de demostración se crean a mano

`CargaInicialDemo` se retira en el PR 1. Los negocios, sus fotos y sus productos
se crean **uno a uno y con detalle**, no por siembra: doce negocios generados por
código se notan, y esta vez la vitrina es el entregable.

Consecuencia que hay que asumir, y no es menor: **desde el PR 1 la base arranca
vacía de negocios**. Quedan los catálogos, los cursos y la cuenta de
administrador, que sí se siembran. Hasta que exista el asistente de registro —PR
11— la única forma de crear un negocio es la colección de Postman, que ya tiene
las peticiones y el token en una variable.

Por eso los hitos de los PR 3 a 6 dicen «un negocio creado a mano» donde antes
habrían dicho «un negocio de la siembra». Es un paso más al verificar, a cambio
de que lo que se enseñe el día de la sustentación sea real.

### 2. El registro de emprendedor es un endpoint nuevo

El alta de emprendedor **no es el alta de cliente con más campos**. El cliente da
correo y contraseña; el emprendedor da además el negocio entero: nombre,
descripción, contacto, ubicación, nivel de precio, redes y escaparate. Que eso
entre por `POST /auth/registro` sería forzar un formulario dentro de otro.

**Las redes van en su propio campo `redes`, no dentro del negocio.** El alta de
A1-bis tampoco las pide, así que meterlas ahí se ignoraba en silencio: la
petición devolvía `201` y el dato se perdía. Costó un rato descubrirlo.

Se añade `POST /api/v1/auth/registro-emprendedor`, que crea cuenta y negocio
**en una sola transacción** y devuelve el token con el rol ya en `EMPRENDEDOR`:

```
POST /api/v1/auth/registro-emprendedor
Content-Type: application/json

{ "nombre": "...", "correo": "...", "contrasena": "...",
  "negocio": { "nombre": "...", "descripcion": "...", "telefono": "3...",
               "categoriaId": 1, "ciudadId": 2, "barrioId": 5,
               "nivelPrecio": "MEDIO" },
  "redes":     { "instagram": "...", "linkedin": "..." },
  "productos": [ { "nombre": "...", "precio": 45000, "disponible": true } ] }

201 -> { token, tipo, expiraEnMillis, nombre, correo, rol: "EMPRENDEDOR", negocioId }
```

Una transacción y no tres llamadas encadenadas porque el fallo intermedio es el
caso que peor se explica: una cuenta creada, un negocio a medias y alguien que ya
no puede repetir el registro porque su correo está cogido. O entra todo, o no
entra nada.

### 3. Las fotos no van en ese endpoint

Es la única parte del negocio que se queda fuera, y por una razón concreta: son
binarios. Meterlas dentro obliga a un `multipart/form-data` que mezcle una parte
JSON con N ficheros —se puede hacer en Spring, pero es la pieza más difícil de
defender de todo el proyecto— y a duplicar la validación de tipo, tamaño y máximo
que `FotoService` ya hace y ya tiene probada.

Van justo después, por `POST /api/v1/negocios/mio/fotos`, con la sesión que
acaba de devolver el registro. Para quien lo usa es el paso siguiente del mismo
asistente; por dentro son dos peticiones en lugar de una.

**Si esta decisión se revierte**, el cambio queda acotado al PR 10 y al PR 12: el
resto del plan no depende de ella.

### 4. A1-bis se mantiene: hay dos caminos y es a propósito

`POST /negocios` sigue existiendo. Un cliente que ya tiene cuenta y opiniones
puede ascender a emprendedor sin abrir una segunda cuenta, que es exactamente lo
que dice A1-bis.

Son dos entradas al mismo estado y hay que saber decir por qué: quien llega
sabiendo que tiene un negocio se registra como emprendedor de una vez; quien se
anima después no pierde su cuenta. Retirar `POST /negocios` habría obligado a
reescribir A1-bis, sus pruebas y la colección de Postman, para dejar peor al
usuario que ya estaba dentro.

`docs/decisiones-dominio.md` recoge la razón en el PR 10, junto a A1 y A1-bis.

### 5. El negocio nace PENDIENTE, y eso se ve

Al terminar el asistente el negocio está en revisión (B6): no sale en el
directorio y su identificador responde `404` hasta que el administrador lo
apruebe. La vista de cierre lo dice con todas las letras. **No es un fallo del
frontend**, y hay que saber explicarlo en la sustentación.

Las fotos siguen la misma lógica: el dueño las ve con su estado, el público solo
las aprobadas.

### 6. El tratamiento de datos no se persiste

Casilla obligatoria que bloquea el envío, más una página con el texto. **No se
guarda en ninguna parte**: no hay campo en `Usuario` y no se abre por esto un PR
de backend. El texto es genérico y no pretende tener validez legal.

Se exige en los dos registros. El requisito lo ató al emprendedor; ponerlo
también en el de cliente cuesta una línea y evita explicar por qué uno sí y el
otro no.

### 7. CORS no existe en el backend: se resuelve con el proxy de Vite

El backend entregado **no configura CORS por ninguna parte**. Un `fetch` desde
`localhost:5173` a `localhost:8080` lo bloquearía el navegador.

Se resuelve en el PR 2 con `server.proxy` de Vite, que es la herramienta que Vite
trae para esto, y que hay que proxear **para `/api/v1` y también para `/fotos`**,
o las imágenes no cargan. En desarrollo todo sale del mismo origen.

**Límite conocido:** si en la sustentación el frontend se sirve compilado desde
un origen distinto al del backend, el proxy ya no está y hace falta añadir CORS
en `SecurityConfig`. Es un ajuste de una clase, pero más vale decidirlo antes del
día de la entrega que ese día.

### 8. Las ramas llevan `frontend-`, los commits llevan el dominio

`semantic-release` está configurado sobre el repositorio entero y lee el tipo del
commit, no la carpeta. Los mensajes siguen la convención del backend
(`feat(portada): …`) y son las **ramas** las que llevan el prefijo `frontend-`,
salvo las de los PR 1 y 10, que son de backend. Conviene saber que un `feat` del
frontend sube la versión del proyecto completo: es un repositorio con una sola
versión, y así se queda.

---

## Cómo se lee el estado

Cada incremento y cada fase llevan su marca. Se pone **al terminar el trabajo y
verificarlo**, y se completa con el número del PR cuando se fusiona.

| Marca | Significa |
|---|---|
| **TERMINADO** | Construido y verificado. Lleva el número de su PR si ya está fusionado |
| **EN CURSO** | Rama abierta, trabajo sin cerrar |
| *(sin marca)* | Sin empezar |

Una fase se marca terminada cuando lo están **todos** sus incrementos.

---

## Fase 0 — Preparar el terreno (PR 1–2) · **TERMINADA**

### PR 1 · `chore/retirar-siembra-demo` — backend · **TERMINADO**

> Fusionado en [#20](https://github.com/JuanAgogoo/emprende-hub/pull/20), junto
> con el PR 2. Verificado: 429 pruebas en verde, cobertura de `service/**` al
> 98,1%, y el ciclo completo probado con `curl` —registro, negocio pendiente,
> rechazo con motivo, reenvío, aprobación y aparición en el directorio—.
>
> **Además de lo previsto** hubo que rehacer la primera carpeta de la colección
> de Postman: tres peticiones entraban con cuentas sembradas y de ellas heredaban
> el token casi todas las demás.

Se retira `CargaInicialDemo` y con él los 12 negocios, las 20 cuentas, las
opiniones, las consultas y los 60 días de visitas. **Se quedan** los otros tres
cargadores, que siguen siendo necesarios: catálogos, administrador y cursos, en
ese orden y con sus `@Order`.

Hay que revisar lo que dependía de esos datos y no dejarlo mintiendo:

- Las pruebas que contaran con negocios sembrados. Las de servicio y repositorio
  crean los suyos, pero **hay que ejecutar el build completo para saberlo**, no
  suponerlo.
- La colección de Postman, si alguna petición usa un identificador sembrado.
- El README y `docs/api.md`, donde el recorrido de demostración parte de datos
  que ya no existen: pasa a empezar creando el negocio.
- La sección «Datos de la demostración» de `api.md`.

```
chore: retirar la siembra de demostración y dejar solo los catálogos
```

> **Hito:** con `docker compose down -v` y la aplicación arrancada de cero,
> `GET /directorio` devuelve una página vacía y `GET /estadisticas/portada`
> devuelve ceros con `calificacionPromedio` nulo —que es lo correcto, no un
> fallo—. `cd backend && ./gradlew build` en verde, con la cobertura intacta.

### PR 2 · `chore/frontend-andamiaje` · **TERMINADO**

> Fusionado en [#20](https://github.com/JuanAgogoo/emprende-hub/pull/20).
> Verificado: `npm run build` sin errores ni advertencias, `npm run contraste`
> 21 de 21 —y comprobado que falla con código 1 al romper un token a propósito—,
> y el proxy respondiendo en `/api/v1` y `/fotos` contra el backend real.
>
> **Queda pendiente la revisión visual**: no había navegador disponible, así que
> el ancho de 360px y el recorrido con el tabulador no se comprobaron.
>
> **Corrección sobre lo planeado:** los jobs de CI no se filtran por `paths`.

Proyecto creado con `npm create vite@latest frontend -- --template react-ts`.
`tsconfig.json` con `strict`, `noImplicitAny`, `strictNullChecks` y
`forceConsistentCasingInFileNames`. Única dependencia añadida:
**`react-router-dom`**. Carpetas `types/`, `api/`, `estado/`, `componentes/`,
`paginas/`.

`api/cliente.ts` concentra la URL base, la cabecera `Authorization` y la lectura
del error del `GlobalExceptionHandler` —que trae `message`, y una clave por campo
cuando la validación falla—. Es el único sitio del proyecto donde se escribe
`fetch`. Proxy de Vite para `/api/v1` y `/fotos`, según la decisión 7.

**Incluye el sistema de diseño de [diseno.md](diseno.md) entero**, porque es lo
que impide que cada incremento invente sus propios valores: `estilos/tokens.css`
con los 17 tokens de color, la escala tipográfica y el espaciado; Manrope
Variable autoalojada en `public/fuentes/` —un `.woff2` de 25 KB que cubre de 400
a 800—; el reinicio de estilos; y `scripts/contraste.mjs` con su
`npm run contraste`, que es la prueba de visibilidad que verifica las 21
combinaciones.

**Incluye además el job de CI del frontend**, sin el cual los once incrementos
que vienen pasarían la verificación sin que nadie los compile. `ci.yml` tiene hoy
un solo job con `working-directory: backend`: se le añade otro con `npm ci`,
`npm run contraste` y `npm run build`.

**Los dos jobs corren siempre, sin filtrar por `paths`.** Filtrarlos era la idea
inicial y no funciona: `release` depende de ellos con `needs`, y un job saltado
por `paths` cuenta como no satisfecho, así que no se publicaría ninguna versión.
Esperar dos minutos de más en un PR de frontend sale más barato que perseguir una
release que no salió.

Layout con cabecera y pie, dos rutas y `<Link>`. README del frontend con el
arranque.

```
chore(frontend): crear el andamiaje con React, TypeScript y el sistema de diseño
```

> **Hito:** `npm run dev` levanta en el 5173. La cabecera y el pie se ven, y
> pasar de una ruta a otra no recarga la página. `npm run build` termina con 0
> errores y 0 advertencias, y `npm run contraste` con 21 de 21. A 360px de ancho
> no hay desbordamiento horizontal. En la CI aparecen los tres jobs: backend,
> frontend y release.

---

## Fase 1 — Vitrina pública (PR 3–5) · **TERMINADA**

Los tres se verifican contra negocios creados a mano con Postman, según la
decisión 1. **El estado vacío deja de ser un caso raro y pasa a ser el primero
que se ve**, así que se construye antes que el lleno, no después.

### PR 3 · `feat/portada` · **TERMINADO**

> Fusionado en [#21](https://github.com/JuanAgogoo/emprende-hub/pull/21).
> Verificado con datos reales creados por la API —7 negocios, 3 destacados y
> calificación media de 4,8—: `npm run build` sin errores ni advertencias,
> `npm run contraste` 21 de 21, y las fotos llegando por el proxy con `200
> image/png`. **Falta la revisión visual en el navegador**: no hay ninguno
> disponible en el entorno, y esa comprobación queda del lado de Pedro.
>
> **Aviso para los PR 4 y 5, que sale de construir este.** Una foto subida a un
> negocio **ya aprobado** entra pendiente de revisión (B2) y el público no la ve:
> `fotoPrincipal` llega nula hasta que el administrador aprueba el cambio. Al
> preparar datos para ver una pantalla llena, el orden es **crear, subir las
> fotos y después aprobar**, o hay que aprobar también el cambio pendiente. No
> es un fallo del frontend y cuesta un rato descubrirlo.
La portada: hero, la barra de cuatro cifras de `GET /estadisticas/portada` y los
destacados de `GET /directorio/destacados`, con los siete bloques que fija
[diseno.md](diseno.md#la-portada).

Aparece la unión discriminada `EstadoCarga<T>` con `CARGANDO`, `EXITO` y `ERROR`,
que gobierna todas las vistas que cargan datos a partir de aquí. Componente
`TarjetaNegocio` con su `key={negocio.id}`, reutilizado luego por el directorio.

Dos cosas que con la base vacía dejan de ser hipotéticas: `calificacionPromedio`
**llega nula mientras no haya ninguna opinión**, y se resuelve con `??` y nunca
con `||`, o un promedio de `0` legítimo se convertiría en el texto por defecto; y
**sin destacados no se enseña una rejilla vacía**, sino el bloque con su mensaje.

```
feat(portada): mostrar las estadísticas y los negocios destacados
```

> **Hito:** con la base recién creada, las cuatro cifras salen a cero y el bloque
> de destacados explica que todavía no hay ninguno. Tras crear dos negocios y
> aprobarlos con Postman, las cifras suben y las tarjetas aparecen. Parar el
> backend y recargar muestra el mensaje de error, no una pantalla en blanco.

### PR 4 · `feat/directorio` · **TERMINADO** · [#22](https://github.com/JuanAgogoo/emprende-hub/pull/22)

> Entregado en la rama `feat/frontend-directorio`. Verificado contra el backend
> con siete negocios: filtro por categoría, por ciudad y por barrio; búsqueda con
> acentos —`texto=Panadería` devuelve su negocio, no un 400—; orden por nombre y
> por calificación, con los negocios sin nota al final (C5); paginación con
> `size=3` moviéndose entre tres páginas; y un `orden` inválido devolviendo 400
> con la lista de valores admitidos.
>
> **Desviación de `diseno.md`, ya corregida allí:** los filtros en móvil no son
> una hoja modal sino un `<details>` plegable. La hoja pedía manejar a mano el
> foco atrapado, Escape y el fondo — JavaScript que nadie pidió para un
> desplegable.
>
> **Falta la revisión visual en el navegador**, que sigue sin haber en el
> entorno.
Listado con los seis filtros combinables (`texto`, `categoriaId`, `ciudadId`,
`barrioId`, `calificacionMinima`, `nivelPrecio`), la ordenación de lista cerrada
(`CALIFICACION`, `NOMBRE`, `RECIENTES`) y la paginación. En móvil los filtros son
una hoja que sube desde abajo, no una barra lateral encogida.

`api/catalogos.ts` trae las 12 categorías y las ciudades **con sus barrios
anidados**, así que el selector de barrio se repuebla desde la ciudad elegida sin
una segunda petición. El buscador de la portada entra aquí con el texto puesto.

Dos avisos del contrato: el directorio **ignora `sort`** y ordena con su propio
parámetro; y la consulta se arma con `URLSearchParams`, que codifica los acentos
—un `texto=café` sin codificar devuelve `400`, la misma trampa que ya mordió con
`curl`—.

```
feat(directorio): añadir búsqueda, filtros, ordenación y paginación de negocios
```

> **Hito:** filtrar por categoría reduce la lista; cambiar de ciudad repuebla los
> barrios; buscar «café» funciona; y una combinación sin resultados muestra el
> estado vacío con un botón para limpiar los filtros. Con más de 12 negocios
> creados, la página 2 trae otros distintos.

### PR 5 · `feat/perfil-publico` · **TERMINADO** · [#23](https://github.com/JuanAgogoo/emprende-hub/pull/23)

> Entregado en la rama `feat/frontend-perfil-publico`. Verificado contra un
> negocio preparado con tres fotos, tres productos —uno agotado— y las dos redes:
> la galería cambia de foto, los precios salen como `$ 12.000`, y **un negocio
> pendiente devuelve 404 y no 403** (B6), comprobado creando uno a propósito.
>
> La transición entre la tarjeta y el perfil lleva un `view-transition-name`
> **único por negocio**: con un nombre compartido, el navegador no sabría cuál de
> las tarjetas del listado está creciendo.
>
> **Falta la revisión visual en el navegador**, en este y en los tres anteriores.
> Es la única comprobación del plan que no se ha podido hacer en ningún
> incremento: el entorno no tiene navegador.

> **Con este incremento la parte pública queda cerrada** y se recorre entera sin
> iniciar sesión: portada, directorio con filtros y perfil de negocio.
Perfil de `GET /directorio/{id}`: galería de fotos aprobadas, escaparate con
precios formateados en pesos con `Intl.NumberFormat`, disponibilidad, teléfono y
enlaces a Instagram y LinkedIn cuando existen. La calificación y el número de
opiniones se muestran; el listado de opiniones no es de esta fase —lo añade el
PR 1 de [la fase 2](plan-de-entrega-frontend-fase-2.md)—.

Un identificador que no existe —o un negocio pendiente, que responde `404` y no
`403` a propósito (B6)— lleva a una página de «no encontrado» y no a un error
crudo.

Aquí entra la transición de vista entre la tarjeta y el perfil: la foto crece
hasta ser la del detalle, con `view-transition-name` y su `@supports`.

```
feat(directorio): añadir el perfil público del negocio con galería y escaparate
```

> **Hito:** desde el directorio se entra a un negocio y se ven sus fotos, sus
> productos con el precio en pesos y sus redes. Un negocio sin fotos muestra el
> marcador de posición, no una imagen rota. Pedir un `id` inventado muestra «no
> encontrado». Con esto la parte pública está cerrada y se recorre entera sin
> iniciar sesión, que es lo que promete la portada.

---

## Fase 2 — Acceso (PR 6–9) · **TERMINADA**

### PR 6 · `feat/login` · **TERMINADO** · [#24](https://github.com/JuanAgogoo/emprende-hub/pull/24)

> Entregado en la rama `feat/frontend-login`. Verificado contra el backend: los
> tres roles entran y la respuesta trae `rol`, una contraseña mala devuelve 401
> con «Credenciales incorrectas», y el token abre `/negocios/mio` mientras que
> sin él responde 401.
>
> **Pendiente para el PR 8:** el login no enlaza al registro porque esa página
> todavía no existe, y el plan prohíbe dibujar enlaces muertos. Ese incremento
> tiene que volver aquí a añadirlo.
>
> La persistencia quedó en `almacenSesion.ts`, no dentro del contexto, porque
> `api/cliente.ts` también necesita el token para la cabecera. Con una sola clave
> y un solo módulo no hay dos copias que se desincronicen.
>
> **Falta la revisión visual en el navegador.**
Una sola pantalla de acceso. `POST /api/v1/auth/login` devuelve el token **y el
rol**, así que no hay que descodificar nada para saber a quién se ha autenticado:
la respuesta decide a dónde va cada quien.

`estado/SesionContext.tsx` es el primer contexto por dominio: expone la sesión y
las acciones `entrar` y `salir`, **nunca el `setState`**. El token se guarda en
`localStorage` y se recupera al arrancar, para que recargar no eche a nadie.
`api/cliente.ts` lo añade como `Authorization: Bearer …` en su único sitio.

La cabecera cambia con la sesión iniciada. Un `401` dice «credenciales
incorrectas» sin sacar a nadie del formulario.

```
feat(auth): añadir el inicio de sesión con derivación por rol
```

> **Hito:** entrar con una cuenta creada por Postman deja su nombre en la
> cabecera; recargar la página mantiene la sesión; «Salir» la limpia; y una
> contraseña mala muestra el mensaje sin vaciar el correo escrito.

### PR 7 · `feat/paginas-informativas` · **TERMINADO** · [#25](https://github.com/JuanAgogoo/emprende-hub/pull/25)

> Entregado en la rama `feat/frontend-paginas-informativas`. El texto describe lo
> que la aplicación hace de verdad —cada fila de la tabla es un campo que el
> backend guarda—, no relleno genérico.
>
> **Cerró dos enlaces muertos que el pie arrastraba desde el PR 2** y destapó un
> tercero: la portada ofrecía «Publicar mi negocio» hacia una ruta que no existe
> hasta el PR 11. Retirado, y **el PR 11 tiene que volver a ponerlo**.
>
> De ahí sale una comprobación que conviene repetir en cada incremento: cotejar
> todos los `to=` del código contra las rutas declaradas en `App.tsx`. Los dos
> del pie llevaban cuatro incrementos sin que nadie lo notara.
>
> **Falta la revisión visual en el navegador.**
Tratamiento de datos e información personal, con texto genérico. Enlazadas desde
el pie y, en los PR siguientes, desde la casilla de los dos registros. Son
públicas y se leen sin sesión.

Van antes que los registros a propósito: la casilla que exige aceptarlas necesita
un enlace que ya funcione.

```
feat(legal): añadir las páginas de tratamiento de datos e información personal
```

> **Hito:** los dos enlaces del pie abren su página, se leen sin sesión y se
> vuelve atrás sin perder nada. El texto no baja de 17px ni se sale a 360px.

### PR 8 · `feat/registro-cliente` · **TERMINADO** · [#26](https://github.com/JuanAgogoo/emprende-hub/pull/26)

> Entregado en la rama `feat/frontend-registro-cliente`. Verificado contra el
> backend: alta válida con `201` y sesión abierta, correo repetido con `400` y
> «Ya existe una cuenta con ese correo», contraseña corta con su clave por campo,
> y la cuenta nueva entrando por el login.
>
> **El backend devuelve dos formas de error distintas** y el formulario trata
> cada una: la validación de forma trae una clave por campo, y la regla de
> negocio solo `message`. Como aquí el único conflicto posible es el correo ya
> usado, ese campo se marca además de decirlo arriba.
>
> **Devuelve al login su enlace al registro**, que el PR 6 tuvo que quitar por no
> dejar enlaces muertos.
>
> **Falta la revisión visual en el navegador.**
El registro corto de A1: nombre, correo y contraseña de mínimo 8 caracteres, más
la casilla de tratamiento de datos. Formulario controlado con una función pura
`validar()` fuera del componente; los mensajes aparecen **solo tras intentar
enviar**, que es el `markAllAsTouched()` del curso traducido.

`POST /api/v1/auth/registro` devuelve `201` con el token, así que quien se
registra queda dentro sin pasar por el login.

Un correo repetido responde `400` con el mensaje del backend: se muestra tal
cual, junto al campo, no en un cartel genérico.

```
feat(auth): añadir el registro de clientes con su validación
```

> **Hito:** un registro válido termina con el nombre en la cabecera; repetir el
> correo devuelve el mensaje del backend junto al campo; y sin marcar la casilla
> el envío no sale. El foco salta al primer campo inválido.

### PR 9 · `feat/mi-negocio` · **TERMINADO** · [#27](https://github.com/JuanAgogoo/emprende-hub/pull/27)

> Entregado en la rama `feat/frontend-mi-negocio`. Verificado contra el backend
> en los cuatro casos: negocio aprobado, pendiente, rechazado —con el motivo que
> escribió quien administra— y una cuenta sin negocio, que responde `404` y se
> explica en vez de tratarse como un fallo.
>
> **`GET /negocios/mio` no trae la galería ni el escaparate**, aunque este plan
> daba por hecho que sí. Van por `/negocios/mio/fotos` y `/negocios/mio/productos`,
> así que la vista hace tres llamadas. La tabla de endpoints queda corregida.
>
> Aparece la primera ruta protegida por rol. Es una comodidad de la interfaz, no
> una medida de seguridad: quien mande la petición a mano sigue topándose con el
> backend, que es donde se comprueba de verdad.
>
> **Devuelve la derivación por rol que el PR 6 dejó a medias**: el emprendedor
> aterriza en su negocio.
>
> **Falta la revisión visual en el navegador.**

> **Con este incremento la fase 2 queda cerrada.** Falta la fase 3, que es el
> alta de emprendedor de punta a punta.
La vista de aterrizaje, **de solo lectura**, con `GET /api/v1/negocios/mio`: los
datos del negocio, su galería, su escaparate y su estado. Si está `PENDIENTE`, lo
explica; si está `RECHAZADO`, muestra el motivo que escribió el administrador
(B1). Ruta `/mi-negocio`, para `EMPRENDEDOR`.

**Esto no es el dashboard.** No edita, no sube, no borra. Es el destino que el
asistente necesita para no terminar en el vacío, y el germen de la fase 2.

**Incluye además** cambiar la derivación del PR 6: hasta ahora todos los roles
caían en la portada, y a partir de aquí `EMPRENDEDOR` cae en `/mi-negocio`.
Volver sobre el PR 6 está contemplado, igual que el PR 5 del backend volvió sobre
el 4; no es un descuido.

```
feat(negocios): añadir la vista del negocio propio y la derivación del emprendedor
```

> **Hito:** entrar con un emprendedor creado por Postman aterriza directo en su
> negocio, con su estado a la vista. Entrar con un cliente sigue llevando a la
> portada, y escribir `/mi-negocio` a mano no le enseña nada.

---

## Fase 3 — Alta del emprendedor (PR 10–12) · **TERMINADA**

Los tres incrementos de las decisiones 2 y 3. El primero es de backend y los dos
siguientes lo consumen: no hay asistente sin endpoint, ni fotos sin negocio.

### PR 10 · `feat/registro-emprendedor` — backend · **TERMINADO**

> Entregado en la rama `feat/registro-emprendedor`. **444 pruebas en verde**
> —quince nuevas— con la cobertura de `service/**` intacta en el 98,1%.
>
> **La transaccionalidad está comprobada, no supuesta**: con un barrio de otra
> ciudad el alta falla *después* de guardar el usuario, y el login con ese correo
> devuelve 401 —no quedó creado—; reintentar con el mismo correo funciona. Era
> justo el escenario que motivaba la decisión 2.
>
> **Las redes hacían falta y se habían quedado fuera.** `RegistrarNegocioRequest`
> no las lleva —tienen su propio endpoint—, así que enviarlas dentro del negocio
> se ignoraba en silencio. Entran como campo propio `redes`, reutilizando la
> validación de dominio de B8.
>
> **No hizo falta tocar `SecurityConfig`**: `/api/v1/auth/**` ya era público.
>
> Los errores anidados nombran su ruta (`negocio.descripcion`), que es lo que el
> PR 11 necesita para devolver al paso que falló.

Añade `POST /api/v1/auth/registro-emprendedor` con la forma de la decisión 2:
cuenta, negocio y productos en una transacción.

La regla que lo gobierna es que **el ajuste imita al código que ya existe
alrededor**: no es la ocasión de introducir un patrón mejor.

- DTO `RegistroEmprendedorRequest` como `record`, con `@Valid` en cascada sobre
  el negocio anidado y sobre cada producto. **Se reutilizan las validaciones ya
  escritas**: mínimo 80 caracteres de descripción, teléfono `3XXXXXXXXX` o
  `60XXXXXXXX` (G8), nivel de precio de lista cerrada (G4), barrio que debe
  pertenecer a la ciudad, precio no negativo y los dominios de Instagram y
  LinkedIn (B8). No se reescribe ninguna regla: se compone lo que hay.
- El método vive en `AuthService`, es `@Transactional` y orquesta los servicios
  que ya existen. Sin capa nueva, sin interfaz con una sola implementación, sin
  mapeador.
- El negocio nace `PENDIENTE` y el usuario nace `EMPRENDEDOR`, sin pasar por
  `CLIENTE`. La ruta es pública en `SecurityConfig`, como las otras dos de
  `/auth`.
- **Los tres niveles de prueba**, con el patrón AAA y los comentarios
  `//arrange`, `//act`, `//assert`, `//verify` escritos tal cual. Casos que no
  pueden faltar: correo repetido, negocio inválido con la cuenta ya validada
  —que **no debe dejar usuario creado**—, y lista de productos vacía, que es
  válida.
- `docs/api.md` y `docs/decisiones-dominio.md` **en el mismo PR**: el contrato
  del endpoint, y la razón de los dos caminos junto a A1-bis. La colección de
  Postman suma su petición.
- Las cifras envejecen mal y hay tres sitios que las repiten: **60 endpoints** en
  `api.md`, en el README y en la tabla final de `plan-de-entrega.md`, más el
  recuento de pruebas.

```
feat(auth): añadir el registro de emprendedores con su negocio en una transacción
```

> **Hito:** `cd backend && ./gradlew build` en verde con la cobertura de
> `service/**` por encima del 80%. Con la aplicación arrancada, un `curl` al
> endpoint devuelve `201` con rol `EMPRENDEDOR`; el mismo correo repetido
> devuelve `400`; y una descripción de 79 caracteres devuelve `400` **sin haber
> creado la cuenta** —comprobado intentando el login después—.

### PR 11 · `feat/registro-emprendedor` — **frontend** · **TERMINADO**

> Entregado en la rama `feat/frontend-registro-emprendedor`. Verificado contra el
> backend con `curl`, enviando el mismo cuerpo que arma el asistente: `201` con
> rol `EMPRENDEDOR` y `negocioId`, el negocio `PENDIENTE` con su barrio y su
> Instagram guardados, los dos productos en el escaparate, y el identificador
> devolviendo `404` en el directorio hasta que se apruebe (B6).
>
> **Las cinco formas del error se comprobaron una a una**, porque de ellas
> depende a qué paso vuelve el asistente: `negocio.descripcion`,
> `redes.instagram` y `productos[0].nombre` traen su clave con la ruta; el correo
> repetido trae solo `message`; y la descripción de 79 caracteres **no deja la
> cuenta creada** —el login posterior devuelve 401—.
>
> **`productos[0].nombre` no tiene un campo donde pintarse**: el backend nombra
> el producto por su posición y la lista del navegador no la enseña. Se listan
> aparte en el paso 3, o el aviso de «revisa los campos marcados» mandaría a
> mirar algo que no está marcado.
>
> **`Formulario.module.css` se amplía en vez de duplicarse.** Es el tercer
> formulario del proyecto —login, registro de cliente y este—, que es cuando toca
> extraer: se le añaden `select` y `textarea` y una tarjeta más ancha.
>
> **Lectura de `diseno.md` que conviene dejar por escrito:** pide «el botón de
> siguiente deshabilitado con una razón visible». Aquí no se deshabilita nunca:
> al pulsarlo salen los errores y el foco va al primer campo que falla, que es el
> `markAllAsTouched()` del curso. La razón visible se cumple igual, y sin un
> botón muerto.
>
> **Devuelve a la portada el «Publicar mi negocio»** que el PR 7 tuvo que quitar
> por no dejar enlaces muertos.
>
> **Falta la revisión visual en el navegador**, igual que en los siete
> incrementos anteriores: el entorno sigue sin tener uno.
El asistente, con su propia ruta y su recorrido, separado del registro de
cliente: paso 1 la cuenta, paso 2 el negocio, paso 3 el escaparate. Los tres
recogen datos en el navegador y **se envían juntos** en la única petición del PR
10. Barra de progreso de cuatro pasos —«Paso 2 de 4» en móvil, según
[diseno.md](diseno.md#asistente-de-pasos)—, con el cuarto llegando en el PR
siguiente.

Se valida en el navegador lo mismo que valida el backend, con la función pura
`validar()` de siempre: el contador de la descripción a la vista, el formato del
teléfono, el barrio dependiente de la ciudad. Que el `400` sea la excepción y no
la forma normal de descubrir un error.

Los productos se añaden uno detrás de otro con la lista creciendo a la vista, y
el paso se puede dejar vacío. El precio se escribe en pesos y viaja como número:
nada de máscaras ni de librerías de moneda.

Cuando el backend responde `201`, el token ya viene dentro: se abre la sesión sin
pasar por el login y se aterriza en `/mi-negocio`.

Si el `400` trae una clave por campo, se pinta en su campo y el asistente vuelve
al paso que lo contiene. Con una transacción detrás, no hay estado a medias que
recomponer.

```
feat(auth): añadir el asistente de registro de emprendedores
```

> **Hito:** un recorrido desde cero crea la cuenta, el negocio y dos productos, y
> aterriza en `/mi-negocio` con el aviso de que está en revisión. Una descripción
> de 79 caracteres no deja avanzar del paso 2. Un correo ya usado devuelve al
> paso 1 con el mensaje en su campo, y **no deja nada creado**: reintentar con
> otro correo funciona.

### PR 12 · `feat/carga-inicial-fotos` · **TERMINADO**

> Entregado en la rama `feat/frontend-carga-inicial-fotos`. Verificado contra el
> backend con `curl`: tres subidas seguidas quedan en `orden` 0, 1 y 2 con la
> primera como `principal`; borrar la portada recoloca y la segunda pasa a serlo
> (B9); la imagen se descarga de `/fotos/{archivo}` con `200 image/png` **sin
> token**; y la galería sigue completa tras cerrar sesión y volver a entrar.
>
> **Los tres rechazos, comprobados uno a uno:** un `.txt` devuelve «La imagen
> tiene que ser JPG o PNG», una de 5,95 MB «Cada imagen puede pesar 5 MB como
> mucho», y la séptima «La galería admite 6 fotos como mucho».
>
> **Corrección a lo que decía este plan.** Daba por hecho que un fichero por
> encima del límite del contenedor lo cortaba la infraestructura con otro
> mensaje. No es así: `GlobalExceptionHandler` trata
> `MaxUploadSizeExceededException` y devuelve **el mismo texto del dominio**. Se
> comprobó con una imagen de 7,33 MB. Comprobar el tamaño en el navegador sigue
> mereciendo la pena, pero por la espera que ahorra, no por el mensaje.
>
> **Las subidas van de una en una y en orden, a propósito.** En paralelo llegan
> desordenadas y la portada sería la que ganara la carrera, porque el backend
> numera por orden de llegada.
>
> **A partir del paso 4 no se puede volver atrás**: el negocio ya existe y
> reenviar el formulario chocaría con el correo recién ocupado. El botón
> «Anterior» desaparece.
>
> Las vistas previas se liberan con `URL.revokeObjectURL` al quitar una imagen,
> al subirla y al salir del paso. Sin lo último, salir a `/mi-negocio` en una SPA
> no descarga el documento y la memoria se queda reservada.
>
> **Falta la revisión visual en el navegador**, la única comprobación del plan
> que no se ha hecho en ningún incremento: el entorno no tiene navegador.

> **Con este incremento la fase 3 queda cerrada** y el asistente está entero.
> Solo falta el PR 13, que es documentación.
El cuarto paso, ya con la sesión abierta: subida de imágenes contra
`POST /api/v1/negocios/mio/fotos`, previsualización antes de enviar, la primera
marcada como portada (B9), quitar una recién subida y saltar el paso.

El tamaño y el tipo se comprueban **antes** de enviar: una imagen de 6 MB viaja
entera por la red para que el backend la rechace al llegar, y quien la subió
espera todo ese rato para que le digan que no.

~~Y porque un fichero por encima del límite del contenedor lo cortaría la
infraestructura con otro mensaje.~~ **Eso era falso** y se comprobó al construir
el incremento: `GlobalExceptionHandler` trata `MaxUploadSizeExceededException` y
devuelve el mismo texto del dominio. La razón es la espera, no el mensaje.

Con esto el asistente está entero y la fase cumple lo que pedía: **el emprendedor
llega a su negocio con las fotos, el nombre y los precios ya puestos.**

```
feat(negocios): añadir la carga inicial de fotos al registro del emprendedor
```

> **Hito:** el recorrido completo, con base limpia, crea la cuenta, el negocio,
> dos productos y tres fotos, y termina en `/mi-negocio` enseñándolos, la primera
> foto como portada. Una imagen de más de 5 MB se rechaza en el navegador con su
> mensaje. Saltar el paso también completa el registro. Cerrar sesión, entrar de
> nuevo y seguir viéndolo todo.

---

## Cierre — Documentación (PR 13)

### PR 13 · `docs/frontend-fase-1` · **TERMINADO**

> Entregado en la rama `docs/frontend-fase-1`. El README de la raíz suma el
> guion completo de la sustentación —levantar las dos mitades, la parte pública,
> el asistente de punta a punta, la aprobación con el administrador y el negocio
> apareciendo—, con la tabla de qué negocios crear antes y una de qué hacer si
> algo falla en directo. El del frontend suma las nueve rutas y lo que **no**
> entra en la fase, para que nadie lo busque.
>
> **El recorrido no se re-ejecutó desde una base limpia.** `docker compose down
> -v` habría borrado los siete negocios que ya están creados a mano con sus fotos
> y sus opiniones, que son justo la vitrina de la sustentación. Los pasos se
> comprobaron sobre la base viva: la lista de pendientes responde y el guion
> encaja con lo que devuelve. El ciclo completo de aprobación ya se verificó en
> el PR 1.
>
> Queda escrita la trampa que más tiempo ha costado: **crear, subir fotos y
> aprobar al final**, porque una foto subida a un negocio ya aprobado entra
> pendiente y las fichas salen sin imagen.

No añade funcionalidad. README de la raíz explicando cómo se levantan las dos
mitades, y el guion del recorrido de la sustentación: base limpia, backend
arriba, frontend arriba, registro de emprendedor de punta a punta, aprobación
desde Postman y el negocio apareciendo en el directorio.

Como ya no hay siembra de demostración, el guion **incluye qué negocios crear**
para que la portada no se enseñe vacía el día de la sustentación. Es la parte que
antes hacía `CargaInicialDemo` y que ahora se prepara a mano.

Se hace al final, con los doce incrementos entregados, por la misma razón que el
PR 8 del backend: antes no hay nada estable que documentar.

```
docs: documentar el arranque del frontend y el recorrido de la fase 1
```

> **Hito:** alguien que no ha tocado el proyecto lo levanta entero siguiendo solo
> el README, y completa el recorrido sin preguntar nada.

---

## Estado de la fase 1 · **TERMINADA**

Los trece incrementos entregados. Once de frontend y dos de backend, según lo
previsto y sin incrementos de más.

| Fase | Incrementos | Estado |
|---|---|---|
| 0 · Preparar el terreno | PR 1–2 | **TERMINADA** |
| 1 · Vitrina pública | PR 3–5 | **TERMINADA** |
| 2 · Acceso | PR 6–9 | **TERMINADA** |
| 3 · Alta del emprendedor | PR 10–12 | **TERMINADA** |
| Cierre · Documentación | PR 13 | **TERMINADO** |

**Lo que queda pendiente, y hay que decirlo antes de que lo pregunten:** la
revisión visual en el navegador **no se ha hecho en ningún incremento**. El
entorno de desarrollo con el que se construyó no tiene navegador, así que los
360px, el recorrido con el tabulador y los tres estados de cada vista están
comprobados por código y por `curl`, pero no vistos. Es la única comprobación del
plan que no se cumplió, y le corresponde a quien tenga la pantalla delante.

Lo que la fase 2 recogerá, ya escrito en el alcance: el dashboard de gestión, el
panel de administración, los cursos y las opiniones. De esa lista,
[la fase 2](plan-de-entrega-frontend-fase-2.md) se llevó las opiniones y añadió
la recuperación de contraseña; el dashboard, el panel y los cursos siguen
pendientes.

---

## Antes de dar cualquier incremento por terminado

**En los once de frontend**, los de
[pruebas.md](pruebas.md#el-frontend-no-tiene-pruebas-automáticas-y-es-a-propósito),
sin excepción:

1. `cd frontend && npm run build` con **0 errores y 0 advertencias de tipos**.
2. Abrir el navegador y comprobar el hito del incremento. En el backend la regla
   era probar con `curl` y no fiarse de las pruebas; aquí es mirar la pantalla.
3. `grep -rn ': any\|as any' frontend/src/` vacío.
4. Todas las listas con `key` estable: el `id`, nunca el índice.
5. Ninguna llamada a `fetch` fuera de `frontend/src/api/`.

Y los cinco de la parte visual, que están en [diseno.md](diseno.md): `npm run
contraste` en verde, ningún color fuera de `tokens.css`, revisado a 360px,
recorrido entero con el tabulador, y los tres estados —cargando, vacío y error—
vistos de verdad y no supuestos.

**En los PR 1 y 10, que son de backend**: `./gradlew build` en verde con su
cobertura, el endpoint probado con `curl` con la aplicación arrancada, y
[api.md](api.md) actualizado en el mismo commit que cambia el contrato.

Y el paso que gobierna el repositorio entero: verificar por script que la
partición de commits cubre cada fichero exactamente una vez, con
`git status --porcelain -uall`.

## Endpoints que consume la fase

Quince: catorce ya entregados y documentados en [api.md](api.md), y uno nuevo.

| Recurso | Endpoints | PR |
|---|---|---|
| Estadísticas | `GET /estadisticas/portada` | 3 |
| Directorio | `GET /directorio`, `/directorio/destacados`, `/directorio/{id}` | 3, 4, 5 |
| Catálogos | `GET /catalogos/categorias-negocio`, `/catalogos/ciudades` | 4 |
| Acceso | `POST /auth/login`, `/auth/registro` | 6, 8 |
| Acceso | **`POST /auth/registro-emprendedor`** — nuevo | 10, 11 |
| Negocios | `GET /negocios/mio` | 9 |
| Fotos | `GET /negocios/mio/fotos` | 9 |
| Fotos | `POST`, `DELETE /negocios/mio/fotos` | 12 |
| Productos | `GET /negocios/mio/productos` | 9 |

Son quince y no trece: al construir el PR 9 se vio que `GET /negocios/mio`
devuelve los datos del negocio pero **no** su galería ni su escaparate, que
tienen endpoint propio. El recuento de arriba queda corregido aquí.

`POST /negocios` y `POST /negocios/mio/productos` siguen existiendo y siguen
probados —son el camino de A1-bis, decisión 4—, pero esta fase no los llama: el
alta de emprendedor pasa por el endpoint nuevo y los productos viajan dentro.

**Si un incremento necesitara un endpoint que no está en esta tabla, hay que
pararse a mirar**: o se ha salido del alcance de la fase, o hace falta otro PR de
backend como el 10. Lo que no vale es descubrirlo a mitad de una rebanada.
