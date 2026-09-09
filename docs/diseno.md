# Sistema de diseño

Las reglas visuales del frontend. Manda sobre cualquier criterio estético
improvisado dentro de un incremento: si algo no está aquí, se añade aquí primero
y se usa después.

Está subordinado a `agent-docs/estilo/00-punto-dulce.md`. Todo lo que sigue se
resuelve **con CSS estándar y sin una sola dependencia de estilo**: nada de
Tailwind, styled-components, Emotion ni sistemas de diseño de terceros.

---

## Los cinco principios

1. **Mobile first, y no de boquilla.** Se escribe primero el CSS del móvil, sin
   media query. Las consultas solo añaden a partir de ahí, nunca deshacen. Si un
   estilo hay que corregirlo en `min-width`, es que estaba mal planteado abajo.
2. **Un color no se escribe dos veces.** Ningún valor de color aparece fuera de
   `tokens.css`. Ni uno. La regla se verifica por script, no por confianza.
3. **Legible antes que bonito.** Cuerpo de 17px en móvil, 4,5:1 de contraste
   mínimo en todo lo que sea texto, 44px de área táctil. Estas tres cifras no se
   negocian por razones de composición.
4. **Innovar en la técnica, no en la cantidad de efectos.** Lo que distingue a
   este frontend son `:has()`, container queries, transiciones de vista y
   animaciones dirigidas por scroll: CSS nativo que casi nadie usa todavía.
   No es un carrusel más, ni un parallax, ni partículas.
5. **Nada de relleno genérico.** Sin degradados morados, sin tarjetas de cristal
   flotando sobre manchas de color, sin iconos de línea genéricos rellenando
   huecos, sin texto de relleno donde debería ir un dato real. Si una sección no
   tiene contenido que justificarla, se quita la sección.

---

## Color

### Los tokens, y solo los tokens

```css
/* estilos/tokens.css — el único fichero del proyecto con valores de color */
:root {
  /* Marca */
  --color-acento:        #0F766E;  /* acciones primarias, enlaces, foco */
  --color-acento-fuerte: #0B5B55;  /* hover y activo del primario */
  --color-acento-suave:  #ECFDF5;  /* fondos de realce, chips activos */

  /* Texto */
  --color-tinta:         #0F172A;  /* titulares */
  --color-cuerpo:        #475569;  /* párrafos */
  --color-apagado:       #64748B;  /* secundario, placeholder, metadatos */

  /* Superficies */
  --color-lienzo:        #FFFFFF;  /* fondo de tarjetas y de página */
  --color-lienzo-alt:    #F8FAFC;  /* franjas alternas, campos en reposo */

  /* Líneas */
  --color-borde:         #E2E8F0;  /* separadores y bordes decorativos */
  --color-borde-control: #64748B;  /* borde de campos y controles */

  /* Señales */
  --color-exito:         #15803D;
  --color-aviso:         #A16207;
  --color-error:         #B91C1C;
  --color-sobre-señal:   #FFFFFF;  /* texto encima de las tres anteriores */
}
```

**Diecisiete tokens. No hay un decimoctavo sin discutirlo.** Las variantes que
hagan falta salen de `color-mix()`, que es CSS estándar y evita inventar hex:

```css
/* Un fondo al 8% del acento, sin añadir un token nuevo */
background: color-mix(in srgb, var(--color-acento) 8%, white);
```

### Contraste verificado, no supuesto

Las 21 combinaciones que el diseño usa de verdad, calculadas con la fórmula de
luminancia relativa de WCAG 2.2. **Ejecutado, no estimado:**

