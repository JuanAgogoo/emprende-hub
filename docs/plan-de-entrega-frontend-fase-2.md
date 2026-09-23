# Plan de entrega — Frontend, fase 2

Seis incrementos, cada uno una rama y un Pull Request, igual que en
[plan-de-entrega-frontend-fase-1.md](plan-de-entrega-frontend-fase-1.md). El
orden es de dependencia: cada rama sale de `main` con el anterior ya fusionado, y
**ningún incremento necesita nada que construya uno posterior**.

La numeración es de la fase y empieza de nuevo en 1. Los números de PR en GitHub
siguen la cuenta del repositorio, que va por el #33.

Cinco incrementos son de frontend y **uno es de backend**: el PR 4 añade la
recuperación de contraseña, que no existe. Los tres primeros consumen el contrato
ya entregado sin tocarlo.

Cada incremento de frontend es una **rebanada vertical**: tipo, llamada a la API,
estado y vista, algo que se abre en el navegador y se ve funcionando. Por eso
cada uno lleva su **hito de verificación** escrito como algo observable, y no se
da por terminado sin comprobarlo con los ojos. Las decisiones técnicas del
frontend están en [arquitectura.md](arquitectura.md#el-frontend), cómo se
verifica en [pruebas.md](pruebas.md), y las reglas visuales —paleta, tipografía,
mobile first y accesibilidad— en [diseno.md](diseno.md).

Cada incremento indica el mensaje de commit exacto, para pegarlo sin redactarlo.

---

## Lo que se pidió y ya estaba hecho

El backlog llegó con seis peticiones. **Tres estaban entregadas y una a medias**,
así que no se rehacen. Queda escrito aquí porque la pregunta «¿y esto por qué no
está en el plan?» se va a hacer sola.

| Petición | Estado al escribir este plan |
|---|---|
| Explorador de directorio sin registro | **Hecho** en la fase 1, PR 4. `/directorio` es público: seis filtros combinables, ordenación de lista cerrada y paginación |
| Buscador principal de emprendimientos | **Hecho.** Es la misma pieza que el anterior, más la búsqueda en vivo de la portada, que enseña resultados sin cambiar de página |
| Ver perfil completo de un emprendimiento | **Hecho salvo las reseñas** en la fase 1, PR 5: galería, escaparate con precios, redes, teléfono, calificación y número de opiniones |
| Calificar con estrellas | Backend entregado y probado. **Frontend: nada** |
| Escribir y publicar una reseña | Backend entregado y probado. **Frontend: nada** |
| Recuperación de contraseña | **No existe en ninguna de las dos mitades.** Ni siquiera hay endpoint para que la restablezca el administrador |

Lo único que le falta al perfil para estar «completo» es el listado de reseñas, y
eso es exactamente el PR 1 de esta fase. Por eso esa petición no tiene incremento
propio: se cumple dentro del trabajo de opiniones.

---

## Alcance de la fase 2

Lo que se construye:

| | |
|---|---|
| Opiniones · leer | Listado paginado en el perfil público, sin sesión |
| Opiniones · escribir | Calificación con estrellas y comentario opcional |
| Opiniones · gestionar | Editar y borrar la propia |
| Recuperar contraseña | Enlace por correo con token de un solo uso |

Lo que **no** entra, y conviene tenerlo por escrito antes de que alguien lo dé
por supuesto:

- **Denunciar una opinión.** El endpoint existe (C3) y la decisión de dejarlo
  fuera está razonada en la decisión 7: una denuncia solo sirve si alguien la
  resuelve, y el panel de administración no está en esta fase.
- **El panel de administración**, otra vez: moderación, denuncias, cursos.
- **El dashboard de gestión** del emprendedor: editar el negocio, el buzón de
  consultas, las métricas de visitas y las notificaciones siguen pendientes.
- **Cambiar la contraseña estando dentro.** Es otra cosa que recuperarla: no hay
  endpoint y nadie lo pidió. Si se quiere, es un incremento aparte.
- **Verificar el correo al registrarse.** La fase añade envío de correos, pero
  solo para la recuperación. Ampliarlo a la verificación de alta cambiaría los
  dos registros y no se pidió.
- **Ninguna dependencia nueva en el frontend**: sigue sin haber más que
  `react-router-dom`. La única dependencia nueva del proyecto es
  `spring-boot-starter-mail`, en el backend, y está razonada en la decisión 2.

La fase consume **ocho endpoints**: cinco ya entregados y tres que añade el PR 4.

---

## Decisiones que gobiernan el plan

Siete cosas se resolvieron antes de escribir los incrementos. Están aquí porque
cambian lo que hay que construir, y porque son las preguntas que va a hacer quien
lea el plan.

### 1. Lo que ya estaba construido no se rehace

Está en la tabla de arriba. Se revisó el código antes de escribir el plan, no se
dio por supuesto: las rutas declaradas en `App.tsx`, los módulos de `api/` y las
reglas de `SecurityConfig`. El directorio, el buscador y el perfil público
funcionan y son públicos; lo único que falta del perfil es el listado de reseñas.

### 2. I1 se reabre: va a haber correos

`docs/decisiones-dominio.md` cerró el dominio con **I1: «No se envía ningún
correo, ni se deja preparado»**, y de ahí salió que se retirara el enlace
«¿olvidé mi contraseña?» del prototipo. Pedir ahora la recuperación reabre esa
decisión, y hay que decirlo con todas las letras en vez de colarlo.

**Se envía correo de verdad**, con `spring-boot-starter-mail` —que es del propio
Spring Boot, no una librería de fuera— contra un **servidor SMTP local**:
[Mailpit](https://github.com/axllent/mailpit), un servicio más de
`docker-compose.yml`. Lo que eso compra:

- Funciona **sin internet y sin cuenta de correo**. El día de la sustentación no
  se depende de que Gmail conteste.
- El correo **se ve llegar** en la interfaz web de Mailpit, en el 8025. Eso es
  enseñable, que es más de lo que consigue un token impreso en un log.
- No hay nada que configurar en la máquina de nadie: sube con el resto del
  `docker compose up -d`.

**Lo que hay que asumir:** el correo no sale al mundo real. Si algún día se
quisiera, se cambian tres propiedades de `application.yml` y nada más, pero eso
no es de esta fase.

`decisiones-dominio.md` se actualiza en el mismo PR que lo construye. I1 no se
borra: se deja escrito qué decía y por qué cambió.

### 3. El token no viaja en la respuesta, y pedirlo nunca dice si el correo existe

Las dos reglas que hacen que esto sea una recuperación y no un agujero:

- **El token sale solo por correo.** La tentación es devolverlo en la respuesta
  de «solicitar» para encadenar la pantalla siguiente sin salir del navegador.
  Eso permitiría a cualquiera cambiar la contraseña de cualquiera sabiendo solo
  su correo. Se descarta.
- **`POST /auth/recuperacion` responde `200` siempre**, exista la cuenta o no.
  Si respondiera `404` para un correo desconocido, el formulario se convertiría
  en una forma de averiguar qué direcciones tienen cuenta.

El token es **aleatorio, de un solo uso y caduca a los 30 minutos**. Se guarda
tal cual en la tabla, no cifrado, y esa es una simplificación consciente: quien
pueda leer esa tabla ya tiene todo lo demás, y la caducidad corta con el uso
único acotan lo que vale. Está escrito aquí para poder defenderlo, no escondido.

### 4. Una opinión por persona y negocio obliga a preguntar antes de dibujar

C2 dice que cada persona tiene **como mucho una opinión por negocio**, y que la
segunda responde `400` remitiendo a editar la primera. Si el perfil dibujara el
formulario de publicar sin más, quien ya opinó se encontraría un `400` que no
puede entender ni arreglar.

Por eso el perfil, con sesión iniciada, pregunta por `GET
/negocios/{id}/opiniones/mia` antes de decidir qué enseña: el formulario de
publicar, o la opinión propia con sus botones. **Un `404` ahí no es un error**,
es que todavía no ha opinado, exactamente igual que en `/negocios/mio`.

Esa llamada entra ya en el **PR 2**, no en el 3. Sin ella el PR 2 tendría un
callejón sin salida propio, y este plan no deja callejones para el siguiente.

### 5. El dueño no opina sobre su negocio, y el botón ni se dibuja

A4 lo prohíbe y el backend lo hace cumplir. El frontend no dibuja el formulario
cuando quien mira es el dueño del negocio, con un texto que lo explica en vez de
dejar un hueco.

Como todas las comprobaciones de rol del proyecto: **es comodidad de la interfaz,
no una medida de seguridad**. Quien mande la petición a mano se topa igual con el
backend, que es donde se comprueba de verdad.

### 6. Las opiniones se publican al instante, y el promedio cambia con ellas

C4: no hay revisión previa. Publicar una opinión la deja visible en el acto, así
que la lista tiene que enseñarla sin recargar la página.

Y con ella cambia la cabecera del perfil: `calificacionPromedio` y
`numeroOpiniones` vienen en la misma respuesta del negocio. El primero
**viaja nulo mientras no haya ninguna opinión** (C5), y se resuelve con `??` y
nunca con `||`, o un promedio legítimo de `0` se convertiría en «Sin opiniones».
Es la misma trampa que ya mordió en la portada.

### 7. Denunciar no entra, y no es un olvido

`POST /opiniones/{id}/denuncias` existe, está probado y C3 lo describe. Se queda
fuera porque **una denuncia solo sirve si alguien la resuelve**, y quien la
resuelve es el panel de administración, que no está en esta fase ni en la
anterior. Dibujar el botón dejaría las denuncias en una cola que nadie mira, y
peor: daría a entender que alguien las atiende.

Entra cuando entre el panel, no antes.

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

## Fase 0 — Opiniones (PR 1–3)

Los tres son **frontend puro**: los cinco endpoints existen, están documentados
en [api.md](api.md) y tienen sus pruebas. Van primero por eso — son la parte sin
riesgo de contrato— y porque cierran de una vez tres de las seis peticiones del
backlog.

Se verifican contra un negocio aprobado y **al menos dos cuentas de cliente**,
porque una sola no deja ver la lista con opiniones de otros ni la regla de A4.

### PR 1 · `feat/frontend-opiniones-listado` · **TERMINADO** · [#35](https://github.com/JuanAgogoo/emprende-hub/pull/35)

El listado de reseñas en el perfil público, que es lo único que le faltaba al
perfil para estar completo. `GET /api/v1/negocios/{id}/opiniones` devuelve una
**página**, las últimas primero, y es público: se lee sin iniciar sesión.

Cada opinión trae `autor`, `calificacion`, `comentario`, `fechaCreacion` y
`editada`. Del autor sale **su nombre y nada más**: ni correo, ni foto, ni
enlace a un perfil que no existe.

Aparece el componente `Estrellas` **en modo lectura**, que dibuja la calificación
de 1 a 5. Lo reutilizan la cabecera del perfil y cada fila de la lista, y el PR 2
lo extiende para que además se pueda pulsar. Las estrellas son decorativas para
quien usa lector de pantalla: el número va en texto, porque «★★★☆☆» no se lee.

Una opinión con `editada: true` lo dice, para que quien la lea sepa que el texto
no es el original.

Los tres estados dibujados, como en toda vista que carga datos: esqueletos
mientras llega, el bloque con su mensaje cuando el negocio **no tiene ninguna
opinión** —que es lo normal en un negocio recién publicado, no un caso raro— y el
error con su reintento.

```
feat(opiniones): mostrar el listado de reseñas en el perfil público
```

> **Hito:** un negocio con tres opiniones las enseña con su nota, su autor y su
> fecha, sin haber iniciado sesión. Un negocio sin ninguna explica que todavía no
> tiene, y la cabecera dice «Sin opiniones» y no «0,0». Con más opiniones que las
> de una página, el paginador trae otras distintas. Parar el backend y recargar
> muestra el mensaje de error, no una pantalla en blanco.

### PR 2 · `feat/frontend-opinar` · **TERMINADO** · [#36](https://github.com/JuanAgogoo/emprende-hub/pull/36)

Calificar con estrellas y escribir la reseña, que son las peticiones 5 y 6 del
backlog y una sola rebanada: no hay forma de publicar una opinión sin nota,
porque `calificacion` es obligatoria de 1 a 5.

El formulario aparece **solo con sesión iniciada** (C1) y **solo si no eres el
dueño** (A4, decisión 5). Sin sesión, en su lugar va un enlace al login que
vuelve al perfil.

`Estrellas` gana su modo interactivo. Es un grupo de radios de verdad —cinco
`<input type="radio">` con su `<fieldset>` y su `<legend>`—, no cinco `<div>` con
`onClick`: así funciona con el tabulador y con las flechas sin escribir una línea
de JavaScript para el teclado, y el lector de pantalla anuncia «3 de 5».

El comentario es **opcional** y admite 300 caracteres, con el contador a la vista
como en la descripción del negocio.

Antes de dibujar nada se pregunta por `GET /opiniones/mia`, según la decisión 4:
si ya hay una, no se enseña el formulario de publicar sino la propia, en modo
lectura. Editarla es el PR 3; aquí solo se dice que existe.

Al publicar, la opinión **aparece en la lista sin recargar** (C4) y la cabecera
del perfil actualiza su promedio y su recuento.

```
feat(opiniones): añadir la publicación de reseñas con calificación por estrellas
```

> **Hito:** con sesión de cliente, calificar con tres estrellas y escribir un
> comentario deja la reseña arriba de la lista, y la nota del negocio cambia en
> la misma pantalla. Sin sesión no hay formulario sino un enlace al login. El
> dueño del negocio no ve el formulario, y si manda la petición a mano el backend
> le responde. Intentar publicar sin elegir estrellas no envía nada.

### PR 3 · `feat/frontend-editar-opinion` · **TERMINADO** · [#37](https://github.com/JuanAgogoo/emprende-hub/pull/37)

Editar y borrar la propia, con `PUT` y `DELETE /opiniones/mia`. Sin esto, C2 deja
atrapado a quien se equivoque al puntuar: no puede corregirlo y la segunda
publicación le responde `400`.

Las rutas de escritura **no llevan identificador de opinión**: cada persona tiene
como mucho una por negocio, así que `/mia` la identifica sin ambigüedad y sin dar
pie a probar con el número de otra. Eso quiere decir que el frontend no necesita
guardar el `id` de la propia para nada.

El formulario del PR 2 se reutiliza con los valores puestos: es el mismo
componente, no una copia con dos campos cambiados.

El borrado **pide confirmación** —es destructivo y no se deshace— y al terminar
vuelve a dejar el formulario de publicar, porque volver a opinar pasa a ser
posible. El promedio del negocio se recalcula, y si era la única opinión vuelve a
**nulo y no a cero** (C5).

```
feat(opiniones): permitir editar y borrar la reseña propia
```

> **Hito:** cambiar la nota de la propia reseña la actualiza en la lista y marca
> «editada»; el promedio del negocio cambia con ella. Borrarla la quita, devuelve
> el formulario de publicar y, si era la única, el negocio vuelve a decir «Sin
> opiniones». Cancelar la confirmación no borra nada.

---

## Fase 1 — Recuperar la contraseña (PR 4–5)

El incremento de las decisiones 2 y 3. El primero es de backend y el segundo lo
consume: no hay pantalla sin endpoint, ni correo sin servidor que lo reciba.

### PR 4 · `feat/recuperar-contrasena` — backend · **TERMINADO** · [#38](https://github.com/JuanAgogoo/emprende-hub/pull/38)

El más grande de la fase, y el único que toca las dos mitades del repositorio.
**Reabre I1**, así que `docs/decisiones-dominio.md` entra en el mismo PR.

La regla que lo gobierna es la de siempre: **el ajuste imita al código que ya
existe alrededor**. No es la ocasión de introducir un patrón mejor.

- **Mailpit en `docker-compose.yml`**, con su puerto SMTP en el 1025 y su
  interfaz web en el 8025. El backend apunta ahí por `application.yml`, con las
  propiedades sustituibles por entorno como todas las demás.
- Dependencia **`spring-boot-starter-mail`**, la única nueva del proyecto.
- Entidad `TokenRecuperacion` con su usuario, su token, su caducidad y su marca
  de usado. Lombok granular y constructor explícito, como el resto de `model/`.
- Tres endpoints en `AuthController`, todos públicos como el resto de `/auth/**`:

```
POST /api/v1/auth/recuperacion            { "correo": "..." }        -> 200 siempre
GET  /api/v1/auth/recuperacion/{token}                               -> 200 | 410
POST /api/v1/auth/recuperacion/{token}    { "contrasena": "..." }    -> 204
```

  El `GET` existe para que la pantalla pueda decir «este enlace ya no vale»
  **antes** de que alguien escriba una contraseña nueva dos veces. Devuelve `410`
  y no `404`: el enlace existió, lo que pasa es que ya no sirve.
- El `POST` de confirmación **invalida el token al usarlo** y exige los mismos 8
  caracteres mínimos de A6. Reutiliza la validación que ya hay, no la reescribe.
- **Los tres niveles de prueba**, con el patrón AAA y los comentarios
  `//arrange`, `//act`, `//assert`, `//verify` escritos tal cual. Casos que no
  pueden faltar: correo desconocido que **responde `200` sin mandar nada**, token
  caducado, token ya usado, token inventado, y contraseña nueva de 7 caracteres.
- `docs/api.md`, `docs/decisiones-dominio.md` —con I1 reabierto— y la colección
  de Postman **en el mismo PR**.
- Las cifras envejecen mal y hay tres sitios que las repiten: el recuento de
  endpoints en `api.md`, en el README y en la tabla final de
  `plan-de-entrega.md`, más el recuento de pruebas.

```
feat(auth): añadir la recuperación de contraseña por correo con token de un solo uso
```

> **Hito:** `cd backend && ./gradlew build` en verde con la cobertura de
> `service/**` por encima del 80%. Con la aplicación arrancada, un `curl` a
> `/auth/recuperacion` devuelve `200` y **el correo aparece en Mailpit**, en el
> 8025, con su enlace. Ese enlace deja poner una contraseña nueva y el login
> funciona con ella; el mismo enlace usado dos veces devuelve `410`. Un correo
> que no existe devuelve `200` y **no** deja ningún correo en Mailpit.

### PR 5 · `feat/frontend-recuperar-contrasena` · **TERMINADO**

Las dos pantallas, más el enlace que la fase 1 tuvo que no dibujar.

- **`/recuperar`**: pide el correo y, al enviarlo, enseña siempre el mismo
  mensaje —«si ese correo tiene cuenta, le llega un enlace»—, sin decir si
  existe. La pantalla tiene que respetar la decisión 3 igual que el endpoint: de
  nada sirve que el backend no filtre si la interfaz lo hace por él.
- **`/recuperar/:token`**: comprueba el token al entrar y, si ya no vale, lo dice
  sin enseñar el formulario. Si vale, pide la contraseña nueva **dos veces**, con
  la misma validación de coincidencia que los dos registros.
- Al terminar, lleva al login con el aviso de que ya puede entrar, exactamente
  como hace el alta de cliente.

**Devuelve al login su enlace «¿Olvidaste tu contraseña?»**, que el prototipo
tenía y que I1 mandó retirar. Es el tercer enlace que esta fase o la anterior
tuvieron que quitar por no dejar rutas muertas; conviene cotejar todos los `to=`
del código contra las rutas de `App.tsx` antes de cerrar el incremento.

El campo de contraseña reutiliza `CampoContrasena`, con su botón de revelar.

```
feat(auth): añadir las pantallas de recuperación de contraseña
```

> **Hito:** desde el login, «¿Olvidaste tu contraseña?» pide el correo; el enlace
> que llega a Mailpit abre la pantalla de contraseña nueva; al guardarla, el
> login entra con ella. Abrir ese mismo enlace otra vez dice que ya no vale, sin
> enseñar el formulario. Un correo que no existe enseña el mismo mensaje que uno
> que sí.

---

## Cierre — Documentación (PR 6)

### PR 6 · `docs/frontend-fase-2`

No añade funcionalidad. Actualiza lo que los cinco incrementos dejaron desfasado:

- El **README de la raíz**: Mailpit en el arranque y en la tabla de puertos, y el
  guion de la sustentación con los dos recorridos nuevos —opinar sobre un negocio
  y recuperar una contraseña viendo llegar el correo—.
- El **README del frontend**: las rutas suben de nueve a once, y la lista de lo
  que **no** entra pierde las opiniones y la recuperación.
- `docs/plan-de-entrega-frontend-fase-1.md`, cuyo alcance dice que las opiniones
  y la recuperación no entran «en esta fase». Sigue siendo verdad, pero conviene
  que apunte a dónde fueron a parar.

Se hace al final, con los cinco incrementos entregados, por la misma razón que el
PR 13 de la fase 1: antes no hay nada estable que documentar.

```
docs: documentar las opiniones y la recuperación de contraseña
```

> **Hito:** alguien que no ha tocado el proyecto levanta las cuatro piezas
> siguiendo solo el README, opina sobre un negocio y recupera una contraseña
> viendo el correo en Mailpit, sin preguntar nada.

---

## Estado de la fase 2

| Fase | Incrementos | Estado |
|---|---|---|
| 0 · Opiniones | PR 1–3 | **TERMINADO** |
| 1 · Recuperar la contraseña | PR 4–5 | **TERMINADO** |
| Cierre · Documentación | PR 6 | |

---

## Antes de dar cualquier incremento por terminado

**En los cinco de frontend**, los de
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

**En el PR 4, que es de backend**: `./gradlew build` en verde con su cobertura,
los tres endpoints probados con `curl` con la aplicación arrancada, **el correo
visto en Mailpit** y [api.md](api.md) actualizado en el mismo commit que cambia
el contrato.

Y el paso que gobierna el repositorio entero: verificar por script que la
partición de commits cubre cada fichero exactamente una vez, con
`git status --porcelain -uall`.

> **La revisión visual sigue siendo el punto flojo.** No se hizo en ninguno de
> los once incrementos de la fase 1 porque el entorno de desarrollo no tiene
> navegador. En esta fase el listado de opiniones y el grupo de estrellas son
> justo lo que no se puede dar por bueno sin mirarlo: el recorrido con el
> tabulador por cinco radios y el comportamiento a 360px no los comprueba ningún
> script.

## Endpoints que consume la fase

Ocho: cinco ya entregados y documentados en [api.md](api.md), y tres nuevos.

| Recurso | Endpoints | PR |
|---|---|---|
| Opiniones | `GET /negocios/{id}/opiniones` | 1 |
| Opiniones | `POST /negocios/{id}/opiniones`, `GET /negocios/{id}/opiniones/mia` | 2 |
| Opiniones | `PUT`, `DELETE /negocios/{id}/opiniones/mia` | 3 |
| Acceso | **`POST /auth/recuperacion`** — nuevo | 4, 5 |
| Acceso | **`GET /auth/recuperacion/{token}`** — nuevo | 4, 5 |
| Acceso | **`POST /auth/recuperacion/{token}`** — nuevo | 4, 5 |

`POST /opiniones/{id}/denuncias` sigue existiendo y sigue probado, pero esta fase
no lo llama, por la decisión 7.

**Si un incremento necesitara un endpoint que no está en esta tabla, hay que
pararse a mirar**: o se ha salido del alcance de la fase, o hace falta otro PR de
backend como el 4. Lo que no vale es descubrirlo a mitad de una rebanada.
