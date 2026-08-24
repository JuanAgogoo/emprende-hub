# Flujo de trabajo

**Trunk-Based Development**, que es el modelo que la semana 1 del curso presenta
como «el modelo moderno» y «el estándar en empresas de alta escalabilidad», frente
a GitFlow. Sus reglas, citadas del material:

> Una sola fuente de verdad: la rama `main`. Ramas efímeras: las de `feature`
> duran horas o máximo 1-2 días. Las ramas de larga duración son un anti-patrón.

Los 14 incrementos del plan de entrega están dimensionados para eso: cada rama
nace y muere el mismo día.

No usamos GitFlow (`develop`, `release/*`, `hotfix/*`) porque el propio material lo
describe como «seguro pero lento, ideal cuando los despliegues a producción son
esporádicos», y aquí no hay despliegue a producción en absoluto.

Todo entra a `main` por Pull Request. `main` queda protegida: sin push directo y
con el workflow de CI en verde como requisito para fusionar.

## Ramas

Una rama por incremento, nombrada con el tipo de cambio que contiene:

```
chore/andamiaje          ci/pipeline           feat/cursos
feat/auth                feat/negocios         fix/...
```

La rama vive lo que dura su PR. Se fusiona con **squash** y se borra. El historial
de `main` queda como una lista legible de incrementos, uno por línea.

## Mensajes de commit

Conventional Commits. El formato lo lee `semantic-release` para decidir la versión:

```
<tipo>(<ámbito opcional>): <descripción en imperativo>
```

| Tipo | Para qué | Efecto en la versión |
|---|---|---|
| `feat` | Funcionalidad nueva | Sube la menor (1.2.0 → 1.3.0) |
| `fix` | Corrección de un fallo | Sube la de parche (1.2.0 → 1.2.1) |
| `chore` | Tareas de proyecto, dependencias | Ninguno |
| `ci` | Workflows y automatización | Ninguno |
| `docs` | Documentación | Ninguno |
| `test` | Pruebas | Ninguno |
| `refactor` | Cambio interno sin alterar comportamiento | Ninguno |

Un cambio incompatible lleva `!` tras el tipo (`feat!:`) y sube la versión mayor.

Ejemplos válidos:

```
feat(cursos): añadir filtrado por nivel y categoría
fix(directorio): excluir negocios pendientes del listado público
test(metricas): cubrir la agregación semanal de visitas
```

## Ciclo de un incremento

1. Crear la rama desde `main`.
2. Subir los cambios con un mensaje que siga la convención.
3. Abrir el PR contra `main`, describiendo qué entra y qué decisión del dominio
   implementa (por ejemplo «implementa B2-bis y B6»).
4. Esperar a que el CI pase: compilación, pruebas y cobertura.
5. Revisión de otra persona del equipo.
6. Squash and merge. La rama se borra.
7. `semantic-release` etiqueta la versión en `main` si el commit lo justifica.

## Abrir un PR sin usar git

No hace falta terminal ni IDE. Desde github.com, con la sesión iniciada:

1. En el repositorio, desplegar el selector de ramas y escribir el nombre de la
   rama nueva (por ejemplo `feat/cursos`). Aparece **Create branch** → pulsar.
2. Con esa rama seleccionada, **Add file → Upload files** y arrastrar los ficheros.
3. En el mensaje de commit, pegar el que indique el plan de entrega.
4. Elegir **Commit directly to the `feat/cursos` branch** → **Commit changes**.
5. Aparece un aviso con **Compare & pull request** → pulsar, describir y crear.

El commit queda a nombre de quien tiene la sesión abierta.

## Release

`semantic-release` sobre `main`, sin changelog. Configuración (`.releaserc.json`):

```json
{
  "branches": ["main"],
  "plugins": [
    "@semantic-release/commit-analyzer",
    "@semantic-release/release-notes-generator",
    "@semantic-release/github"
  ]
}
```

Sin `@semantic-release/changelog` ni `@semantic-release/git`: no se escribe fichero
de changelog ni se commitea nada de vuelta al repositorio. Las notas de la versión
se publican en la release de GitHub, generadas desde los mensajes de commit.

**La versión del `build.gradle` se queda fija** en `0.0.1-SNAPSHOT`.
`semantic-release` no sabe actualizarla y hacerlo requeriría
`@semantic-release/exec`, más piezas de las que este proyecto necesita. El
versionado vive en los tags de Git, que es lo que se ve desde GitHub.

Los tipos `chore`, `ci`, `docs`, `test` y `refactor` no generan versión: los PR 1
y 2 no producen ningún tag. **El primer tag será `v1.0.0`, con el primer `feat:`**,
que según el plan es el PR 3.

## Workflow de CI

`.github/workflows/ci.yml`, con la misma forma del ejemplo de la semana 1
(`name`, `on.push.branches`, trabajos encadenados con `needs`), adaptado a Gradle
y JDK 25 en lugar de Maven y JDK 17. Dos trabajos:

- **build**: en cada PR y en `main`. Compila, ejecuta las pruebas y publica como
  artefactos los informes de JaCoCo y de pruebas.

  Levanta PostgreSQL con un *service container* de GitHub Actions, publicado en
  el 5433 para que coincida con `application.yml` y no haga falta ninguna variable
  de entorno. Es distinto de Testcontainers, que se usa dentro de las pruebas de
  `@DataJpaTest`: aquí la base de datos la aporta el runner.

- **release**: solo al integrar en `main`, nunca en un PR, y solo si build pasó.
  Ejecuta `semantic-release` con los permisos mínimos (`contents`, `issues`,
  `pull-requests`) y `fetch-depth: 0`, que necesita para leer el historial.

**Cobertura**: JaCoCo genera el informe acotado a `service/**` desde este PR. El
umbral del 80% se activa en el PR 4, cuando exista lógica de negocio que medir.

## Plantilla de Pull Request

`.github/pull_request_template.md` se rellena solo al abrir un PR. Pide qué hace,
**qué decisiones del dominio implementa** (los códigos de
[decisiones-dominio.md](decisiones-dominio.md)), cómo probarlo y qué queda fuera.
Esa segunda pregunta es la que hace el trabajo trazable contra lo acordado.