| Combinación | Ratio | Mín | Nivel |
|---|---|---|---|
| Titular sobre lienzo | 17,85 | 4,5 | AAA |
| Titular sobre lienzo-alt | 17,06 | 4,5 | AAA |
| Cuerpo sobre lienzo | 7,58 | 4,5 | AAA |
| Cuerpo sobre lienzo-alt | 7,24 | 4,5 | AAA |
| Secundario y placeholder | 4,76 | 4,5 | AA |
| Secundario sobre lienzo-alt | 4,55 | 4,5 | AA |
| Texto de botón primario | 5,47 | 4,5 | AA |
| Botón primario en hover | 7,95 | 4,5 | AAA |
| Enlace y botón fantasma | 5,47 | 4,5 | AA |
| Acento sobre acento-suave | 5,20 | 4,5 | AA |
| Acento sobre lienzo-alt | 5,23 | 4,5 | AA |
| Texto sobre éxito | 5,02 | 4,5 | AA |
| Texto sobre error | 6,47 | 4,5 | AA |
| Texto sobre aviso | 4,92 | 4,5 | AA |
| Error de formulario | 6,47 | 4,5 | AA |
| Mensaje de éxito | 5,02 | 4,5 | AA |
| Mensaje de aviso | 4,92 | 4,5 | AA |
| Borde de campo (1.4.11) | 4,76 | 3,0 | AA |
| Borde de campo sobre gris | 4,55 | 3,0 | AA |
| Anillo de foco (1.4.11) | 5,47 | 3,0 | AA |
| Anillo de foco sobre gris | 5,23 | 3,0 | AA |

**21 de 21.** El script vive en `frontend/scripts/contraste.mjs` y se ejecuta con
`npm run contraste`. Si alguien añade un token, añade su fila; si el script falla,
el incremento no se cierra.

**Dos cosas que costaron una corrección y conviene no repetir:**

- **El borde de los campos no puede ser gris claro.** El `#E2E8F0` que se usa
  para separadores da 1,23:1 y **incumple** la regla 1.4.11 de WCAG, que exige
  3:1 en los bordes que identifican un control. Por eso hay dos tokens de línea:
  `--color-borde` para lo decorativo y `--color-borde-control` para lo que el
  usuario tiene que poder ver que es un campo. Es el fallo de accesibilidad más
  común en las interfaces minimalistas bonitas, y la referencia adjunta lo tiene.
- **El color nunca es la única señal.** Un campo con error lleva borde rojo,
  icono y texto; un producto agotado lleva etiqueta, no solo un tono más pálido.

### Cómo se reparte

El acento **no se usa para decorar**. Marca lo accionable: el botón primario de
cada pantalla, los enlaces, el paso activo del asistente, el chip de filtro
puesto y el anillo de foco. Una pantalla con seis cosas en verde es una pantalla
sin jerarquía.

Regla práctica: **un solo botón primario por vista**. Lo demás es secundario
(borde y texto en acento, fondo transparente) o terciario (solo texto).

---

## Tipografía

### La familia

**Manrope Variable**, autoalojada en `frontend/public/fuentes/` como un único
`.woff2`. No entra por Google Fonts —una petición a un tercero que puede tardar o
fallar— ni por un paquete de npm: es un fichero y una regla `@font-face`.

```css
@font-face {
  font-family: 'Manrope';
  src: url('/fuentes/manrope-variable.woff2') format('woff2-variations');
  font-weight: 400 800;
  font-display: swap;          /* el texto se lee mientras la fuente carga */
}

:root {
  --fuente: 'Manrope', system-ui, -apple-system, 'Segoe UI', sans-serif;
}
```

Se eligió sobre Inter porque Inter es la tipografía por defecto de medio internet
y de casi todo lo generado automáticamente; Manrope tiene algo de carácter
geométrico sin perder legibilidad a 15px. Es variable, así que **un solo fichero
cubre de 400 a 800** y no hay que descargar un peso por variante.

### La escala

Ocho pasos, fluidos con `clamp()`: crecen con el ancho sin una sola media query.
El primer número es el tamaño en móvil (360px) y el segundo en escritorio.

```css
:root {
  --texto-xs:  0.8125rem;                            /* 13px  fijo */
  --texto-sm:  0.9375rem;                            /* 15px  fijo */
  --texto-md:  1.0625rem;                            /* 17px  fijo — el cuerpo */
  --texto-lg:  clamp(1.1875rem, 1.1rem + 0.4vw, 1.375rem);   /* 19 → 22 */
  --texto-xl:  clamp(1.375rem,  1.2rem + 0.8vw, 1.75rem);    /* 22 → 28 */
  --texto-2xl: clamp(1.6875rem, 1.4rem + 1.3vw, 2.25rem);    /* 27 → 36 */
  --texto-3xl: clamp(2.125rem,  1.7rem + 2.1vw, 3rem);       /* 34 → 48 */

  --alto-apretado: 1.15;   /* titulares grandes */
  --alto-normal:   1.35;   /* titulares pequeños */
  --alto-comodo:   1.6;    /* párrafos */
}
```

