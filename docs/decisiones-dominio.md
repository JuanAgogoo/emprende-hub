# Decisiones de dominio

Estado: **cerrado, sin puntos abiertos.** Las 33 preguntas iniciales más las rondas
1 (6), 2 (4), 3 (6), 4 (7) y 5 (1).
Fuente: prototipo `EmprendeHub_Prototipo_v2.html` + respuestas del cliente (23 ago 2026).

Cuando el prototipo y este documento discrepen, **manda este documento**.

---

## A. Usuarios y roles

El prototipo solo implementa dos roles (`emprendedor`, `admin`) más el visitante
anónimo. El rol "Cliente" aparece escrito en la tabla del panel admin pero no
existe en el código. Se implementan **cuatro perfiles**:

| Perfil | Cómo se crea | Qué hace |
|---|---|---|
| Visitante | No se registra | Ve directorio, perfiles y cursos |
| Cliente | Registro corto: nombre, correo, contraseña | Opina, califica, contacta negocios |
| Emprendedor | Asistente de 4 pasos (registra su negocio) | Gestiona su negocio |
| Admin | Sembrado en migración, uno solo | Aprueba, suspende, publica cursos, modera |

**Un solo correo por cuenta.** El prototipo pedía dos en el registro (del negocio y
de acceso); se descarta el del negocio por no aportar al alcance.

**El correo no se muestra nunca en el perfil público.** Al haber un único correo,
publicarlo sería publicar el identificador de acceso. El público contacta por el
formulario; el dueño ve el correo del cliente en su buzón para responder por fuera.

- **A1** Cliente tiene registro propio, separado del de negocio.
- **A1-bis** Un cliente puede registrar un negocio más tarde **con su misma cuenta**,
  que pasa a emprendedor conservando sus opiniones. No abre una segunda cuenta.
- **A1-ter** Quien llega sabiendo que tiene un negocio se da de alta con él en una
  sola petición, y su cuenta **nace ya como emprendedor**. Convive con A1-bis a
  propósito: son dos entradas al mismo estado porque las dos situaciones existen
  —quien viene a publicar y quien se anima después—, y retirar la segunda dejaría
  peor al cliente que ya tenía opiniones escritas. **Todo entra en una
  transacción**: un fallo a mitad dejaría una cuenta creada sin negocio y a
  alguien sin poder reintentar, porque su propio correo estaría cogido.
- **A2** Un negocio por cuenta de emprendedor.
- **A3** Un único admin, sembrado. Credenciales irrelevantes para el alcance del curso.
- **A4** El emprendedor opina sobre otros negocios, **nunca sobre el suyo**.
- **A5** El admin no puede tener negocio.
- **A6** La contraseña exige **mínimo 8 caracteres y nada más**. El medidor de fuerza
  del prototipo (mayúscula, dígito, símbolo) es orientativo y vive en el frontend;
  el backend no lo impone. Cualquier regla extra habría que documentarla y probarla.

> El prototipo comprueba *si hay sesión*, nunca *qué rol* (única excepción: la
> pantalla admin). Los permisos por rol se diseñan aquí, no se copian de ahí.

## B. Ciclo de vida del negocio

Estados: `PENDIENTE → APROBADO | RECHAZADO`. La suspensión vive en el usuario, no
en el negocio.

- **B1** Un negocio rechazado corrige y reenvía, **sin límite de intentos**.
- **B2** Los cambios sobre campos públicos (nombre, descripción, fotos) vuelven a
  revisión. Teléfono y correo se actualizan al instante.
- **B2-bis** Mientras un cambio espera revisión, **el público sigue viendo la versión
  aprobada**. Los valores propuestos viven en una tabla aparte y solo se copian al
  negocio cuando el admin los aprueba. El negocio nunca sale del directorio por editar.
- **B3** El sello "Verificado" **se elimina**. No había criterio para otorgarlo.
- **B4** Al suspender un usuario: su negocio se oculta del directorio, las opiniones
  que escribió se mantienen, no puede entrar hasta reactivación.
- **B5** Plazo de revisión: **3 días hábiles**.
- **B6** Un negocio en estado `PENDIENTE` o `RECHAZADO` **solo lo ven su dueño y el
  admin**. Nunca aparece en el directorio ni es accesible por su identificador
  público. Se comprueba en el caso de uso, no en el controlador.
- **B7** El número de opiniones **no influye en la aprobación**. Un negocio nuevo
  tiene cero opiniones y debe poder aprobarse; exigir opiniones para aprobar sería
  un bloqueo circular, porque solo se puede opinar sobre negocios ya publicados.
- **B8** Enlaces a **redes sociales** (Instagram, LinkedIn) como campos opcionales,
  editables desde el panel. El prototipo los mostraba en el perfil pero no los pedía
  en ningún formulario.
- **B9** La **foto principal es la primera** por orden, sin campo aparte que la marque.
  Máximo 6, JPG o PNG, 5 MB cada una.

## C. Opiniones

