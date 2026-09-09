# Frontend

React + Vite + TypeScript. Consume la API del backend, que vive en
[`../backend`](../backend).

## Arrancar

Necesita **el backend corriendo**, porque en desarrollo el servidor de Vite hace
de proxy hacia él:

```bash
docker compose up -d              # PostgreSQL, desde la raíz
cd backend && ./gradlew bootRun   # API en el 8080
cd frontend && npm install        # solo la primera vez
npm run dev                       # http://localhost:5173
```

| Orden | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga en caliente |
| `npm run build` | Comprueba los tipos y compila. **0 errores y 0 advertencias** |
| `npm run contraste` | Verifica la paleta contra WCAG 2.2. Falla si algo baja del mínimo |
| `npm run preview` | Sirve lo compilado, para mirar el resultado real |

## Por qué no hay CORS

El backend no lo configura, así que `vite.config.ts` reenvía `/api/v1` y
`/fotos` al 8080 y todo sale del mismo origen. **Las fotos también**: sin
proxearlas, las imágenes no cargan.

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