| Uso | Token | Peso | Alto | Espaciado |
|---|---|---|---|---|
| Titular de portada | `--texto-3xl` | 800 | apretado | −0.02em |
| Titular de página | `--texto-2xl` | 700 | apretado | −0.015em |
| Titular de sección | `--texto-xl` | 700 | normal | −0.01em |
| Titular de tarjeta | `--texto-lg` | 600 | normal | 0 |
| **Cuerpo** | `--texto-md` | 400 | cómodo | 0 |
| Cuerpo secundario | `--texto-sm` | 400 | cómodo | 0 |
| Etiqueta y metadato | `--texto-xs` | 500 | normal | 0.01em |

**Reglas que no se saltan:**

- **13px es el suelo.** No hay texto más pequeño en toda la aplicación, ni en
  pies de foto, ni en avisos legales, ni en las etiquetas de los campos.
- **El cuerpo es 17px y no encoge en móvil.** Es el punto dulce: por debajo de 16
  se lee mal, por encima de 18 obliga a hacer scroll por todo.
- **Los campos de formulario, 16px como mínimo.** Con menos, Safari en iOS hace
  zoom automático al enfocar y descoloca la pantalla. Es la razón técnica; la
  cifra sale de ahí, no del gusto.
- **Máximo 70 caracteres por línea** en textos largos: `max-width: 65ch`.
- **Solo tres pesos**: 400 cuerpo, 600 subtítulos, 700–800 titulares.

---

## Espaciado, formas y elevación

Escala de espaciado de base 4, que es lo que hace que las pantallas se vean
ordenadas sin pensarlo:

```css
:root {
  --e-1: 0.25rem;  --e-2: 0.5rem;   --e-3: 0.75rem;  --e-4: 1rem;
  --e-5: 1.5rem;   --e-6: 2rem;     --e-7: 3rem;     --e-8: 4rem;

  --radio-sm: 8px;    /* chips, etiquetas */
  --radio-md: 12px;   /* campos, botones */
  --radio-lg: 16px;   /* tarjetas */
  --radio-full: 999px;/* píldoras y avatares */

  /* Sombras de un solo tono, sutiles. Nada de sombras de color */
  --sombra-sm: 0 1px 2px rgb(15 23 42 / 0.04);
  --sombra-md: 0 4px 12px rgb(15 23 42 / 0.06);
  --sombra-lg: 0 12px 32px rgb(15 23 42 / 0.10);

  --ancho-max: 72rem;      /* contenedor de página */
  --ancho-lectura: 65ch;   /* textos largos */
}
```

La elevación tiene tres niveles y significan algo: `sm` para tarjetas en reposo,
`md` al pasar por encima o para elementos fijos, `lg` solo para lo que flota
encima del contenido. **Una tarjeta no sube dos niveles al hacer hover.**

---

## Mobile first

```css
:root {
  --bp-tablet:  48rem;   /*  768px */
  --bp-escrit:  64rem;   /* 1024px */
  --bp-ancho:   80rem;   /* 1280px */
}
```

Solo tres puntos de ruptura y **siempre `min-width`**. El CSS base es el del
móvil y no lleva ninguna consulta.

Donde el componente deba adaptarse a su hueco y no a la ventana —la tarjeta de
negocio, que aparece en la portada, en el directorio y en el asistente— se usan
**container queries**, no puntos de ruptura de página:

```css
.tarjeta-negocio { container-type: inline-size; }

@container (min-width: 22rem) {
  .tarjeta-negocio__cuerpo { display: grid; grid-template-columns: 8rem 1fr; }
}
```

Es la técnica que hace que un componente sea de verdad reutilizable, es CSS
estándar desde 2023 y se explica en una frase: *«la tarjeta se adapta al espacio
que le dan, no al tamaño de la pantalla»*.

**Lo que hay que comprobar en móvil, en cada incremento:**

- A 360px de ancho nada desborda en horizontal.
- Todo lo pulsable mide **44×44px** como mínimo, con `min-height: 44px` en
  botones y campos.
