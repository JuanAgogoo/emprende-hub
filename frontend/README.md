# Frontend

React + Vite + TypeScript. Consume la API del backend, que vive en
[`../backend`](../backend).

## Arrancar

Tres pasos: instalar las dependencias, decir dónde está el backend y ejecutar.
**Necesita el backend corriendo**, porque en desarrollo el servidor de Vite hace
de proxy hacia él.

### 1. Instalar las dependencias

```bash
cd frontend && npm install
```

### 2. Configurar la URL del backend

Por defecto **no hay que configurar nada**: el frontend pide a `/api/v1`, que es
una ruta relativa, y el proxy de Vite la reenvía a `http://localhost:8080`. Con
el backend en el mismo equipo y en su puerto de siempre, funciona tal cual.

Si el backend está en otro sitio, hay dos variables y **no se tocan las dos**:

| Variable | Por defecto | Cuándo se cambia | Dónde se pone |
|---|---|---|---|
| `VITE_PROXY_TARGET` | `http://localhost:8080` | El backend está en otra máquina u otro puerto, y se sigue usando el proxy | Variable de entorno al arrancar Vite, que es lo que hace `docker-compose.yml` |
| `VITE_API_URL` | `/api/v1` | Solo si se quiere llamar al backend **sin** el proxy, con su dirección completa | `frontend/.env`, que Vite lee al arrancar |

```bash
# Con el proxy, que es lo normal en desarrollo:
VITE_PROXY_TARGET=http://192.168.1.50:8080 npm run dev
```

