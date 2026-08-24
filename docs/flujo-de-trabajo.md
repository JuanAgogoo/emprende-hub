# Flujo de trabajo

**Trunk-Based Development**, que es el modelo que la semana 1 del curso presenta
como «el modelo moderno» y «el estándar en empresas de alta escalabilidad», frente
a GitFlow. Sus reglas, citadas del material:

> Una sola fuente de verdad: la rama `main`. Ramas efímeras: las de `feature`
> duran horas o máximo 1-2 días. Las ramas de larga duración son un anti-patrón.

Los 13 incrementos del plan de entrega están dimensionados para eso: cada rama
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

**La versión del `pom.xml` se queda fija.** `semantic-release` no sabe actualizarla
y hacerlo requeriría `@semantic-release/exec` llamando a `mvn versions:set`, más
piezas de las que este proyecto necesita. El versionado vive en los tags de Git,
que es lo que se ve desde GitHub.

## Workflow de CI

`.github/workflows/ci.yml`, con la misma forma del ejemplo de la semana 1
(`name`, `on.push.branches`, trabajos encadenados con `needs`), adaptado a Gradle
y JDK 25 en lugar de Maven y JDK 17. Dos trabajos:

- **build**: en cada PR y en `main`. Compila, ejecuta pruebas unitarias y de
  integración (Testcontainers levanta PostgreSQL) y publica el informe de JaCoCo.
- **release**: solo en `main` y solo si build pasó. Ejecuta `semantic-release`.
