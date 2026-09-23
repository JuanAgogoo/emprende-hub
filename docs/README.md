# Documentación del proyecto

Documentación **del proyecto y sus decisiones**, dirigida a personas: el equipo,
el docente y quien sustente. Va versionada en Git y se entrega con el repositorio.

## Índice

**Qué construimos y por qué**

- [decisiones-dominio.md](decisiones-dominio.md) — Todas las reglas de negocio,
  con su código (`B2-bis`, `G7`, `C7`…). Se citan desde el código y los PRs.
- [arquitectura.md](arquitectura.md) — Las dos mitades: stack, capas y seguridad
  del backend, y las decisiones del frontend con lo que deliberadamente no entra.
- [api.md](api.md) — El contrato de los endpoints, con un recorrido de demostración.
- [diseno.md](diseno.md) — El sistema de diseño del frontend: paleta, tipografía,
  mobile first y accesibilidad. Manda sobre cualquier criterio estético suelto.

**Cómo lo construimos**

- [pruebas.md](pruebas.md) — Cómo se verifica cada mitad: los tres niveles y la
  cobertura en el backend, el cierre obligatorio en el frontend, y las trampas
  que ya costaron tiempo.
- [flujo-de-trabajo.md](flujo-de-trabajo.md) — Ramas, commits, Pull Requests y release.
  Las cifras que estos documentos repiten —endpoints, peticiones, pruebas y
  cobertura— las verifica `./scripts/cifras.sh`, que sale con error si alguna
  se quedó atrás.
- [plan-de-entrega.md](plan-de-entrega.md) — Los 14 incrementos del backend, uno
  por rama y PR.
- [plan-de-entrega-frontend-fase-1.md](plan-de-entrega-frontend-fase-1.md) — Los
  13 de la primera fase del frontend.
- [plan-de-entrega-frontend-fase-2.md](plan-de-entrega-frontend-fase-2.md) — Los 6
  de la segunda: opiniones y recuperación de contraseña.

## Material del curso

La fuente de verdad de lo que enseña el docente es su repositorio de contenidos,
no la copia local, que estaba incompleta:

<https://github.com/jpcc1217/upb_contenidos_cursos> · publicado en
<https://jpcc1217.github.io/upb_contenidos_cursos/>

Relevante para este proyecto:

| Ruta | Contenido |
|---|---|
| `2026_2/plataformas/entregas_calificables/entrega_backend.html` | El enunciado de esta entrega |
| `2026_2/plataformas/procode/autenticacion/` | Teoría de JWT y taller guiado de Spring Security |
| `2026_2/plataformas/procode/unitarias/` | Pruebas unitarias: Mockito, `@DataJpaTest`, `@WebMvcTest` |
| `2026_2/ing_sw/clases_practicas/taller_ci_devops_guiado.html` | CI con GitHub Actions (el ejemplo es en Python) |

**El backend se entregó el martes 25 de agosto de 2026** y se sustentó. Se envió
el enlace del repositorio a `juan.canoc@upb.edu.co` con asunto
`Entrega Backend - [Nombre del Proyecto]`. Lo que se construye ahora es el
frontend, según su plan de entrega.

## Qué va aquí y qué no

| Va en `docs/` | Va en `agent-docs/` |
|---|---|
| Decisiones de dominio y su justificación | Instrucciones operativas para agentes de IA |
| Arquitectura, capas y contratos de API | Convenciones de prompt, plantillas, contexto de sesión |
| Guía de instalación y ejecución | Notas de trabajo de agentes |
| Cualquier cosa que el docente deba leer | Nada que deba leer una persona |

**Regla:** `docs/` se versiona y se entrega. `agent-docs/` está en `.gitignore`
y nunca sale de la máquina local.

**Regla de dependencia:** `agent-docs/` puede referirse a `docs/` como fuente de
verdad. `docs/` **nunca** menciona `agent-docs/` ni asume que existe — si lo hiciera,
la documentación entregada tendría enlaces rotos para quien clone el repositorio.