- **C1** Exigen sesión iniciada. **No hay opiniones anónimas** (el prototipo se
  contradecía: mostraba una firmada por `anonimo@mail.com`).
- **C2** Una opinión por persona y negocio. Su autor puede editarla o borrarla.
- **C3** Denuncia una opinión cualquier usuario con sesión, más el dueño del negocio
  afectado. **El botón de denunciar no existe en el prototipo: es nuevo.**
- **C4** Se publican al instante. Solo se moderan si alguien las denuncia.
- **C5** Un negocio sin ninguna opinión **no tiene calificación** (no es cero). Se
  muestra como "Nuevo" y queda fuera del filtro de estrellas, para no castigarlo
  por ser reciente.
- **C6** El motivo de denuncia es una **lista cerrada**, no texto libre.
- **C7** Para salir en **Destacados** hacen falta **mínimo 5 opiniones**. Sin ese
  mínimo, una sola opinión de 5★ desplazaría de la portada a un negocio con 4,9 y
  320 opiniones. Aplica **solo a Destacados**; con la aprobación no tiene ninguna
  relación (ver B7).
- Límite heredado del prototipo: 300 caracteres.

## D. Consultas a negocios

- **D1** El mensaje se guarda en un **buzón dentro de la plataforma**, visible desde
  el panel del emprendedor. **Esa pantalla no existe en el prototipo: es nueva.**
- **D2** El emprendedor **no responde desde la plataforma**. El buzón le muestra el
  correo del cliente para que responda por fuera.
- **D3** Las consultas se marcan como **leídas / no leídas**.
- Contactar exige sesión iniciada.
- Límite heredado del prototipo: asunto + mensaje de 500 caracteres.

## E. Cursos

Un curso es **una ficha de catálogo con enlace externo**. No se relaciona con
ninguna otra entidad del sistema.

- **E1** Sin pagos de ningún tipo. La ficha muestra el precio y enlaza fuera.
  **Sin pasarela, ni siquiera simulada.**
- **E2** Sin inscripciones, sin progreso, sin certificados.
- **E3** Solo el admin publica cursos.
- **E4** Se conserva el estado **borrador / publicado**. Solo los publicados salen
  en el catálogo público.
- Taxonomía propia, **independiente de la de negocios**: 5 categorías (marketing,
  finanzas, ventas, digital, gestión) y 3 niveles (básico, intermedio, avanzado).

## F. Productos

- **F1** Escaparate. **No se venden**: sin carrito, sin pago, sin envíos.
- **F2** Nombre, precio y descripción opcional. (El prototipo se contradecía: el
  perfil mostraba descripciones que su propio formulario no permitía escribir.)
- **F3** Sin inventario. Solo un interruptor disponible / no disponible.
- **F4** **Cada producto lleva una foto obligatoria**, con las mismas reglas de
  imagen que la galería (B9): JPG o PNG y 5 MB como mucho. Un escaparate con
  huecos no es un escaparate, y la única forma de que el hueco no exista es que
  la imagen entre en la misma petición que crea el producto. De ahí que el alta
  sea `multipart` y no JSON, y de ahí que el escaparate saliera del alta de
  emprendedor (A1-ter), que es pública y no tiene sesión con la que subir
  binarios: los productos se crean justo después, con el token que devuelve.

## G. Catálogos

- **G1** **12 categorías de negocio**, las de la portada: Gastronomía, Moda,
  Tecnología, Belleza, Artesanías, Salud y bienestar, Educación, Hogar, Finanzas,
  Deportes, Mascotas, Eventos. Idénticas en todas las pantallas. (El prototipo
  usaba tres listas distintas: 12, 8 y 6.)
- **G2** Alcance geográfico: **área metropolitana del Valle de Aburrá**. Ciudades:
  Medellín, Envigado, Itagüí y Bello. Los barrios (El Poblado, Laureles) cuelgan
  de Medellín, no conviven con ella al mismo nivel.
- **G3** Dos niveles: **ciudad → barrio**. Así un negocio de El Poblado aparece al
  filtrar por Medellín, cosa que hoy no ocurre.
- **G4** El nivel de precio ($, $$, $$$) lo elige el dueño al registrarse; hay que
  **añadir esa pregunta al formulario**, que hoy no la hace. Se elimina la opción
  "Gratis", copiada por error desde la pantalla de cursos.
- **G5** Categorías y ciudades fijas. El admin no las edita en esta versión.
- **G6** El buscador compara **nombre y descripción**, como promete la interfaz.
  (El prototipo comparaba nombre y categoría.)
- **G7** El orden "Más recientes" usa la **fecha de aprobación**, no la de creación:
  es cuando el negocio aparece de verdad en el directorio. El prototipo declaraba
  la opción sin implementarla y sin fecha alguna en los datos.
- **G8** El teléfono admite **fijo y móvil**: 10 dígitos empezando por `3` (móvil)
  o por `60` (fijo). El patrón del prototipo solo aceptaba móviles, lo que dejaba
  fuera a cualquier local con línea fija.

## H. Métricas