- Las acciones principales quedan al alcance del pulgar, en la mitad inferior.
- Los filtros del directorio no son una barra lateral encogida: en móvil se
  pliegan sobre los resultados y a partir de tablet pasan a su propia columna.

  Se resuelve con `<details>`, no con una hoja modal. La hoja obligaba a manejar
  a mano el foco atrapado, la tecla Escape y el fondo: JavaScript que nadie pidió
  para un desplegable. `<details>` da lo mismo con teclado incluido y sin una
  línea de guion. **Cuidado con una trampa**: al ocultar el `summary` en
  escritorio hay que forzar la visibilidad del contenido, o quien lo pliegue en
  móvil y luego ensanche la ventana se queda sin poder abrirlo.
- El asistente de registro muestra **un campo por fila**. Las dos columnas de la
  referencia son de escritorio.

---

## Componentes

Se definen aquí una vez y se reutilizan. Cada uno vive en su
`componentes/<Nombre>/` con su módulo CSS.

### Botones

| Variante | Fondo | Texto | Borde | Cuándo |
|---|---|---|---|---|
| Primario | `--color-acento` | blanco | ninguno | La acción de la vista. **Solo uno** |
| Secundario | transparente | `--color-acento` | `--color-acento` | Acción alternativa |
| Terciario | transparente | `--color-cuerpo` | ninguno | «Volver», «Omitir» |
| Peligro | `--color-error` | blanco | ninguno | Solo destructivo |

Estados obligatorios en los cuatro: reposo, hover, **foco visible**, activo,
deshabilitado y **cargando**. El de cargando importa más de lo que parece: sin él
la gente pulsa dos veces y se registra dos veces.

```css
.boton:focus-visible {
  outline: 2px solid var(--color-acento);
  outline-offset: 2px;
}
```

Nunca `outline: none` sin sustituto. Es la primera cosa que se rompe y la que
más se nota con teclado.

### Campos de formulario

Etiqueta siempre visible arriba —**nada de placeholders como etiqueta**, que
desaparecen justo cuando hacen falta—, el placeholder como ejemplo, el error
debajo con icono y texto, y el contador donde haya mínimo o máximo.

El error aparece **solo tras intentar enviar**, que es la traducción del
`markAllAsTouched()` del curso, y el campo se marca con `aria-invalid` y
`aria-describedby` apuntando al mensaje.

`:has()` resuelve el estado del grupo sin una sola clase condicional en el JSX:

```css
.campo:has(input:focus-visible) .campo__etiqueta { color: var(--color-acento); }
.campo:has([aria-invalid='true']) .campo__control { border-color: var(--color-error); }
```

### Tarjeta de negocio

La pieza que más se repite. Foto en 4:3 con `aspect-ratio` y
`object-fit: cover`, nombre, categoría, ciudad, calificación y nivel de precio.

Dos casos que hay que tener resueltos desde el primer día, porque los datos
sembrados los tienen: **negocio sin foto** —marcador de posición con la inicial
sobre `--color-acento-suave`, jamás una imagen rota— y **negocio sin
calificación**, que muestra «Sin opiniones» y no «0,0», porque no tener notas no
es tener malas notas (C5).

### Asistente de pasos

El de la referencia, adaptado: círculos numerados unidos por una línea que se
rellena al avanzar. En móvil, **«Paso 2 de 4» y una barra**, no cuatro círculos
apretados.

El paso completado se marca con un check, no solo con color. El actual lleva
`aria-current="step"`, y el conjunto va en un `<ol>`, que es lo que es.

### Estados de carga y vacío

Cada vista que carga datos tiene sus tres estados dibujados, porque la unión
`EstadoCarga<T>` obliga a tratarlos:

- **Cargando:** esqueletos con la forma del contenido que viene, no un texto
  «Cargando…» ni un spinner centrado.
- **Vacío:** dice qué pasó y qué hacer. «Ningún negocio coincide con estos
  filtros» con un botón para limpiarlos, no una página en blanco.
- **Error:** dice qué falló y ofrece reintentar.

---

## Movimiento

Duraciones cortas y una sola curva:

```css
:root {
  --dur-rapida: 150ms;   /* hover, foco, cambios de color */
  --dur-media:  250ms;   /* entradas, despliegues */
  --dur-lenta:  400ms;   /* transiciones entre vistas */
  --curva: cubic-bezier(0.2, 0, 0, 1);
}
```

**Lo que sí:**

- Hover y foco a 150ms.
- Entrada escalonada de las tarjetas al aparecer en pantalla, con
  `animation-timeline: view()`. Sin JavaScript, sin `IntersectionObserver`.
