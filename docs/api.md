# Contrato de la API

Todas las rutas cuelgan de `/api/v1`. Los cuerpos son JSON.

Este documento es la fuente de la colección de Postman del PR 14 y el guion para
explicar el flujo de datos en la sustentación.

## Autenticación

La API es *stateless*: no hay sesión y el servidor no guarda nada entre
peticiones. El token viaja en cada llamada:

```
Authorization: Bearer <token>
```

`POST /auth/login` devuelve el token, su tipo, la caducidad en milisegundos, y el
nombre y rol de quien entra, para que el cliente no tenga que descodificar el
token solo para saberlo.

### Quién puede llamar a qué

| Acceso | Significa |
|---|---|
| **Público** | Sin cabecera. El directorio se explora sin registrarse |
| **Sesión** | Cualquier rol autenticado |
| **ADMIN** | Solo el administrador |
| **Dueño** | Con sesión, y solo sobre su propio negocio |

Dos códigos que no significan lo mismo:

- **401** — falta autenticarse: no hay token, o no sirve.
- **403** — hay token válido, pero ese rol no puede hacer eso.

## Errores

Un único `GlobalExceptionHandler` decide todos los códigos. Los servicios lanzan
excepciones y no saben nada de HTTP.

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "..." }
```

Los errores de validación añaden **una clave por campo inválido**:

```json
{ "status": 400, "error": "Bad Request", "titulo": "El título es obligatorio" }
```

Un parámetro de consulta que no se puede convertir sigue la misma forma, y
cuando es de lista cerrada **enumera los valores admitidos**:

```json
{
  "status": 400, "error": "Bad Request",
  "orden": "«MAS_BARATOS» no es un valor válido para orden. Se admiten: CALIFICACION, NOMBRE, RECIENTES"
}
```

| Código | Cuándo |
|---|---|
| `400` | Validación de forma, o una regla de negocio incumplida |
| `401` | Sin token, token corrupto o caducado, o credenciales incorrectas |
| `403` | Rol insuficiente, o cuenta suspendida |
| `404` | No existe, **o no es visible para quien pregunta** |
| `410` | El enlace de un solo uso ya no vale: caducado, usado o inventado |

Un negocio pendiente o un curso en borrador devuelven **404, no 403**: no se
filtra información sobre lo que existe sin publicar.

## Paginación

Todos los listados devuelven una página de Spring Data:

```
?page=0&size=12&sort=titulo,asc
```

```json
{ "content": [ … ], "totalElements": 42, "totalPages": 4, "number": 0 }
```

Dos excepciones, las dos del PR 9: el **directorio** ignora `sort` y ordena con
su propio parámetro `orden`, de lista cerrada; y **destacados** devuelve una
lista suelta, porque son seis y no se paginan.

---

## Acceso · `/auth`

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `POST` | `/auth/registro` | Público | `201` con el token. Alta de cliente (A1) |
| `POST` | `/auth/registro-emprendedor` | Público | `201` con el token y el negocio (A1-ter) |
| `POST` | `/auth/login` | Público | `200` con el token |

```bash
curl -X POST localhost:8080/api/v1/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"María García","correo":"maria@gmail.com","contrasena":"contrasena123"}'
```

Contraseña de **mínimo 8 caracteres** (A6). El correo identifica la cuenta, es
único y **nunca se muestra en público**.

**Sí hay recuperación de contraseña**, y va aquí abajo. Lo que sigue sin haber
es verificación del correo al registrarse: I1 se reabrió solo para lo primero.

### Recuperar la contraseña

Tres endpoints públicos, como el resto de `/auth/**`. **Reabren I1**, que había
cerrado el dominio sin correos; la decisión y su porqué están en
[decisiones-dominio.md](decisiones-dominio.md).

| Método | Ruta | Devuelve |
|---|---|---|
| `POST` | `/auth/recuperacion` | `200` **siempre**, sin cuerpo |
| `GET` | `/auth/recuperacion/{token}` | `200` si el enlace vale, `410` si no |
| `POST` | `/auth/recuperacion/{token}` | `204` |

```bash
curl -X POST localhost:8080/api/v1/auth/recuperacion \
  -H 'Content-Type: application/json' -d '{"correo":"maria@gmail.com"}'

curl -X POST localhost:8080/api/v1/auth/recuperacion/$TOKEN \
  -H 'Content-Type: application/json' -d '{"contrasena":"contrasenaNueva"}'
```

Las dos reglas que hacen que esto sea una recuperación y no un agujero:

- **El token sale solo por correo**, nunca en la respuesta. Devolverlo aquí
  encadenaría bien las pantallas y de paso dejaría cambiar la contraseña de
  cualquiera sabiendo únicamente su dirección.
- **Pedirlo responde `200` exista la cuenta o no.** Un `404` para un correo
  desconocido convertiría el formulario en una forma de averiguar qué
  direcciones están registradas.

El token es **aleatorio, de un solo uso y caduca a los 30 minutos**. El `GET`
existe para que la pantalla pueda decir «este enlace ya no vale» **antes** de que
alguien escriba una contraseña nueva dos veces, y responde `410` y no `404`: el
enlace existió, lo que pasa es que ya no sirve. Caducado, usado e inventado dan
la misma respuesta, para no confirmar cuáles fueron reales.

La contraseña nueva exige los **mismos 8 caracteres mínimos de A6**: recuperar la
cuenta no rebaja la regla del alta.

**El correo sale a un SMTP local**, Mailpit, que es un servicio más de
`docker-compose.yml`. Se ve llegar en su bandeja web del **8025**. Si el servidor
de correo no contesta, la petición sigue devolviendo `200` y el fallo queda en el
registro: un `500` ahí delataría que esa cuenta existe.

### Alta de emprendedor con su negocio

Quien llega sabiendo que tiene un negocio no da solo su correo: trae el nombre,
la descripción, el contacto, la ubicación y su escaparate. Hacerlo pasar por el
registro corto y luego por `POST /negocios` obligaría a encadenar dos peticiones
y a dejar a medias a quien falle en la segunda.

```bash
curl -X POST localhost:8080/api/v1/auth/registro-emprendedor \
  -H 'Content-Type: application/json' -d '{
  "nombre": "Lucía Restrepo",
  "correo": "lucia@emprendehub.co",
  "contrasena": "contrasena123",
  "negocio": {
    "nombre": "Panadería La Espiga",
    "descripcion": "Pan de masa madre horneado cada mañana en horno de leña …",
    "telefono": "3105551234",
    "categoriaId": 1, "ciudadId": 3, "barrioId": 1,
    "nivelPrecio": "BAJO"
  },
  "redes":     { "instagram": "https://instagram.com/laespiga" }
}'
```

**El escaparate no entra aquí**, por lo mismo que las fotos: cada producto lleva
una imagen obligatoria y esta ruta es pública, sin sesión con la que subir
binarios. Se montan justo después, uno a uno, con el token que devuelve esta
llamada.

| Parte | Regla |
|---|---|
| La cuenta | Las mismas que A1: correo único, contraseña de 8 caracteres |
| `negocio` | **Obligatorio.** Las mismas validaciones que `POST /negocios` |
| `redes` | Opcional. Se validan contra su dominio, igual que en B8 |

**Todo entra en una sola transacción.** Si el negocio o un producto no pasan la
validación, no queda ni cuenta ni negocio: quien lo reintente encontrará su
correo libre. Es la razón de que esto sea un endpoint y no tres llamadas
encadenadas desde el navegador.

La cuenta **nace ya como `EMPRENDEDOR`**, sin pasar por cliente, y el negocio
nace `PENDIENTE` de revisión (B6).

> **Interruptor provisional de desarrollo.** Con `MODERACION_AUTOMATICA=true` el
> negocio nace `APROBADO` y las fotos que se suban nacen `APROBADA`, sin pasar
> por el administrador. Es para trabajar sin moderar a mano; **el valor por
> defecto es `false`** y la regla del dominio sigue siendo B6. Las dos mitades
> van juntas a propósito: aprobar solo el negocio dejaría sus fotos esperando
> revisión y el directorio enseñaría fichas sin imagen. La respuesta añade `negocioId` al cuerpo
habitual, para que quien acaba de registrarse pueda subir sus fotos sin pedir
antes `GET /negocios/mio`.

Los errores de los campos anidados **nombran su ruta**, que es lo que permite
llevar a quien rellena el formulario al paso que falló:

```json
{ "status": 400, "error": "Bad Request",
  "message": "La petición no es válida",
  "negocio.descripcion": "La descripción debe tener al menos 80 caracteres" }
```

**Las fotos no entran aquí.** Son binarios y ya tienen su endpoint multipart con
su validación de tipo, tamaño y máximo; traerlas obligaría a mezclar JSON y
ficheros en la misma petición y a duplicar esa comprobación. Se suben justo
después, con el token que devuelve este endpoint.

> `POST /negocios` sigue existiendo y es el camino de A1-bis: el cliente que ya
> tiene cuenta y opiniones y se anima más tarde. Son dos entradas al mismo
> estado, y está explicado en
> [decisiones-dominio.md](decisiones-dominio.md).

## Catálogos · `/catalogos`

Fijos, de solo lectura y públicos (G5).

| Método | Ruta | Devuelve |
|---|---|---|
| `GET` | `/catalogos/categorias-negocio` | Las 12 categorías (G1) |
| `GET` | `/catalogos/ciudades` | Ciudades **con sus barrios anidados** (G2, G3) |
| `GET` | `/catalogos/categorias-curso` | Las 5 categorías de formación |
| `GET` | `/catalogos/niveles-curso` | Básico, Intermedio, Avanzado |
| `GET` | `/catalogos/motivos-denuncia` | Los motivos para denunciar una opinión (C6) |

Las ciudades traen sus barrios en la misma respuesta: el selector de localización
necesita los dos niveles a la vez. Solo Medellín tiene barrios.

## Cursos · `/cursos`

Catálogo público. **Solo enseña los publicados** (E4).

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `GET` | `/cursos` | Público | Página filtrable |
| `GET` | `/cursos/{id}` | Público | Un curso publicado, o `404` |

Filtros, todos opcionales y combinables:

```
?categoria=MARKETING&nivel=BASICO&gratuito=true&texto=redes
```

El texto busca en título y descripción, sin distinguir mayúsculas.

## Directorio · `/directorio`

Público y sin sesión: la portada promete «explora sin necesidad de registrarte».

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `GET` | `/directorio` | Público | Página filtrable de negocios |
| `GET` | `/directorio/destacados` | Público | Lista de destacados (C7) |
| `GET` | `/directorio/{id}` | Público | Perfil público, o `404` |

**Solo salen los negocios aprobados de cuentas activas.** Uno pendiente,
rechazado o de un dueño suspendido no aparece en el listado y su identificador
responde `404`, nunca `403` (B6, B4). Mientras un cambio espera revisión, el
público sigue viendo la versión aprobada (B2-bis).

Filtros, todos opcionales y combinables:

```
?texto=pan&categoriaId=1&ciudadId=2&barrioId=1&calificacionMinima=4.0&nivelPrecio=MEDIO
```

| Filtro | Qué hace |
|---|---|
| `texto` | Busca en **nombre y descripción**, sin distinguir mayúsculas (G6) |
| `categoriaId` | Una de las 12 categorías (G1) |
| `ciudadId` | Ciudad. Incluye a los negocios **sin barrio** |
| `barrioId` | Afina dentro de la ciudad (G3) |
| `calificacionMinima` | Deja fuera a quien todavía no tiene calificación (C5) |
| `nivelPrecio` | `BAJO`, `MEDIO` o `ALTO`. **No hay «Gratis»** (G4) |

La ordenación es un conjunto cerrado, no un nombre de columna:

```
?orden=CALIFICACION   # por defecto: mejor calificados
?orden=NOMBRE         # A-Z
?orden=RECIENTES      # por fecha de APROBACIÓN, no de creación (G7)
```

Un `orden` o un `nivelPrecio` que no esté en la lista devuelve `400`. Los
negocios sin calificación van **al final**, no al principio: no tener opiniones
no es lo mismo que tener malas notas (C5).

`/directorio/destacados` devuelve **una lista, no una página**: son seis por
defecto, con `?limite=` entre 1 y 12. Solo entran los que tienen **al menos
cinco opiniones** (C7); sin ese mínimo, una sola opinión de 5★ desplazaría de la
portada a un negocio con 4,9 y 320 opiniones.

El perfil público **no lleva el estado ni el motivo del rechazo**, que son
conversación entre el dueño y el administrador. El teléfono sí es público; el
correo no aparece nunca.

Desde el PR 10 el perfil trae además **la galería, el escaparate y las redes**, y
por eso ya no devuelve la misma forma que la tarjeta:

```json
{
  "nombre": "Floristería Girasol", "instagram": "https://instagram.com/girasol",
  "fotos":     [ { "url": "/fotos/a1b2.png", "orden": 0, "principal": true } ],
  "productos": [ { "nombre": "Ramo de girasoles", "precio": 45000, "disponible": true,
                   "foto": "/fotos/c3d4.jpg" } ]
}
```

Solo salen las fotos **aprobadas**. La tarjeta del listado lleva `fotoPrincipal`
—una sola imagen— en lugar de la galería entera.

**Pedir el perfil anota una visita** (H1). Es el único sitio donde se cuentan; el
detalle está en [Métricas de visitas](#métricas-de-visitas--negociosmiometricas).

## Estadísticas · `/estadisticas`

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `GET` | `/estadisticas/portada` | Público | Las cuatro cifras de la barra |

```json
{ "negociosActivos": 12, "categorias": 12, "usuariosRegistrados": 34, "calificacionPromedio": 4.7 }
```

**Cifras calculadas, no fijas** (H4). Se cuentan con el mismo criterio con el que
el directorio enseña: un negocio que no se ve tampoco suma. Los usuarios
registrados excluyen al administrador, que es una cuenta sembrada (A3). Mientras
no haya ninguna opinión, `calificacionPromedio` viaja como **nulo y no como
cero** (C5).

## Negocios · `/negocios`

| Método | Ruta | Acceso | Hace |
|---|---|---|---|
| `POST` | `/negocios` | Sesión | Registra y **asciende a emprendedor** (A1-bis) |
| `GET` | `/negocios/mio` | Dueño | Su negocio, en el estado que esté |
| `PUT` | `/negocios/mio` | Dueño | **Propone** cambiar nombre o descripción |
| `PATCH` | `/negocios/mio/contacto` | Dueño | Cambia el teléfono, al instante |
| `PATCH` | `/negocios/mio/redes` | Dueño | Instagram y LinkedIn, al instante (B8) |
| `POST` | `/negocios/mio/reenviar` | Dueño | Corrige un rechazado y reenvía (B1) |

El registro llega en **una sola petición**: los cuatro pasos del prototipo son
una división visual del formulario.

| Campo | Regla |
|---|---|
| `nombre` | 3 a 120 caracteres |
| `descripcion` | **Mínimo 80 caracteres** |
| `telefono` | Móvil `3XXXXXXXXX` o fijo `60XXXXXXXX` (G8) |
| `nivelPrecio` | `BAJO`, `MEDIO`, `ALTO` (G4) |
| `barrioId` | Opcional, y debe pertenecer a la ciudad indicada |

**`PUT /negocios/mio` no cambia el negocio.** Guarda una propuesta que espera
revisión, y el público sigue viendo la versión aprobada mientras tanto (B2-bis).
El teléfono y las redes son la excepción: se aplican al momento.

Los dos enlaces de B8 son opcionales y se validan contra su dominio: un
`instagram` que apunte a otro sitio devuelve `400`. Sin eso, el botón
«Instagram» de un perfil público podría llevar a cualquier parte. Enviarlos
vacíos es quitarlos.

## Fotos del negocio · `/negocios/mio/fotos`

Solo el **dueño**, y siempre sobre su propio negocio.

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/negocios/mio/fotos` | Su galería, revisadas y sin revisar |
| `POST` | `/negocios/mio/fotos` | Sube una imagen (`201`). **`multipart/form-data`** |
| `PATCH` | `/negocios/mio/fotos/orden` | Reordena la galería y con ella la portada |
| `DELETE` | `/negocios/mio/fotos/{id}` | La quita al momento (`204`) |

```bash
curl -X POST $A/negocios/mio/fotos -H "Authorization: Bearer $T" \
  -F "archivo=@local.jpg"
```

Las tres reglas de B9, todas en el servicio y todas con su prueba:

| Regla | Qué pasa si no se cumple |
|---|---|
| **JPG o PNG** | `400`. El tipo sale de la cabecera, no del nombre del fichero |
| **5 MB por imagen** | `400` |
| **6 por negocio** | `400`. Cuentan también las que esperan revisión |

**La principal es la primera por orden** (B9), sin campo que la marque. Al borrar
una, las demás se recolocan y la portada pasa a ser la siguiente.

**Elegir portada es reordenar**, por lo mismo: se manda la galería entera en el
orden deseado y la primera queda de portada.

```bash
curl -X PATCH $A/negocios/mio/fotos/orden -H "Authorization: Bearer $T" \
  -H 'Content-Type: application/json' -d '{"orden": [20, 18, 19]}'
```

Viaja la **lista completa** y no un movimiento suelto —«sube esta una
posición»—: el orden es el estado, no la acción. Así el servidor no reconstruye
de dónde venía nada y dos pestañas abiertas no lo dejan a medias. Devuelve la
galería ya recolocada.

Se exige que nombre **exactamente** las fotos del negocio, una sola vez cada una:
ni de menos, ni repetidas, ni ajenas. Cualquier otra cosa es `400`, porque
aceptar una lista parcial obligaría a inventar dónde van las que faltan.

**Reordenar no pasa por revisión**, igual que borrar: no publica ninguna imagen
que no estuviera ya publicada, que es el riesgo que controla B2. Si la que sube
al primer puesto todavía espera revisión, el público sigue viendo como portada la
primera de las **aprobadas**, porque el perfil solo recibe esas.

**Una foto nueva nace `PENDIENTE`.** B2 manda a revisión los cambios sobre campos
públicos y nombra las fotos expresamente, pero B2-bis prohíbe que el negocio
salga del directorio mientras espera. Con el estado en cada foto se cumplen las
dos:

- Si el negocio **todavía no está aprobado**, sus fotos se publican con él.
- Si **ya está publicado**, subir una abre una propuesta de cambio y el público
  sigue viendo las anteriores hasta que el administrador la aprueba.
- **Borrar no pasa por revisión**: quitar una imagen no publica nada nuevo, que
  es el riesgo que B2 controla.

Las imágenes se descargan de **`/fotos/{archivo}`**, que es público y va fuera de
`/api`: la etiqueta `<img>` de un navegador no manda cabecera de token.

> **Al preparar datos, el orden importa.** Subir las fotos *después* de aprobar
> el negocio las deja esperando revisión, y entonces el directorio devuelve
> `fotoPrincipal` nula y las fichas salen sin imagen. Parece un fallo del
> frontend y es la regla funcionando. Para dejar una demostración lista: crear el
> negocio, subir las fotos y **después** aprobarlo; o aprobar también el cambio
> pendiente que la subida generó.

## Productos · `/negocios/mio/productos`

Solo el **dueño**. Es un **escaparate**: no se vende nada (F1).

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/negocios/mio/productos` | Su escaparate |
| `POST` | `/negocios/mio/productos` | Crea (`201`). **`multipart/form-data`** |
| `PUT` | `/negocios/mio/productos/{id}` | Edita |
| `PATCH` | `/negocios/mio/productos/{id}/disponibilidad?disponible=false` | El interruptor de F3 |
| `DELETE` | `/negocios/mio/productos/{id}` | Elimina (`204`) |

```bash
curl -X POST $A/negocios/mio/productos -H "Authorization: Bearer $T" \
  -F "nombre=Pan de masa madre" -F "precio=12000" \
  -F "descripcion=Fermentado 24 horas" -F "disponible=true" \
  -F "foto=@pan.jpg"
```

| Campo | Regla |
|---|---|
| `nombre` | 2 a 120 caracteres |
| `precio` | Obligatorio, no negativo, dos decimales como mucho |
| `descripcion` | **Opcional** (F2). En blanco se guarda como ausente |
| `disponible` | El único estado que hay: **no existe inventario** (F3) |
| `foto` | **Obligatoria.** JPG o PNG, 5 MB como mucho, igual que la galería (B9) |

**El alta va en `multipart` y no en JSON** porque la imagen es obligatoria y es
un binario. Los campos viajan sueltos, que es lo que manda un formulario con un
`<input type="file">`; no hay una parte JSON aparte que componer.

Un escaparate con huecos no es un escaparate: por eso la foto entra en la misma
petición y no en una segunda. Crear el producto primero y completarlo después es
justo lo que permitiría que el hueco existiera.

`ProductoResponse` devuelve `foto` con la URL pública, que **nunca es nula**. Se
descarga de `/fotos/{archivo}`, sin token, igual que las de la galería. Al borrar
el producto se borra también su fichero.

Si falta la parte `foto`, la respuesta es `400` con el formato de siempre y una
clave `foto` que nombra lo que falta.

Un producto no disponible **sigue saliendo** en el perfil, marcado. Esconderlo
haría del interruptor un borrado con otro nombre.

> **`PUT` no cambia la imagen.** Edita los campos de texto y el precio; sustituir
> la foto no está en el contrato entregado y ninguna pantalla de esta fase lo
> pide.

El producto de otro negocio responde `404`, no `403`: la consulta busca por
producto **y** negocio a la vez, así que el ajeno sencillamente no aparece.

## Opiniones · `/negocios/{id}/opiniones`

**Leerlas es público; escribirlas exige sesión** (C1). Es la única ruta del
proyecto donde el mismo prefijo mezcla las dos cosas.

| Método | Ruta | Acceso | Hace |
|---|---|---|---|
| `GET` | `/negocios/{id}/opiniones` | Público | Página de opiniones, las últimas primero |
| `POST` | `/negocios/{id}/opiniones` | Sesión | Publica la suya (`201`) |
| `GET` | `/negocios/{id}/opiniones/mia` | Sesión | La propia, o `404` si no ha opinado |
| `PUT` | `/negocios/{id}/opiniones/mia` | Autor | La edita |
| `DELETE` | `/negocios/{id}/opiniones/mia` | Autor | La borra (`204`) |

Las rutas de escritura **no llevan identificador de opinión**: cada persona
tiene como mucho una por negocio (C2), así que `/mia` la identifica sin
ambigüedad y sin dar pie a probar con el número de otra.

| Campo | Regla |
|---|---|
| `calificacion` | Obligatoria, **de 1 a 5** |
| `comentario` | **Opcional**, 300 caracteres como mucho |

Las reglas que devuelven `400` o `404`:

- **Una por persona y negocio** (C2). La segunda responde `400` y remite a
  editar la primera. La restricción está también en el esquema, así que dos
  peticiones a la vez tampoco se cuelan.
- **Nunca sobre el negocio propio** (A4).
- **Solo sobre negocios publicados** (B6). Uno pendiente responde `404`, igual
  que su perfil.
- Se publican **al instante** (C4): no hay revisión previa.

**Cada cambio recalcula el promedio del negocio**, incluido el borrado que hace
el administrador. Sin ninguna opinión el promedio vuelve a **nulo, no a cero**
(C5): el negocio se enseña como «Nuevo», sale del filtro de estrellas y se
ordena al final en «Mejor calificados».

Una opinión editada viaja con `editada: true`, para que quien la lea sepa que el
texto no es el original. Del autor sale **su nombre y nada más**.

## Denunciar una opinión · `/opiniones/{id}/denuncias`

| Método | Ruta | Acceso | Hace |
|---|---|---|---|
| `POST` | `/opiniones/{id}/denuncias` | Sesión | Denuncia la opinión (`204`) |

```json
{ "motivo": "LENGUAJE_INAPROPIADO" }
```

El motivo sale de una **lista cerrada** (C6), que se consulta en
`GET /catalogos/motivos-denuncia`: `LENGUAJE_INAPROPIADO`, `INFORMACION_FALSA`,
`SPAM`, `NO_ES_SOBRE_EL_NEGOCIO` y `DATOS_PERSONALES`. Uno inventado devuelve
`400` con los admitidos.

Denuncia **cualquier usuario con sesión**, incluido el dueño del negocio
afectado (C3). No se puede denunciar la propia —para eso está borrarla— ni dos
veces la misma.

**Denunciar no oculta la opinión**: sigue publicada mientras el administrador
decide. Esconderla al primer aviso convertiría el botón en uno de censurar.

## Consultas · buzón del negocio

Todo exige sesión. El cliente escribe desde el perfil; el dueño lee en su panel.

| Método | Ruta | Acceso | Hace |
|---|---|---|---|
| `POST` | `/negocios/{id}/consultas` | Sesión | Escribe al negocio (`201`) |
| `GET` | `/negocios/mio/consultas` | Dueño | Su buzón, las más recientes primero |
| `PATCH` | `/negocios/mio/consultas/{id}/lectura?leida=true` | Dueño | Marca leída o la desmarca |

| Campo | Regla |
|---|---|
| `asunto` | Obligatorio, 120 caracteres como mucho |
| `mensaje` | Obligatorio, **500 caracteres** como mucho |

La petición **no lleva nombre ni correo**: contactar exige sesión, así que salen
de la cuenta y no de un formulario donde cualquiera podría escribir el correo de
otra persona.

**Solo se escribe a negocios publicados** (B6), y nadie se escribe a su propio
buzón.

### El correo del cliente

```json
{
  "asunto": "Ramo para un cumpleaños",
  "mensaje": "¿Entregan a domicilio el sábado?",
  "nombreCliente": "María García", "correoCliente": "maria@gmail.com",
  "leida": false, "fechaLectura": null
}
```

**Es la única respuesta de la API que enseña el correo de otra persona**, y es a
propósito (D2). La plataforma no manda correos por esto —el único que manda es
el de recuperar la contraseña, I1-bis— ni permite responder desde
dentro, así que sin esa dirección el buzón sería un montón de preguntas sin forma
de contestarlas. La excepción está acotada: el correo **no aparece** en el
directorio, ni en el perfil público, ni en las opiniones, y solo lo recibe el
dueño del negocio al que va dirigida la consulta.

### Leídas y no leídas

`?leida=false` devuelve las pendientes, y el `totalElements` de esa página es el
número que el panel enseña como aviso: no hace falta un endpoint de contador.

**Listar el buzón no marca nada como leído.** Abrir la pantalla para echar un
vistazo no es lo mismo que haber atendido lo que hay dentro, así que el estado lo
decide el dueño con `PATCH`. Desmarcar una consulta le quita también la fecha de
lectura: dejarla puesta diría que se abrió y se ignoró.

**No hay respuesta desde la plataforma** (D2) ni hilo de conversación. Es un
buzón de entrada, y la limitación es deliberada: montar mensajería de ida y
vuelta sin correos de aviso habría exigido que las dos partes entraran a mirar.

## Métricas de visitas · `/negocios/mio/metricas`

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `GET` | `/negocios/mio/metricas/visitas` | Dueño | Semana, mes, variación y serie diaria |

```json
{
  "totalHistorico": 1034,
  "semana": { "actual": 248, "anterior": 221, "variacionPorcentual": 12.2 },
  "mes":    { "actual": 1034, "anterior": 957, "variacionPorcentual": 8.0 },
  "serie":  [ { "fecha": "2026-07-28", "visitas": 31 }, … ]
}
```

**No hay endpoint para registrar una visita.** Se anota sola al pedir
`GET /directorio/{id}`, y solo ahí: uno aparte dejaría inflar el contador con un
bucle de curl.

Las reglas del conteo (H1):

| Regla | Qué significa |
|---|---|
| **Una por perfil, sesión y día** | Recargar la página no suma |
| **El dueño no cuenta** | Solo se le descarta si tiene sesión iniciada |
| **Un perfil que no se ve no cuenta** | Un negocio pendiente responde `404` y no registra nada |

Con sesión, la visita se agrupa por la cuenta: la misma persona cuenta una vez al
día aunque mire desde el móvil y desde el portátil. Sin sesión se resume la
petición —dirección y navegador— en un hash que no identifica a nadie y solo
sirve para decir «esto ya lo conté hoy». Tiene un límite conocido: varias
personas tras la misma salida a internet cuentan como una. Sin cookies ni sesión
de servidor no hay forma mejor.

El día se corta en **America/Bogota**, no en UTC: con UTC un negocio de Medellín
cambiaría de día a las siete de la tarde.

**La variación es nula cuando el periodo anterior fue cero.** Pasar de ninguna
visita a cinco no es un aumento del quinientos por ciento ni del infinito: es que
antes no había con qué comparar.

**La serie trae los días vacíos en cero.** Si se saltaran, la gráfica uniría el
lunes con el jueves y aparentaría una caída que no existió.

## Notificaciones · `/notificaciones`

| Método | Ruta | Acceso | Hace |
|---|---|---|---|
| `GET` | `/notificaciones` | Sesión | Los avisos propios, los más recientes primero |
| `PATCH` | `/notificaciones/{id}/lectura?leida=true` | Sesión | Marca uno, o lo devuelve a pendiente |
| `PATCH` | `/notificaciones/leer-todas` | Sesión | Marca todo lo pendiente y responde cuántos eran |

Cuelgan de la **persona**, no del negocio. Cada una nace de un hecho (H2), y son
solo cuatro:

| Tipo | Cuándo |
|---|---|
| `OPINION_NUEVA` | Alguien opinó sobre su negocio |
| `CONSULTA_NUEVA` | Alguien le escribió al buzón |
| `NEGOCIO_APROBADO` | El administrador lo publicó |
| `NEGOCIO_RECHAZADO` | El administrador pidió cambios, con el motivo |

**Fuera los hitos de visitas** que enseñaba el prototipo («superaste las 1.000
este mes»): no son un hecho del que haya que enterarse, y H2 los descarta.

Sin correos de aviso (I1, que I1-bis solo reabre para la recuperación) esto no
es decoración: es el único canal por el que un emprendedor se entera de que le
rechazaron el negocio.

`?leida=false` devuelve las pendientes, y el `totalElements` de esa página es el
número de la campana. El texto **se escribe cuando ocurre el hecho y se guarda
tal cual**: si después se borra la opinión que lo provocó, el aviso sigue
diciendo lo que pasó en su momento.

## Gestión de cursos · `/admin/cursos`

Solo **ADMIN** (E3). A diferencia del catálogo público, aquí se ven los borradores.

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/admin/cursos` | Lista todo, borradores incluidos |
| `GET` | `/admin/cursos/{id}` | Uno cualquiera |
| `POST` | `/admin/cursos` | Crea (`201`). **Nace en borrador** |
| `PUT` | `/admin/cursos/{id}` | Edita |
| `PATCH` | `/admin/cursos/{id}/publicar` | Publica |
| `PATCH` | `/admin/cursos/{id}/borrador` | Retira del catálogo |
| `DELETE` | `/admin/cursos/{id}` | Elimina (`204`) |

Regla que cruza dos campos y vive en el servicio: **un curso gratuito no puede
llevar precio y uno de pago está obligado a llevarlo.** Publicar dos veces
devuelve `400`.

## Moderación · `/admin/moderacion`

Solo **ADMIN**.

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/negocios-pendientes` | Cola de revisión, los más antiguos primero |
| `PATCH` | `/negocios/{id}/aprobar` | Aprueba y sella la fecha |
| `PATCH` | `/negocios/{id}/rechazar` | Rechaza. **Motivo obligatorio** |
| `GET` | `/cambios-pendientes` | Cola de propuestas, las más antiguas primero |
| `PATCH` | `/negocios/{id}/cambio/aprobar` | Publica los valores propuestos **y sus fotos** |
| `PATCH` | `/negocios/{id}/cambio/rechazar` | Descarta la propuesta |
| `GET` | `/denuncias` | Cola de opiniones denunciadas, las más antiguas primero |
| `PATCH` | `/denuncias/{id}/eliminar-opinion` | Borra la opinión. **Motivo obligatorio** (`204`) |
| `PATCH` | `/denuncias/{id}/desestimar` | La deja publicada (`204`) |
| `PATCH` | `/usuarios/{id}/suspender` | Suspende la cuenta (`204`) |
| `PATCH` | `/usuarios/{id}/reactivar` | La reactiva (`204`) |
| `GET` | `/log` | Historial, filtrable por fechas |

El motivo del rechazo lo lee el dueño en `GET /negocios/mio`: sin correos de
aviso (I1), ese campo es el único sitio donde se entera de por qué le
rechazaron.

`GET /cambios-pendientes` enseña el valor actual junto al propuesto —revisar es
comparar— y cuántas fotos espera publicar cada propuesta. Una propuesta puede ser
**solo de fotos**, y entonces los dos textos coinciden. Aprobar una publica sus
imágenes; rechazarla **las descarta**, porque dejarlas pendientes haría que la
siguiente propuesta del dueño las publicara de rebote.

```
GET /admin/moderacion/log?desde=2026-08-23T00:00:00Z&hasta=2026-08-25T00:00:00Z
```

La cola de denuncias trae **el texto denunciado, de quién es y sobre qué
negocio**: decidir si una opinión se borra exige leerla. Las dos salidas quedan
en el log —`OPINION_ELIMINADA` con su motivo y `DENUNCIA_DESESTIMADA`—, y
borrar una opinión **recalcula el promedio del negocio**, que es el momento que
más fácil se olvida porque no lo dispara su autor.

El log es de solo escritura: nunca se edita ni se borra.

---

## Recorrido completo para la demostración

Sirve como guion de la colección de Postman y de la sustentación.

```bash
A=http://localhost:8080/api/v1

# 1. Un cliente se registra y entra
curl -X POST $A/auth/registro -H 'Content-Type: application/json' \
  -d '{"nombre":"Sofía Ruiz","correo":"sofia@gmail.com","contrasena":"contrasena123"}'
TC=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"sofia@gmail.com","contrasena":"contrasena123"}' | jq -r .token)

# 2. Registra su negocio: nace PENDIENTE y ella pasa a EMPRENDEDOR
curl -X POST $A/negocios -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{ … }'

# 3. El administrador entra, lo ve en la cola y lo rechaza con motivo
TA=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"admin@emprendehub.co","contrasena":"admin12345"}' | jq -r .token)
curl -H "Authorization: Bearer $TA" $A/admin/moderacion/negocios-pendientes
curl -X PATCH $A/admin/moderacion/negocios/1/rechazar -H "Authorization: Bearer $TA" \
  -H 'Content-Type: application/json' -d '{"motivo":"La descripción no explica qué vendes"}'

# 4. Ella lee el motivo en su panel, corrige y reenvía
curl -H "Authorization: Bearer $TC" $A/negocios/mio
curl -X POST $A/negocios/mio/reenviar -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{ … }'

# 5. El administrador aprueba
curl -X PATCH $A/admin/moderacion/negocios/1/aprobar -H "Authorization: Bearer $TA"

# 6. Ella propone cambiar el nombre: el negocio SIGUE publicado con el viejo
curl -X PUT $A/negocios/mio -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{ … }'

# 7. El administrador aprueba el cambio y ahora sí se publica
curl -X PATCH $A/admin/moderacion/negocios/1/cambio/aprobar -H "Authorization: Bearer $TA"

# 7-bis. Sube fotos y monta el escaparate
curl -X POST $A/negocios/mio/fotos -H "Authorization: Bearer $TC" -F "archivo=@local.jpg"
curl -X POST $A/negocios/mio/productos -H "Authorization: Bearer $TC" \
  -F "nombre=Pan de masa madre" -F "precio=12000" -F "disponible=true" \
  -F "foto=@pan.jpg"
curl -X PATCH $A/negocios/mio/redes -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' \
  -d '{"instagram":"https://instagram.com/panaderia"}'

# 7-ter. La foto de un negocio ya publicado espera revisión (B2)
curl -H "Authorization: Bearer $TA" $A/admin/moderacion/cambios-pendientes
curl -X PATCH $A/admin/moderacion/negocios/1/cambio/aprobar -H "Authorization: Bearer $TA"

# 8. Y ya se busca en el directorio, sin ninguna sesión
curl "$A/directorio?texto=pan&ciudadId=2&orden=RECIENTES"
curl $A/directorio/1
curl $A/estadisticas/portada

# 9. Si el administrador suspende al dueño, el negocio sale del directorio (B4)
curl -X PATCH $A/admin/moderacion/usuarios/2/suspender -H "Authorization: Bearer $TA"
curl $A/directorio            # ya no está
curl $A/directorio/1          # 404, no 403

# 9-bis. Un cliente opina; el promedio del negocio cambia al instante (C4)
curl -X POST $A/negocios/1/opiniones -H "Authorization: Bearer $TC2" \
  -H 'Content-Type: application/json' \
  -d '{"calificacion":5,"comentario":"La mejor de Medellín"}'
curl $A/negocios/1/opiniones          # sin sesión

# 9-ter. Alguien la denuncia y el administrador decide
curl -X POST $A/opiniones/1/denuncias -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{"motivo":"INFORMACION_FALSA"}'
curl -H "Authorization: Bearer $TA" $A/admin/moderacion/denuncias
curl -X PATCH $A/admin/moderacion/denuncias/1/desestimar -H "Authorization: Bearer $TA"

# 9-quater. Un cliente pregunta y la dueña lo lee en su buzón (D1, D2, D3)
curl -X POST $A/negocios/1/consultas -H "Authorization: Bearer $TC2" \
  -H 'Content-Type: application/json' \
  -d '{"asunto":"Reserva para 4","mensaje":"¿Tienen mesa el sábado a las 8?"}'
curl -H "Authorization: Bearer $TC" "$A/negocios/mio/consultas?leida=false"
curl -X PATCH "$A/negocios/mio/consultas/1/lectura?leida=true" -H "Authorization: Bearer $TC"

# 9-quinquies. Cada visita al perfil cuenta una vez al día (H1)
curl $A/directorio/1; curl $A/directorio/1     # recargar no suma
curl -H "Authorization: Bearer $TC" $A/negocios/mio/metricas/visitas

# 9-sexies. Y el panel avisa de lo que fue pasando (H2, H3)
curl -H "Authorization: Bearer $TC" "$A/notificaciones?leida=false"
curl -X PATCH $A/notificaciones/leer-todas -H "Authorization: Bearer $TC"

# 10. Todo quedó registrado
curl -H "Authorization: Bearer $TA" $A/admin/moderacion/log
```

## Datos de la demostración

La aplicación arranca solo con lo que no tiene sentido escribir a mano: los
catálogos, la cuenta de administrador y los 8 cursos. **Los negocios no se
siembran**, se crean, y por eso el recorrido de arriba empieza por crearlos.

Con la base recién levantada, `GET /directorio` devuelve una página vacía y
`/estadisticas/portada` devuelve ceros con `calificacionPromedio` nulo. Es lo
correcto: las cifras se calculan (H4) y todavía no hay nada que contar.

La colección de Postman `backend/postman/EmprendeHub.postman_collection.json` cubre los
**64 endpoints** en 75 peticiones, agrupadas por quién las usa. Su primera
carpeta, **1 · Acceso y datos de partida**, crea la clienta, la emprendedora y su
negocio, y guarda cada token en su variable; el resto de peticiones los heredan.
La carpeta **6 · Seguridad** cubre aparte los casos que tienen que fallar: 401,
403 y 400.

---

**Al añadir endpoints, actualizar este documento en el mismo PR.** Un contrato
que va por detrás del código es peor que no tenerlo.
