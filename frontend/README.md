# Frontend

React + Vite + TypeScript. Consume la API del backend, que vive en
[`../backend`](../backend).

## Arrancar

Necesita **el backend corriendo**, porque en desarrollo el servidor de Vite hace
de proxy hacia él. Lo más corto, desde la raíz del repositorio:

```bash
docker compose up -d              # base, API y web, con recarga en caliente
```

O en el host, si prefieres no pasar por Docker para el frontend:

```bash
docker compose up -d postgres     # solo la base, desde la raíz
cd backend && ./gradlew bootRun   # API en el 8080
cd frontend && npm install        # solo la primera vez
npm run dev                       # http://localhost:5173
```

Los dos modos usan el mismo puerto, así que **no se pueden tener a la vez**.

| Orden | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga en caliente |
| `npm run build` | Comprueba los tipos y compila. **0 errores y 0 advertencias** |
| `npm run contraste` | Verifica la paleta contra WCAG 2.2. Falla si algo baja del mínimo |
| `npm run preview` | Sirve lo compilado, para mirar el resultado real |

## Las pantallas

Las nueve rutas de la fase 1, todas declaradas en `src/App.tsx`:

| Ruta | Pantalla | Quién entra |
|---|---|---|
| `/` | Portada: cifras, destacados y buscador | Cualquiera |
| `/directorio` | Listado con seis filtros, ordenación y paginación | Cualquiera |
| `/negocios/:id` | Perfil público: galería, escaparate y redes | Cualquiera |
| `/entrar` | Un solo acceso que deriva según el rol | Cualquiera |
| `/registro` | Alta de cliente: el formulario corto | Cualquiera |
| `/registro-emprendedor` | El asistente de cuatro pasos | Cualquiera |
| `/mi-negocio` | El negocio propio, **solo de consulta** | `EMPRENDEDOR` |
| `/tratamiento-de-datos` | Texto legal | Cualquiera |
| `/informacion-personal` | Texto legal | Cualquiera |

Lo que **no** entra en esta fase, para que nadie lo busque: el panel de
administración, el dashboard de gestión —editar, buzón, visitas—, los cursos, el
listado de opiniones y recuperar la contraseña, que no existe en el backend.

> `/mi-negocio` comprueba el rol **por comodidad de la interfaz, no por
> seguridad**. Quien mande la petición a mano se topa igual con el backend, que
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
el plan de incrementos en
[`../docs/plan-de-entrega-frontend-fase-1.md`](../docs/plan-de-entrega-frontend-fase-1.md).

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
