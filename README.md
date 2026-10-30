# EmprendeHub

Portal de emprendimiento local. Proyecto de la asignatura Plataformas de
Programación Empresarial.

| Mitad | Dónde | Stack |
|---|---|---|
| **API REST** | [`backend/`](backend) | Java 25 · Spring Boot 4.1.1 · Gradle · PostgreSQL · Spring Data JPA · Spring Security |
| **Interfaz web** | [`frontend/`](frontend) | React · Vite · TypeScript |

## Qué hace

Un directorio de negocios del Valle de Aburrá, con su catálogo de cursos para
emprendedores. Cuatro perfiles, cada uno con lo suyo:

| Perfil | Qué puede hacer |
|---|---|
| **Visitante**, sin cuenta | Buscar y filtrar el directorio, ver perfiles con galería y escaparate, leer opiniones y recorrer el catálogo de cursos |
| **Cliente** | Además: calificar y opinar, denunciar una opinión ajena y escribirle a un negocio |
| **Emprendedor** | Registrar su negocio con un asistente de cuatro pasos y gestionarlo: datos, fotos, escaparate, buzón de consultas, visitas y avisos |
| **Administrador** | Un panel con sus colas: aprobar negocios y cambios con vista previa, resolver denuncias, suspender cuentas, publicar cursos y consultar el historial de moderación |

Un negocio nuevo no se ve hasta que el administrador lo aprueba, y sus cambios
posteriores esperan revisión sin sacarlo del directorio. Las reglas, con su
porqué, están en [docs/decisiones-dominio.md](docs/decisiones-dominio.md).

## Arrancar

Para levantarlo hace falta **solo Docker**. Ni Gradle, ni el JDK 25, ni Node: van
dentro de las imágenes.

Antes, **las credenciales de la base de datos**. No están en el repositorio: se
eligen en un `.env` en la raíz, que git ignora.

```bash
cp .env.example .env     # y rellena DB_USER y DB_PASSWORD con los que quieras
docker compose up -d
```

PostgreSQL crea el usuario con esas credenciales la primera vez que arranca, y el
backend las usa para conectar. Sin ellas, Compose no arranca y dice cuál falta.

> **Si cambias la contraseña después**, la base ya existe con la anterior y el
> backend no podrá entrar. `docker compose down -v` la borra y la crea de nuevo.