- `view-transition-name` entre el directorio y el perfil: la foto de la tarjeta
  crece hasta ser la del detalle. Es el efecto que más impresiona por menos
  código, y es una línea de CSS más `startViewTransition`.
- Esqueletos con un barrido suave mientras cargan.

**Lo que no**, y está cerrado: parallax, partículas, cursores personalizados,
scroll secuestrado, pantalla de bienvenida, contadores que suben solos, textos
que se escriben letra a letra.

**Obligatorio en todo lo anterior:**

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

Y una comprobación de soporte para lo moderno: si `view-transition` o
`animation-timeline` no existen en el navegador, la aplicación **funciona igual,
sin la animación**. Se usa `@supports`; ninguna funcionalidad depende de un
efecto.

---

## Accesibilidad: el mínimo que se verifica

No es una sección aparte del diseño, es parte de darlo por terminado.

1. **Contraste**: 4,5:1 en texto, 3:1 en bordes de control y anillos de foco.
   Verificado por script.
2. **Foco visible** en todo lo que se puede enfocar, recorriendo la página entera
   con el tabulador.
3. **Área táctil de 44px** en todo lo pulsable.
4. **HTML con sentido**: `<button>` para acciones, `<a>` para navegar, `<ol>`
   para los pasos, un solo `<h1>` por página y los encabezados sin saltarse
   niveles.
5. **Formularios**: `<label for>` real, `aria-invalid` y `aria-describedby` en
   los errores, y el foco al primer campo inválido al fallar el envío.
6. **Imágenes**: `alt` con el nombre del negocio; `alt=""` en las decorativas.
7. **El color nunca solo**: siempre acompañado de texto, icono o forma.
8. **Zoom al 200%** sin perder contenido ni funciones.

---

## Cómo se aplica a las dos pantallas de referencia

### La portada

Los siete bloques de la infografía, con lo que este proyecto tiene de verdad y
sin inventar nada:

| Bloque | Aquí es | De dónde salen los datos |
|---|---|---|
| 1. Titular | La promesa, con el buscador debajo | — |
| 2. Foco visual | Rejilla de negocios destacados con foto | `GET /directorio/destacados` |
| 3. Beneficios | Tres razones, con las categorías reales | `GET /catalogos/categorias-negocio` |
| 4. Prueba social | Las cuatro cifras calculadas | `GET /estadisticas/portada` |
| 5. Narrativa | Cómo funciona, en tres pasos | — |
| 6. Garantía | Negocios revisados antes de publicarse | — |
| 7. Llamada a la acción | Dos caminos: explorar o registrar mi negocio | — |

La prueba social son **cifras reales, no inventadas** (H4): si la base está vacía
salen a cero, y eso es correcto. **No se ponen testimonios falsos** ni logos de
empresas que no existen. Es la diferencia entre una portada honesta y una
plantilla rellena.

### El asistente de registro

De la referencia se toman la estructura y la contención: paso numerado, tarjeta
con un título y un subtítulo que explica qué se pide, campos con etiqueta
visible, asterisco en los obligatorios, y «Anterior» a la izquierda con
«Siguiente» a la derecha.

Lo que se cambia: **una columna en móvil**, el contador de la descripción a la
vista, el borde de los campos con contraste suficiente —el de la referencia no lo
tiene— y el botón de siguiente deshabilitado con una razón visible, no
deshabilitado y en silencio.

---

## Antes de dar un incremento por terminado

A los cinco puntos del §10 de `agent-docs/estilo/01-frontend-react.md` se suman
estos cinco. Son la parte visual de la misma lista:

1. `npm run contraste` en verde. Si se añadió un token, se añadió su fila.
2. **Ningún color fuera de `tokens.css`**:
   `grep -rnE '#[0-9a-fA-F]{3,8}|rgb\(|hsl\(' frontend/src --include=*.css | grep -v tokens.css`
   devuelve vacío.
3. Revisado a **360px** de ancho: nada desborda, nada se solapa, nada se sale.
4. La página entera recorrida con el tabulador: el foco se ve siempre y el orden
   tiene sentido.
5. Los tres estados de la vista —cargando, vacío, error— vistos de verdad, no
   supuestos. El vacío se provoca con un filtro imposible; el error, parando el
   backend.
