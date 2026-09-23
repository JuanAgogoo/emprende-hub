# Plan de entrega

Catorce incrementos, cada uno una rama y un Pull Request. El orden es de dependencia:
cada rama sale de `main` con el anterior ya fusionado.

Cada incremento indica el mensaje de commit exacto, para pegarlo sin redactarlo.

---

## Fase 1 — Andamiaje (PR 1–3)

### PR 1 · `chore/andamiaje`
Proyecto **Gradle** con **Java 25 y Spring Boot 4.1.x**, generado desde Spring
Initializr con Spring Web, Spring Data JPA y el driver de PostgreSQL. Paquetes
`controller`, `service`, `repository`, `model`, `dto`, `exception`, `config`.
`docker-compose.yml` con PostgreSQL. `application.yml` con `ddl-auto: update`,
zona `America/Bogota` y `spring.threads.virtual.enabled=true`. README de arranque.

```
chore: crear andamiaje del proyecto con las capas y PostgreSQL
```

### PR 2 · `ci/pipeline`
Workflow de CI en dos trabajos (`build` con PostgreSQL como *service container*,
y `release` solo en `main`), JaCoCo acotado a `service/**`, `.releaserc.json` sin
changelog y plantilla de PR. La protección de `main` se configura en los ajustes
del repositorio, no en el código. Ver [flujo-de-trabajo.md](flujo-de-trabajo.md).

```
ci: añadir workflow de integración continua y release automático
```

### PR 3 · `feat/catalogos`
Entidades de catálogo y su carga inicial mediante `CommandLineRunner`: 12
categorías de negocio, ciudades del Valle de Aburrá con sus barrios, 5 categorías
y 3 niveles de curso. Endpoints de solo lectura. Implementa G1, G2, G3, G5.

```
feat(catalogos): añadir categorías, ciudades y barrios con sus endpoints
```

---

## Fase 2 — Primer vertical y acceso (PR 4–5)

### PR 4 · `feat/cursos`
Vertical completo de punta a punta: entidad, repositorio, servicio, controlador y
DTOs como records. Borrador/publicado, filtros por categoría, nivel y gratuito.
**Los tres niveles de prueba** (Mockito sobre el servicio, `@DataJpaTest`,
`@WebMvcTest`), que quedan como plantilla para el resto.

> Aviso: estos `@WebMvcTest` se escriben sin seguridad en el classpath. El PR 5 la
> añade y empezarán a devolver 401, así que ese PR incluye volver aquí a anotarlos
> con `@WithMockUser`. Está contemplado, no es un descuido.
Implementa E1, E2, E3, E4.

```
feat(cursos): implementar catálogo de cursos con estados y filtros
```

### PR 5 · `feat/auth`
Sigue el taller guiado de `procode/autenticacion/` con sus mismos nombres de
clase: `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`,
`ApplicationConfig` y `AuthController`. Entidad `Usuario` implementando
`UserDetails`, enum `Rol` con `CLIENTE`, `EMPRENDEDOR` y `ADMIN`, BCrypt y jjwt
0.12.5. Contraseña de mínimo 8 caracteres. Implementa A1, A3, A6.