Eso levanta las cuatro piezas y deja la web en <http://localhost:5173>, la API
en <http://localhost:8080> y la bandeja de correo de
[Mailpit](https://github.com/axllent/mailpit) en <http://localhost:8025>, que es
donde se ven los correos que manda la aplicación. **La primera vez tarda varios minutos**: Gradle se
descarga sus dependencias. Las siguientes son segundos, porque la caché vive en
un volumen.

```bash
docker compose logs -f backend   # para ver cuándo termina de arrancar
```

> El backend tarda unos segundos más que la web. Hasta que responde, la portada
> enseña su mensaje de error: es lo esperado, no un fallo.

Las dos mitades corren **en modo desarrollo, con el código montado desde el
host**, no copiado a la imagen:

| Mitad | Al cambiar el código |
|---|---|
| Frontend | **Recarga en caliente.** Guardar el fichero y ya |
| Backend | `docker compose restart backend`. Java no tiene recarga en caliente |

### Sin Docker para las dos mitades

Si prefieres el bucle de siempre —`bootRun` y `npm run dev` en el host—, arranca
solo la base:

En una terminal, la base, el correo y la API (necesita el JDK 25):

```bash
docker compose up -d postgres mailpit   # la base en el 5433 y el correo en el 1025
cd backend
set -a && . ../.env && set +a           # el backend lee las credenciales del entorno
./gradlew bootRun                       # la API en el 8080
```

Sin el `set -a`, el backend arranca sin credenciales y falla con `password
authentication failed for user "${DB_USER}"`: Spring no lee el `.env`, Compose sí.

En otra, desde la raíz, la web (necesita Node):

```bash
cd frontend
npm install                             # solo la primera vez
npm run dev                             # la web en el 5173
```

Mailpit entra también aquí: sin un SMTP escuchando, el correo de recuperación no
sale. La aplicación no falla por eso —la petición sigue devolviendo `200`—, pero
el enlace no llega a ninguna parte.

**No mezcles los dos modos a la vez**: los dos quieren el 8080 y el 5173, y el
segundo en arrancar falla. Para cambiar de uno a otro, `docker compose stop
backend frontend` primero.

### Parar y limpiar

```bash
docker compose stop           # para todo, sin borrar nada
docker compose down           # además retira los contenedores; los datos siguen
docker compose down -v        # borra también la base y las cachés
```

> `down -v` se lleva la base entera, y con ella los negocios creados a mano. Las
> fotos **no**: viven en `backend/uploads/`, en el disco. Después de un `down -v`
> la base referencia ficheros que ya no tienen dueño y sobran ahí.

### Puertos y usuario

| Variable | Por defecto | Para qué |
|---|---|---|
| `DB_PORT` | `5433` | El 5432 suele estar ocupado por otro PostgreSQL |
| `API_PORT` | `8080` | Cambiar si ya tienes un backend corriendo |
| `WEB_PORT` | `5173` | |
| `SMTP_PORT` | `1025` | El SMTP de Mailpit, al que manda el backend |
| `MAILPIT_PORT` | `8025` | La bandeja de Mailpit, que se abre en el navegador |
| `HOST_UID` / `HOST_GID` | `1000` | El usuario con el que corren los contenedores |
| `MODERACION_AUTOMATICA` | `false` | **Provisional.** A `true`, los negocios nacen publicados, sus fotos aprobadas y las ediciones se publican al momento |

`HOST_UID` existe para que las fotos que suba el backend **no queden siendo de
root** dentro del repositorio. Con un usuario 1000 —lo normal en Linux— no hay
que tocar nada; si el tuyo es otro:

```bash
HOST_UID=$(id -u) HOST_GID=$(id -g) docker compose up -d
```

No uses `UID` a secas: en bash es una variable de solo lectura y la asignación
falla. Para no repetirlo, `echo "HOST_UID=$(id -u)" >> .env`.

### En Windows

Funciona con **Docker Desktop** y no hace falta nada más: el JDK 25 va dentro de
la imagen del backend y Node dentro de la del frontend.

Lo único que hay que respetar es **no forzar `core.autocrlf`**. El repositorio
trae un `.gitattributes` que obliga a LF, porque `gradlew` se ejecuta dentro de
un contenedor Linux y con CRLF su intérprete pasa a ser `/bin/sh\r`: el backend
no arranca y el error no dice por qué. Con clonar normalmente basta; si el repo
ya estaba clonado de antes, `git rm -r --cached . && git checkout .` lo recoloca.

`HOST_UID` no hace falta: Docker Desktop no traslada la propiedad de los
ficheros del anfitrión. Y `./scripts/cifras.sh` pide bash, que viene con Git
Bash; no es parte de levantar el proyecto.

### Dos cosas que muerden al levantarlo

- **Los `container_name` son fijos**, así que el proyecto **no se puede levantar
  dos veces en la misma máquina**: el segundo falla con «container name already
  in use» y el mensaje no dice que ese sea el problema. Si hay otro checkout del
  repositorio, solo uno puede estar arriba.
- **Si un puerto está ocupado, el servicio no arranca** y el resto queda a
  medias. Los cinco son variables (`DB_PORT`, `API_PORT`, `WEB_PORT`,
  `MAILPIT_PORT`, `SMTP_PORT`): se cambian en un `.env` junto a
  `docker-compose.yml`. Y si tras el choque el contenedor sigue sin ir, quedó
  creado sin red: hace falta `docker compose up -d --force-recreate <servicio>`,
  porque `up -d` a secas solo lo arranca.

`MODERACION_AUTOMATICA` **ya no está puesta**: el proyecto modera de verdad. El
negocio nace `PENDIENTE` (B6), el administrador lo publica, y lo que su dueño
edite después espera en la cola de cambios (B2-bis) mientras el público sigue
viendo la versión aprobada.

Durante el desarrollo estuvo a `true` en `docker-compose.yml`, para no aprobar
cada negocio a mano. Si vuelve a hacer falta, se añade esa variable al servicio
`backend`: con ella el negocio nace publicado, sus fotos aprobadas y lo que el
dueño edita se publica al momento. **Pero entonces no hay nada que moderar**, y
el recorrido de abajo pierde su paso más vistoso.

### Datos de prueba

**Al arrancar por primera vez, con la base vacía, ya hay con qué recorrer todo**,
sin crear nada antes. Se siembran los catálogos —12 categorías y las 4 ciudades
del Valle de Aburrá, Medellín con 2 barrios—, la cuenta de administrador, los 8
cursos y **los datos de prueba**, generados por código a partir de una semilla
fija: cada base nueva tiene los mismos negocios, cuentas y cifras. Las fechas se
anclan al día del arranque.

Pasan por los mismos servicios que la API, con sus reglas: una opinión por
persona y negocio (C2), la foto obligatoria de cada producto (F4) y las fotos
subidas **antes** de aprobar (B2). Las imágenes se dibujan al sembrar —el color
de la categoría y la inicial— y se guardan en `FOTOS_DIR`, porque `uploads/` no
está en git.

La siembra es idempotente: si ya hay negocios no toca nada, así que reiniciar no
duplica. Para empezar de cero, `docker compose down -v`. Tarda unos 12 segundos,
solo el primer arranque, y durante ese rato la API ya responde pero el directorio
sale vacío: esperar en los logs a la línea `Sembrados 40 negocios`.

| Qué | Cuánto, medido sobre una base nueva |
|---|---|
| Negocios | **40**: 37 publicados, 2 pendientes y 1 rechazado, repartidos por las 12 categorías, las 4 ciudades y los 3 niveles de precio |
| Cuentas | **72**: el administrador, 31 clientes y 40 emprendedores |
| Productos del escaparate | 179, cada uno con su foto |
| Fotos de galería | 96: 90 publicadas y 6 pendientes de revisión |
| Opiniones | 216 |
| Consultas al buzón | 69, las de más de una semana ya leídas |
| Visitas | Unas 5 400 en los últimos 90 días: 5 362 arrancando el 29 de octubre de 2026. Es la única cifra que cambia con el día del arranque, porque depende de dónde caen los fines de semana |
| Avisos | 324 |
| Cola de negocios | 2 pendientes de aprobar |
| Cola de cambios | 2: uno de texto y una foto nueva |
| Cola de denuncias | 2 abiertas, y 1 ya desestimada |
| Historial de moderación | 40 entradas |

Los que tienen un papel en el recorrido:

| Negocio | Para qué está |
|---|---|
| **Café Pergamino** | El de la emprendedora fija: 4 fotos, 6 productos, 10 opiniones, buzón, visitas y avisos sin leer |
| **Estudio Arrayán** y **Tienda deportiva Mandarina** | Pendientes: hay algo que aprobar en directo (B6) |
| **Casa El Mirador** | Rechazado, con su motivo a la vista de su dueño |
| **Academia Aurora** | Propone una descripción nueva (B2-bis) |
| **Fotografía Tierra Viva** | Subió una foto ya publicado, que espera en «Cambios» (B2) |
| **Peluquería canina Trapiche** y **Tienda Buen Viaje** | Tienen una opinión denunciada, en «Denuncias» (C3) |
| **Laboratorio Siete Cueros** | Recién publicado y sin opiniones: se ve «Sin opiniones» y no «0,0» (C5) |
| **Estudio Santa Elena** | Sin fotos de galería: sale el marcador con la inicial |
| **Barbería El Roble** | El primer destacado de la portada: 9 opiniones (C7) |

**Para arrancar sin datos de prueba**, con la base vacía:
`SEMILLA_DEMO=false docker compose up -d`. Entonces el directorio sale vacío y la
portada muestra ceros con la calificación media nula, que es lo correcto: las
cifras se calculan (H4).

### Cuentas de prueba

| Cuenta | Correo | Contraseña | Para qué sirve |
|---|---|---|---|
| Administrador | `admin@emprendehub.co` | `admin12345` | El panel: colas de moderación, usuarios, cursos e historial |
| Clienta | `cliente@emprendehub.co` | `cliente12345` | Opinar, denunciar y escribir a un negocio. Ya opinó en tres, pero no en Café Pergamino |
| Emprendedora | `emprendedora@emprendehub.co` | `emprende12345` | El panel de Café Pergamino |
| Clientes generados | `cliente01@emprendehub.co` … `cliente30@emprendehub.co` | `demo12345` | Autores de las opiniones y las consultas |
| Emprendedores generados | `emprendedor01@emprendehub.co` … `emprendedor39@emprendehub.co` | `demo12345` | Uno por negocio, en el orden en que se crearon |

Los dueños de los negocios con papel en el recorrido: Estudio Arrayán es
`emprendedor38`, Tienda deportiva Mandarina `emprendedor39`, Casa El Mirador
`emprendedor37`, Academia Aurora `emprendedor04` y Fotografía Tierra Viva
`emprendedor05`.

> Con `MODERACION_AUTOMATICA=true` los negocios nacen publicados y **las colas
> del administrador salen vacías**: no hay pendientes, ni rechazado, ni cambios.

## El recorrido de una demostración

El guion de punta a punta, con las dos mitades arriba. Son unos veinte minutos.

> Parte del que se usó en la sustentación del **29 de septiembre de 2026**. Desde
> entonces la moderación ya no se hace desde Postman sino desde el **panel de
> administración**, y el guion lo enseña.

### Antes

Nada que preparar: los [datos de prueba](#datos-de-prueba) traen un negocio
pendiente que aprobar, un cambio y una denuncia por resolver, destacados en la
portada y un panel de emprendedora con métricas. Basta con arrancar el día antes,
para que la primera compilación y la siembra no ocurran delante de nadie.

> **El orden de las fotos importa**, por si se crea un negocio en directo: hay que
> **crearlo, subirle las fotos y aprobarlo al final**. Una foto subida a un
> negocio **ya aprobado** entra pendiente de revisión (B2), el público no la ve y
> el directorio devuelve `fotoPrincipal` nula. Parece un fallo del frontend y es
> la regla funcionando.

### Durante: el guion

**1. Levantar el proyecto.** Un comando y una terminal:

```bash
docker compose up -d
docker compose logs -f backend    # esperar a «Started EmprendeHubApplication»
```

> Hacerlo **antes** de empezar, no delante de nadie: aunque
> las imágenes ya estén construidas, el backend tarda unos segundos en responder
> y la portada enseña su mensaje de error mientras tanto.

**2. La parte pública, sin iniciar sesión.** En <http://localhost:5173>:

- La portada, con las cuatro cifras **calculadas** y los destacados.
- Buscar desde la portada: lleva al directorio con el texto ya puesto.
- En el directorio, filtrar por categoría, cambiar de ciudad —el selector de
  barrio se repuebla solo— y ordenar por calificación.
- Entrar a un negocio: la foto de la tarjeta crece hasta ser la del detalle.
  Galería, precios en pesos y enlaces a las redes.
- **Cursos**, en la cabecera: escribir en el buscador filtra según se escribe,
  sin pulsar nada, y «Solo gratuitos» se combina con la categoría y el nivel.
  Cada tarjeta dice «Gratis» o su precio antes de abrirla.

**3. El alta de emprendedor, de punta a punta.** «Publicar mi negocio», en la
portada:

| Paso | Qué enseñar |
|---|---|
| 1 · Cuenta | Enviar con la casilla sin marcar: el foco salta al campo que falla |
| 2 · Negocio | El contador de la descripción; a 79 caracteres no deja avanzar |
| 3 · Escaparate | Añadir dos productos, con el precio en pesos |
| 4 · Fotos | Subir dos o tres. La primera queda marcada como portada (B9) |

Al terminar aterriza en `/mi-negocio` **con el aviso de que está en revisión**.
El negocio nace `PENDIENTE` (B6): buscarlo en el directorio no lo encuentra, y su
identificador responde `404`. **No es un fallo, es la regla**, y conviene decirlo
antes de que lo pregunten.

**4. Aprobarlo, con el administrador.** Cerrar sesión y entrar en `/entrar` con
la cuenta del administrador: **aterriza directamente en el panel**, y la cabecera
enseña «Admin».

| Qué hacer | Qué se ve |
|---|---|
| Pestaña «Negocios» | La cola, la más antigua primero, con cuántos esperan |
| «Revisar» el negocio recién creado | **La vista previa**: su perfil tal como se publicará, con las fotos que aún nadie ha aprobado |
| «Rechazar» sin escribir nada | El motivo es obligatorio: el diálogo no se cierra |
| Cancelar y «Aprobar y publicar» | Vuelve a la cola con el aviso, y hay uno menos |
| Abrir Mailpit en <http://localhost:8025> | **El correo de aviso a la dueña** ya está ahí (I1-ter) |

Como el negocio todavía no estaba aprobado, **sus fotos se publican con él**: no
hay que aprobar nada más.

**5. Volver a la web y recargar el directorio.** El negocio recién creado ya
aparece, con su foto de portada, y las cifras de la portada han subido. Con eso
el círculo se cierra: se registró, se moderó y se publicó, todo desde el
navegador.

**6. Opinar sobre un negocio, con la cuenta de clienta.** Entrar en `/entrar` y
abrir un negocio que **no** sea el suyo:

| Qué hacer | Qué se ve |
|---|---|
| Mirar las opiniones sin sesión | Se leen igual: la lista es pública (C1) |
| Calificar con tres estrellas y escribir la reseña | Aparece arriba de la lista **sin recargar**, y la nota de la ficha cambia en la misma pantalla (C4) |
| Cambiarla a cinco estrellas | La fila se actualiza y queda marcada como «Editada» |
| Borrarla | Pide confirmación, y al aceptar vuelve el formulario de publicar |
| Abrir el negocio propio con la cuenta de la emprendedora | **No hay formulario**: el dueño no opina sobre lo suyo (A4) |
| «Denunciar» la opinión de otra persona | Se elige el motivo de una lista cerrada (C6). La opinión **sigue publicada** hasta que decida el administrador |

> Si era la única opinión del negocio, al borrarla la ficha vuelve a decir «Sin
> opiniones» y **no «0,0»**. Es C5, y merece nombrarlo: son dos cosas distintas.

**7. Recuperar una contraseña, viendo llegar el correo.** Es el recorrido que
cruza las dos mitades y el servidor de correo:

1. En `/entrar`, **«¿Olvidaste tu contraseña?»**.
2. Escribir el correo de la clienta. Sale siempre el mismo mensaje —«si ese
   correo tiene una cuenta, le acabamos de enviar un enlace»— **exista o no**:
   decir lo contrario convertiría el formulario en una lista de qué direcciones
   están registradas.
3. Abrir **Mailpit en <http://localhost:8025>**. El correo está ahí, con su
   enlace. Conviene enseñarlo en pantalla: es la prueba de que el correo sale de
   verdad y no es un `log`.
4. Abrir el enlace, escribir la contraseña nueva dos veces y guardar. Lleva al
   login con el aviso, y entra con ella.
5. **Volver a abrir el mismo enlace**: dice que ya no vale, sin enseñar el
   formulario. Sirve una sola vez y caduca a los 30 minutos.

> Probarlo con la cuenta de la clienta y no con la del administrador: si algo
> sale mal, la moderación del paso 4 sigue estando disponible.

**8. El resto del panel, con el administrador.** Cada pestaña es una cola de
trabajo, y todo lo que se decide queda escrito:

| Pestaña | Qué enseñar |
|---|---|
| Cambios | La foto subida al negocio ya aprobado espera aquí, con lo de ahora al lado de lo propuesto. Aprobarla la publica |
| Denuncias | La opinión denunciada en el paso 6, entera. Borrarla pide un motivo y **recalcula el promedio** del negocio |
| Usuarios | Suspender una cuenta pide confirmación; su dueño ya no puede entrar y le llega un correo. Reactivarla |
| Cursos | Crear uno de pago y publicado: sale al momento en el catálogo público. Pasarlo a borrador lo retira |
| Historial | Todo lo anterior, con quién lo hizo y cuándo, filtrable por fechas. Es de solo lectura (L) |

### Si algo falla en directo

| Síntoma | Qué es | Qué hacer |
|---|---|---|
| La web carga pero no hay datos | El backend todavía no responde | `docker compose logs -f backend` y esperar |
| Las fichas salen sin foto | Se subieron **después** de aprobar y esperan revisión | Aprobarlas en el panel, pestaña «Cambios» |
| El negocio nuevo no sale | Está `PENDIENTE`. Es lo correcto | Aprobarlo en el panel, pestaña «Negocios» |
| Un puerto ya está ocupado | Hay otro backend o otro Vite corriendo en el equipo | Pararlo, o `API_PORT=8081 docker compose up -d` |
| Tras un fallo de puerto, sigue sin ir | El contenedor quedó creado **sin red**: ni publica puertos ni resuelve `postgres` | `docker compose up -d --force-recreate backend`. Un `up -d` a secas solo lo arranca |
| Todas las fotos rotas | La base apunta a ficheros que no están en `backend/uploads/` | Recuperar el directorio; la base y el disco van por separado |
| El correo no llega a Mailpit | El backend arrancó sin el servicio de correo delante | `docker compose up -d mailpit` y repetir. La petición sigue devolviendo `200`: el fallo solo está en el log |

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

**El contrato completo, con los 67 endpoints y un recorrido de demostración de
punta a punta, está en [docs/api.md](docs/api.md).**

## Ver las peticiones según llegan

`docker compose logs -f backend` enseña el arranque y los errores, pero **no las
peticiones**: Spring Boot no las registra por defecto, y eso despista. Para que
salgan, añade al servicio `backend`:

```yaml
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB: DEBUG
```

Y filtra, porque son unas seis líneas por petición:

```bash
docker compose logs -f backend | grep DispatcherServlet
```

Quedan dos por petición: `GET "/api/v1/directorio"` y `Completed 200 OK`. En
PowerShell el filtro es `| Select-String DispatcherServlet`.

## OpenAPI y Swagger

**La especificación es [`openapi.json`](openapi.json), en la raíz del
repositorio**: OpenAPI 3.1, con las 67 operaciones, sus parámetros, sus cuerpos y
todos sus códigos de respuesta, los de error incluidos, y el JWT declarado en
`components/securitySchemes` y aplicado solo en las operaciones protegidas.

Con el backend levantado, la API se explora y se prueba desde el navegador:

| Dirección | Qué es |
|---|---|
| <http://localhost:8080/swagger-ui.html> | La interfaz, con los 67 endpoints agrupados |
| <http://localhost:8080/v3/api-docs> | El mismo documento, en vivo |

Las dos son **públicas**: describen el contrato, que ya es público, y no
devuelven ni un dato. Para probar lo protegido, `POST /api/v1/auth/login` y
pegar el `token` de la respuesta en **Authorize**, arriba a la derecha; no hace
falta escribir «Bearer».

`openapi.json` lo genera springdoc desde los controladores y se exporta con un
script. Validarlo y contrastarlo con la API no instala nada en el proyecto:

```bash
./scripts/exportar-openapi.sh                        # vuelve a escribir openapi.json (API levantada)
npx --yes @redocly/cli@2.63.0 lint openapi.json      # válido según el estándar
python3 scripts/comprobar-openapi.py                 # ni 404 de ruta ni 405; el candado, el declarado
```

`redocly lint` da por válido el fichero y deja ocho avisos a sabiendas: seis
operaciones sin ningún 4xx —los catálogos y las cifras de la portada, que no
fallan—, el proyecto sin licencia y el servidor en `localhost`, que es donde
corre. Qué se anota y dónde está en
[docs/arquitectura.md](docs/arquitectura.md).

## Colección de Postman

`backend/postman/EmprendeHub.postman_collection.json`, con **78 peticiones que cubren los
67 endpoints**.

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

Las pruebas que necesitan base de datos levantan su propio PostgreSQL con
Testcontainers, así que Docker tiene que estar corriendo. No usan la base de
`docker compose`: pasan sin nada escuchando en el 5433.

**515 pruebas en verde y 98,4% de cobertura sobre `service/**`**, muy por encima
del 80% que exige la rúbrica. Repartidas en los tres niveles del taller:

| Nivel | Herramienta | Qué prueba |
|---|---|---|
| Unitario | Mockito | Los 17 servicios, con el repositorio simulado |
| Repositorio | `@DataJpaTest` + Testcontainers | Las consultas contra PostgreSQL real |
| Controlador | `@WebMvcTest` + MockMvc | Rutas, códigos y forma del JSON |

Además, `SeguridadAccesoTest` y `SeguridadHttpRealTest` verifican la matriz de
acceso completa, esta última con el servidor levantado.

- Informe de pruebas: `build/reports/tests/test/index.html`
- Informe de cobertura: `build/reports/jacoco/test/html/index.html`

**La cobertura de `service/**` tiene que quedarse en el 80% o más**: por debajo,
el build falla. Ejecutar una sola clase con `--tests` también lo hace fallar,
porque la cobertura se mide sobre el paquete entero; para eso usa `cd backend && ./gradlew build`.

### Verificar el frontend

Las pruebas del frontend son de **Vitest con jsdom**, un navegador simulado: no
abren ninguna ventana ni necesitan el backend, porque `fetch` se sustituye por
respuestas preparadas. Cubren el servicio de autenticación, el interceptor que
adjunta el token y el CRUD del escaparate. Necesitan Node, no Docker.

```bash
cd frontend
npm ci             # solo la primera vez: instala las dependencias del package-lock
npm test           # las pruebas, en una sola pasada
npm run build      # tipos y compilación: 0 errores y 0 advertencias
npm run contraste  # las 21 combinaciones de la paleta contra WCAG 2.2
```

`npm run contraste` lee los colores de `src/estilos/tokens.css` y falla si alguna
combinación baja de su mínimo: 4,5:1 en texto y 3:1 en bordes de control. Si se
añade un token, se añade su fila. **No mide las mezclas con `color-mix()`**, y ahí
hay cinco insignias pendientes que no llegan al mínimo: están listadas en
[docs/pruebas.md](docs/pruebas.md#el-contraste-de-los-tintes-no-lo-mide-ningún-script).

Y lo que ningún script sustituye: **abrirlo en el navegador**, mirarlo a 360 px
de ancho y recorrerlo con el tabulador. El detalle está en
[docs/pruebas.md](docs/pruebas.md#las-pruebas-del-frontend).

## Configuración

Valores por defecto en `backend/src/main/resources/application.yml`, todos sustituibles
por variables de entorno:

| Variable | Por defecto |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/emprendehub` |
| `DB_USER` / `DB_PASSWORD` | **Sin valor por defecto.** Se eligen en el `.env` (ver [Arrancar](#arrancar)) |
| `PORT` | `8080` |
| `JWT_SECRET` | Clave de desarrollo, sustituir en cualquier despliegue |
| `JWT_EXPIRATION` | `3600000` (una hora) |
| `ADMIN_CORREO` / `ADMIN_CONTRASENA` | `admin@emprendehub.co` / `admin12345` |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` (Mailpit) |
| `MAIL_FROM` | `no-responder@emprendehub.co` |
| `APP_URL` | `http://localhost:5173`, que es lo que lleva el enlace del correo |
| `RECUPERACION_MINUTOS` | `30`, lo que vive un enlace de recuperación |
| `SEMILLA_DEMO` | `true`: siembra los [datos de prueba](#datos-de-prueba) la primera vez, con la base vacía |

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

Y dentro del frontend:

```
frontend/src
├── types/        interfaces y uniones del dominio
├── api/          lo que habla con el backend — el único sitio con fetch
├── estado/       un contexto por dominio
├── componentes/  piezas reutilizables, con su módulo CSS al lado
├── paginas/      una por ruta
└── estilos/      tokens.css manda: ningún color se escribe fuera
```

La única dependencia añadida a la plantilla de Vite es `react-router-dom`. El
porqué está en [docs/arquitectura.md](docs/arquitectura.md#el-frontend).

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
| [docs/plan-de-entrega-frontend-fase-2.md](docs/plan-de-entrega-frontend-fase-2.md) | Los 6 de la fase 2: opiniones y recuperar la contraseña |