> **`VITE_API_URL` deja al frontend fuera del proxy, y entonces hace falta CORS**,
> que el backend entregado no configura. Salvo que se sepa que el backend lo
> permite, la opción buena es la primera. El detalle está en
> [Por qué no hay CORS](#por-qué-no-hay-cors).

### 3. Ejecutar

```bash
npm run dev     # http://localhost:5173
```

### Todo junto, con Docker

Desde la raíz del repositorio, lo más corto —levanta también el backend, la base
y el correo, con recarga en caliente—:

```bash
docker compose up -d
```

O a mano, con el frontend en el host:

```bash
docker compose up -d postgres mailpit   # base y correo, desde la raíz
cd backend && ./gradlew bootRun         # API en el 8080
cd frontend && npm run dev              # la web en el 5173
```

Mailpit recoge los correos de la recuperación de contraseña y los enseña en
<http://localhost:8025>. Sin él, el enlace no llega a ninguna parte.

Los dos modos usan el mismo puerto, así que **no se pueden tener a la vez**.

| Orden | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga en caliente |
| `npm run build` | Comprueba los tipos y compila. **0 errores y 0 advertencias** |
| `npm run contraste` | Verifica la paleta contra WCAG 2.2. Falla si algo baja del mínimo |
| `npm run preview` | Sirve lo compilado, para mirar el resultado real |

## Las pantallas

Las rutas, todas declaradas en `src/App.tsx`: las nueve de la fase 1, las dos
que añade la recuperación de contraseña, el catálogo de cursos y el panel de
administración.

| Ruta | Pantalla | Quién entra |
|---|---|---|
| `/` | Portada: cifras, destacados y buscador | Cualquiera |
| `/directorio` | Listado con seis filtros, ordenación y paginación | Cualquiera |
| `/negocios/:id` | Perfil público: galería, escaparate y redes | Cualquiera |
| `/cursos` | Catálogo de cursos: búsqueda según se escribe, categoría, nivel y «solo gratuitos» | Cualquiera |
| `/entrar` | Un solo acceso que deriva según el rol | Cualquiera |
| `/registro` | Alta de cliente: el formulario corto | Cualquiera |
| `/registro-emprendedor` | El asistente de cuatro pasos | Cualquiera |
| `/recuperar` | Pide el correo y manda el enlace | Cualquiera |
| `/recuperar/:token` | Elegir la contraseña nueva, desde el enlace del correo | Quien tenga el enlace |
| `/mi-negocio` | El negocio propio: sus cifras, sus avisos y su buzón | `EMPRENDEDOR` |
| `/admin` | El panel: abre la primera cola, la de negocios | `ADMIN` |
| `/admin/negocios` | Negocios esperando su primera revisión, los más antiguos primero | `ADMIN` |
| `/admin/negocios/:id` | Vista previa del perfil, con sus fotos sin aprobar, y la decisión | `ADMIN` |
| `/admin/cambios` | Ediciones de negocios ya publicados: lo de ahora al lado de lo propuesto | `ADMIN` |
| `/admin/denuncias` | Opiniones denunciadas, con el texto entero: borrarla o desestimar | `ADMIN` |
| `/admin/usuarios` | Las cuentas con su correo, rol y estado; suspender y reactivar | `ADMIN` |
| `/admin/cursos` | Los cursos, borradores incluidos: publicar, retirar y eliminar | `ADMIN` |
| `/admin/cursos/nuevo` | Alta de un curso, como borrador o ya publicado | `ADMIN` |
| `/admin/cursos/:id` | Editar un curso, también su estado | `ADMIN` |
| `/admin/historial` | Todas las acciones de moderación, filtrables por fechas. Solo lectura | `ADMIN` |
| `/tratamiento-de-datos` | Texto legal | Cualquiera |
| `/informacion-personal` | Texto legal | Cualquiera |

El perfil público lleva además dos cosas que no son rutas aparte, porque viven
dentro de `/negocios/:id`:

- **Las opiniones**: se leen sin sesión, y con sesión se califica con estrellas,
  se escribe la reseña y se edita o se borra la propia. Las ajenas se pueden
  **denunciar** con un motivo de la lista cerrada (C6); siguen publicadas hasta
  que el administrador decide.
- **Escribirle al negocio**: con sesión, asunto y mensaje van a su buzón. El
  nombre y el correo los pone la cuenta, no un campo del formulario.

Y `/mi-negocio` es el otro lado de eso: **las cifras** del negocio —visitas de
la semana y del mes con su variación, consultas recibidas y calificación, más la
**gráfica de visitas por día**, que alterna entre semana y mes sin volver a
pedir nada—, los
**avisos** de la persona y el **buzón**, con el correo de quien escribe para
poder responderle. La plataforma no responde desde dentro (D2), así que cada
consulta lleva su enlace de correo.

El **panel de administración** es una ruta por cola, con pestañas encima. Quien
entra como administrador aterriza en él y la cabecera le enseña el enlace
«Admin». Aprobar y rechazar solo se puede desde la vista previa, no desde la
lista: así no se publica nada sin haberlo mirado. Rechazar pide el motivo en un
diálogo, y el dueño lo recibe en su panel y por correo (I1-ter).

> `/mi-negocio` y `/admin` comprueban el rol **por comodidad de la interfaz, no
> por seguridad**. Quien mande la petición a mano se topa igual con el backend, que
> es donde se comprueba de verdad.

### El asistente de registro

Los tres primeros pasos recogen datos en el navegador y **se envían juntos** en
una sola petición: cuenta, negocio y escaparate entran en una transacción, así
que un fallo a mitad no deja el correo ocupado. El cuarto son las fotos, que van
aparte porque son binarios y tienen su propio endpoint `multipart`.

Al terminar el paso 3 el negocio ya existe y la sesión está abierta, por eso
desde el paso 4 no se puede volver atrás.

## Por qué no hay CORS

El backend no lo configura, así que `vite.config.ts` reenvía `/api/v1` y
`/fotos` al 8080 y todo sale del mismo origen. **Las fotos también**: sin
proxearlas, las imágenes no cargan.

A dónde reenvía sale de `VITE_PROXY_TARGET`, con `http://localhost:8080` por
defecto. Dentro de Docker `localhost` sería el propio contenedor del frontend,
así que docker-compose le pone el nombre del servicio: `http://backend:8080`.

Esto vale en desarrollo. Si algún día el frontend se sirve compilado desde otro
origen, habrá que añadir CORS en `SecurityConfig`.

## Cómo está organizado

```
src/
├── api/          Todo lo que habla con el backend. El único sitio con fetch
├── componentes/  Piezas reutilizables, con su módulo CSS al lado
├── estado/       Un contexto por dominio (aparece con el login)
├── estilos/      tokens.css manda: ningún color se escribe fuera
├── paginas/      Una por ruta
└── types/        Interfaces y uniones del dominio
```

Las reglas de estilo visual están en [`../docs/diseno.md`](../docs/diseno.md) y
los planes de incrementos en
[`../docs/plan-de-entrega-frontend-fase-1.md`](../docs/plan-de-entrega-frontend-fase-1.md)
y
[`../docs/plan-de-entrega-frontend-fase-2.md`](../docs/plan-de-entrega-frontend-fase-2.md).

## Antes de dar un incremento por terminado

```bash
npm run build                                    # 0 errores, 0 advertencias
npm run contraste                                # 21 de 21
grep -rn ': any\|as any' src/                    # vacío
grep -rnE '#[0-9a-fA-F]{3,8}|rgb\(' src --include=*.css | grep -v tokens.css   # vacío
```

Y lo que ningún script comprueba: abrirlo en el navegador, mirarlo **a 360px de
ancho** y recorrer la página entera con el tabulador.

## La tipografía

Manrope Variable, en `public/fuentes/`. Se sirve desde el propio proyecto, no
desde Google Fonts: son 25 KB, cubren de 400 a 800 en un solo fichero y no
dependen de que un tercero responda.