**Incluye además**: añadir `spring-security-test`, anotar con `@WithMockUser` los
`@WebMvcTest` de los PR 3 y 4, y aplicar la matriz de acceso de
[arquitectura.md](arquitectura.md#quién-accede-a-qué). Es el PR más grande de la
fase y el que más preparación necesita para la sustentación.

```
feat(auth): añadir registro, autenticación con JWT y control de roles
```

---

## Fase 3 — Negocios (PR 6–7)

### PR 6 · `feat/negocios`
Modelo de Negocio y su registro en una sola transacción. Validaciones de nombre,
descripción, teléfono fijo o móvil y nivel de precio. Ascenso de cliente a
emprendedor. Implementa A1-bis, A2, A5, G4, G8.

```
feat(negocios): implementar registro de negocios con sus validaciones
```

### PR 7 · `feat/moderacion`
Estados como `sealed interface` con pattern matching, aprobación y rechazo con
motivo visible en el panel,
cambios pendientes sin sacar al negocio del directorio, reglas de visibilidad y
log de moderación. Implementa B1–B7 y la sección L.

```
feat(moderacion): añadir aprobación de negocios, cambios pendientes y log
```

---

## Paréntesis — Documentación (PR 8)

No añade código. Se hace aquí, con siete incrementos entregados, porque es el
punto en que la documentación había acumulado suficiente deriva como para
estorbar en vez de ayudar.

### PR 8 · `docs/consolidacion`
Contrato de la API con los 28 endpoints que existían entonces, que hasta ahora no
estaban listados en ningún sitio. Se corrige una contradicción interna de
`arquitectura.md` y se parte en dos, separando el diseño de cómo se prueba. El
README pasa a explicar cómo llamar a la API, no solo cómo arrancarla.

```
docs: añadir el contrato de la API y reorganizar la documentación
```

---

## Fase 4 — Directorio y contenido del negocio (PR 9–10)

### PR 9 · `feat/directorio`
Búsqueda por nombre y descripción, filtros por categoría, ciudad, barrio,
calificación y precio. Ordenación y paginación. Destacados con mínimo cinco
opiniones. Estadísticas de portada calculadas. Implementa C7, G6, G7, H4.

```
feat(directorio): implementar búsqueda, filtros y paginación del directorio
```

### PR 10 · `feat/fotos-productos`
Subida de imágenes con validación de tipo, tamaño y máximo, la primera como
principal. Productos con disponibilidad. Enlaces a redes sociales.
Implementa B8, B9, F1, F2, F3.

```
feat(negocios): añadir gestión de fotos, productos y redes sociales
```

---

## Fase 5 — Interacción y cierre (PR 11–14)

### PR 11 · `feat/opiniones`
Alta, edición y borrado con una opinión por persona y negocio, nunca sobre el
propio. Promedio recalculado en cada cambio, incluida la moderación. Denuncias con
motivos de lista cerrada. Implementa A4, C1–C6.

```
feat(opiniones): implementar opiniones, calificaciones y denuncias
```

### PR 12 · `feat/consultas`
Buzón de consultas con marcado de leídas y el correo del cliente visible para el
dueño. Implementa D1, D2, D3.

```
feat(consultas): añadir buzón de consultas para los negocios
```

### PR 13 · `feat/metricas`
Registro de visitas con una por perfil, sesión y día, excluyendo al dueño.
Agregación semanal y mensual con variación porcentual. Notificaciones derivadas de
hechos, con marcado de leídas. Implementa H1, H2, H3.

```
feat(metricas): implementar registro de visitas y notificaciones del panel
```

### PR 14 · `chore/seed-postman`
Datos sembrados por `CommandLineRunner`: admin, clientes, los 12 negocios y 8
cursos del prototipo, opiniones e histórico de visitas. Las contraseñas pasan por
el `PasswordEncoder`, nunca en texto plano. La carga se salta si ya hay datos. Colección de Postman en la que el
login guarda el token en una variable y el resto de peticiones lo heredan.
README final con instrucciones de ejecución y cobertura.

```
chore: añadir datos de prueba, colección de Postman y documentación final
```

---

## Cobertura

El umbral del 80% sobre `service/**` se verifica en cada PR desde el 4. Un PR que
lo baje no se fusiona: es más barato escribir la prueba en su incremento que
recuperar cobertura al final.

## Resultado

Los catorce incrementos se entregaron. El contador de este documento se quedó
atrás una vez —decía 27 endpoints donde había 28—, así que aquí queda la cifra
final para no volver a arrastrarla:

| | |
|---|---|
| Endpoints | **64**, todos en [api.md](api.md) y en la colección de Postman |
| Pruebas | **480** en verde, en los tres niveles |
| Cobertura de `service/**` | **98,3%**, sobre el 80% que exige la rúbrica |

Al cerrarse los catorce incrementos eran 60 endpoints y 444 pruebas. Lo que hay
de más son las **ampliaciones acordadas** que vinieron después, ya con el backend
entregado: el alta de emprendedor, la foto obligatoria por producto, el
reordenado de la galería y la recuperación de contraseña, que reabre I1 y es la
única que trae dependencia nueva. Cada una se acordó antes de tocar nada; las
reglas están en `agent-docs/estilo/02-backend.md`.

**Al cambiar el código, actualizar estas cifras aquí y en el README.** Un número
en un documento entregable envejece peor que no ponerlo. `./scripts/cifras.sh`
las cuenta y dice cuáles se quedaron atrás; sale con código 1 si alguna falla.