- **H1** Visitas **reales**: cada visita a un perfil se registra, y el cálculo de
  semana, mes y variación porcentual se computa de verdad. Se siembra histórico
  para que la gráfica no salga vacía en la demo. Las visitas del propio dueño a su
  perfil no cuentan. **Una visita por perfil, sesión y día**: recargar la página no
  suma. Como el directorio es público, solo se descarta al dueño cuando hay sesión
  iniciada; las visitas anónimas siempre cuentan.
- **H2** Notificaciones reales, derivadas de hechos: nueva opinión, nueva consulta,
  negocio aprobado, negocio rechazado. **Fuera las de hitos de visitas.**
- **H3** Las notificaciones se marcan como **leídas / no leídas**.
- **H4** La barra de la portada (negocios activos, categorías, usuarios registrados,
  calificación promedio) muestra **cifras reales calculadas**, no las fijas del
  prototipo. Tras decidir H1, dejar números inventados en la portada sería incoherente.

> La agregación por periodo es lógica de negocio pura, sin base de datos de por
> medio. Es el mejor candidato para cubrir el 80% que exige la rúbrica.

## I. Correos

- **I1** **No se envía ningún correo, ni se deja preparado.** Consecuencias asumidas:
  - No hay recuperación de contraseña. Solo el admin puede restablecerla.
    Se retira el enlace "¿Olvidé mi contraseña?" del prototipo.
  - El motivo del rechazo **debe verse en el panel del emprendedor**.
    Pantalla y endpoint nuevos.
  - El panel es el único canal de avisos. Las notificaciones de H2 dejan de ser
    decorativas y pasan a ser el sistema de avisos.
- **I1-bis** **Se reabre I1 para la recuperación de contraseña, y solo para eso.**
  El backlog de la segunda entrega la pidió, y no existía en ninguna de las dos
  mitades. Lo que cambia y lo que no:
  - **Sí hay correo**, con `spring-boot-starter-mail` contra un **SMTP local**:
    [Mailpit](https://github.com/axllent/mailpit), un servicio más de
    `docker-compose.yml`. Funciona sin internet y sin cuenta de correo de nadie,
    y el mensaje **se ve llegar** en su bandeja web del 8025, que es enseñable.
    El correo no sale al mundo real; para que saliera se cambian tres
    propiedades de `application.yml` y nada más.
  - **Vuelve el enlace «¿Olvidaste tu contraseña?»** que I1 mandó retirar.
  - **Sigue sin haber verificación del correo al registrarse.** Ampliarlo
    cambiaría los dos registros y nadie lo pidió.
  - **El panel sigue siendo el canal de avisos** (H2): el motivo del rechazo y
    las notificaciones no se mandan por correo.
  - El token es **aleatorio, de un solo uso y caduca a los 30 minutos**. Se
    guarda sin cifrar, y es una simplificación consciente: quien pueda leer esa
    tabla ya tiene la de usuarios. **Sale solo por correo** —devolverlo en la
    respuesta dejaría cambiar la contraseña de cualquiera sabiendo su
    dirección— y **pedirlo responde igual exista la cuenta o no**, para que el
    formulario no sirva de lista de qué correos están registrados.

## J. Datos personales

- **J1** Ni la empresa, ni el domicilio, ni el correo de la política son reales.
  El texto queda como relleno.
- **J1-bis** Se conserva el **consentimiento como dato**: booleano, fecha y versión
  de política. Cuesta casi nada y ya está en la pantalla del prototipo.
- **J2** Sin borrado de cuenta a petición del usuario. La suspensión de B4 cubre
  la necesidad del administrador.

## K. Escala

- **K1** Irrelevante. El proyecto no llega a producción. Las cifras de la portada
  ("+500 negocios") son de muestra.

---

## L. Log de moderación

Tabla de solo escritura, alimentada por los casos de uso del admin. Registra quién,
qué y cuándo, y se consulta filtrando por rango de fechas.

Eventos: negocio aprobado, negocio rechazado (con motivo), **cambio pendiente
aprobado**, **cambio pendiente rechazado** (los dos que introduce B2-bis), opinión
eliminada (con motivo), denuncia desestimada, curso publicado, cuenta suspendida,
cuenta reactivada.

Nunca se edita ni se borra. Es el argumento de trazabilidad para la sustentación.

## Fuera de alcance

Explícito, para que nadie los espere en la entrega. Ninguno está en el prototipo:

- Responder a una opinión desde el negocio.
- Eliminar o pausar un negocio (existe la suspensión por el admin, B4).
- Borrado de cuenta a petición del usuario (J2).
- Verificación del correo al registrarse (I1). La recuperación de contraseña sí
  entra, desde I1-bis.
- Carrito, pagos y pasarela, ni siquiera simulada (E1, F1).
- Inscripciones, progreso y certificados de cursos (E2).

## Puntos abiertos

Ninguno. El dominio quedó cerrado el 23 de agosto de 2026, y la única decisión
reabierta desde entonces es I1, con I1-bis.

Cualquier decisión nueva se añade a la sección que le corresponda, no aquí.
