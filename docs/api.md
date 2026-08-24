# Contrato de la API

Todas las rutas cuelgan de `/api/v1`. Los cuerpos son JSON.

Este documento es la fuente de la colección de Postman del PR 14 y el guion para
explicar el flujo de datos en la sustentación.

## Autenticación

La API es *stateless*: no hay sesión y el servidor no guarda nada entre
peticiones. El token viaja en cada llamada:

```
Authorization: Bearer <token>
```

`POST /auth/login` devuelve el token, su tipo, la caducidad en milisegundos, y el
nombre y rol de quien entra, para que el cliente no tenga que descodificar el
token solo para saberlo.

### Quién puede llamar a qué

| Acceso | Significa |
|---|---|
| **Público** | Sin cabecera. El directorio se explora sin registrarse |
| **Sesión** | Cualquier rol autenticado |
| **ADMIN** | Solo el administrador |
| **Dueño** | Con sesión, y solo sobre su propio negocio |

Dos códigos que no significan lo mismo:

- **401** — falta autenticarse: no hay token, o no sirve.
- **403** — hay token válido, pero ese rol no puede hacer eso.

## Errores

Un único `GlobalExceptionHandler` decide todos los códigos. Los servicios lanzan
excepciones y no saben nada de HTTP.

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "..." }
```

Los errores de validación añaden **una clave por campo inválido**:

```json
{ "status": 400, "error": "Bad Request", "titulo": "El título es obligatorio" }
```

| Código | Cuándo |
|---|---|
| `400` | Validación de forma, o una regla de negocio incumplida |
| `401` | Sin token, token corrupto o caducado, o credenciales incorrectas |
| `403` | Rol insuficiente, o cuenta suspendida |
| `404` | No existe, **o no es visible para quien pregunta** |

Un negocio pendiente o un curso en borrador devuelven **404, no 403**: no se
filtra información sobre lo que existe sin publicar.

## Paginación

Todos los listados devuelven una página de Spring Data:

```
?page=0&size=12&sort=titulo,asc
```

```json
{ "content": [ … ], "totalElements": 42, "totalPages": 4, "number": 0 }
```

---

## Acceso · `/auth`

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `POST` | `/auth/registro` | Público | `201` con el token. Alta de cliente (A1) |
| `POST` | `/auth/login` | Público | `200` con el token |

```bash
curl -X POST localhost:8080/api/v1/auth/registro \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"María García","correo":"maria@gmail.com","contrasena":"contrasena123"}'
```

Contraseña de **mínimo 8 caracteres** (A6). El correo identifica la cuenta, es
único y **nunca se muestra en público**.

No hay verificación de correo ni recuperación de contraseña (I1): quien la olvide
tiene que pedírsela al administrador.

## Catálogos · `/catalogos`

Fijos, de solo lectura y públicos (G5).

| Método | Ruta | Devuelve |
|---|---|---|
| `GET` | `/catalogos/categorias-negocio` | Las 12 categorías (G1) |
| `GET` | `/catalogos/ciudades` | Ciudades **con sus barrios anidados** (G2, G3) |
| `GET` | `/catalogos/categorias-curso` | Las 5 categorías de formación |
| `GET` | `/catalogos/niveles-curso` | Básico, Intermedio, Avanzado |

Las ciudades traen sus barrios en la misma respuesta: el selector de localización
necesita los dos niveles a la vez. Solo Medellín tiene barrios.

## Cursos · `/cursos`

Catálogo público. **Solo enseña los publicados** (E4).

| Método | Ruta | Acceso | Devuelve |
|---|---|---|---|
| `GET` | `/cursos` | Público | Página filtrable |
| `GET` | `/cursos/{id}` | Público | Un curso publicado, o `404` |

Filtros, todos opcionales y combinables:

```
?categoria=MARKETING&nivel=BASICO&gratuito=true&texto=redes
```

El texto busca en título y descripción, sin distinguir mayúsculas.

## Negocios · `/negocios`

| Método | Ruta | Acceso | Hace |
|---|---|---|---|
| `POST` | `/negocios` | Sesión | Registra y **asciende a emprendedor** (A1-bis) |
| `GET` | `/negocios/mio` | Dueño | Su negocio, en el estado que esté |
| `PUT` | `/negocios/mio` | Dueño | **Propone** cambiar nombre o descripción |
| `PATCH` | `/negocios/mio/contacto` | Dueño | Cambia el teléfono, al instante |
| `POST` | `/negocios/mio/reenviar` | Dueño | Corrige un rechazado y reenvía (B1) |

El registro llega en **una sola petición**: los cuatro pasos del prototipo son
una división visual del formulario.

| Campo | Regla |
|---|---|
| `nombre` | 3 a 120 caracteres |
| `descripcion` | **Mínimo 80 caracteres** |
| `telefono` | Móvil `3XXXXXXXXX` o fijo `60XXXXXXXX` (G8) |
| `nivelPrecio` | `BAJO`, `MEDIO`, `ALTO` (G4) |
| `barrioId` | Opcional, y debe pertenecer a la ciudad indicada |

**`PUT /negocios/mio` no cambia el negocio.** Guarda una propuesta que espera
revisión, y el público sigue viendo la versión aprobada mientras tanto (B2-bis).
El teléfono es la excepción: se aplica al momento.

## Gestión de cursos · `/admin/cursos`

Solo **ADMIN** (E3). A diferencia del catálogo público, aquí se ven los borradores.

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/admin/cursos` | Lista todo, borradores incluidos |
| `GET` | `/admin/cursos/{id}` | Uno cualquiera |
| `POST` | `/admin/cursos` | Crea (`201`). **Nace en borrador** |
| `PUT` | `/admin/cursos/{id}` | Edita |
| `PATCH` | `/admin/cursos/{id}/publicar` | Publica |
| `PATCH` | `/admin/cursos/{id}/borrador` | Retira del catálogo |
| `DELETE` | `/admin/cursos/{id}` | Elimina (`204`) |

Regla que cruza dos campos y vive en el servicio: **un curso gratuito no puede
llevar precio y uno de pago está obligado a llevarlo.** Publicar dos veces
devuelve `400`.

## Moderación · `/admin/moderacion`

Solo **ADMIN**.

| Método | Ruta | Hace |
|---|---|---|
| `GET` | `/negocios-pendientes` | Cola de revisión, los más antiguos primero |
| `PATCH` | `/negocios/{id}/aprobar` | Aprueba y sella la fecha |
| `PATCH` | `/negocios/{id}/rechazar` | Rechaza. **Motivo obligatorio** |
| `PATCH` | `/negocios/{id}/cambio/aprobar` | Publica los valores propuestos |
| `PATCH` | `/negocios/{id}/cambio/rechazar` | Descarta la propuesta |
| `PATCH` | `/usuarios/{id}/suspender` | Suspende la cuenta (`204`) |
| `PATCH` | `/usuarios/{id}/reactivar` | La reactiva (`204`) |
| `GET` | `/log` | Historial, filtrable por fechas |

El motivo del rechazo lo lee el dueño en `GET /negocios/mio`: sin correos (I1),
ese campo es el único sitio donde se entera de por qué le rechazaron.

```
GET /admin/moderacion/log?desde=2026-08-23T00:00:00Z&hasta=2026-08-25T00:00:00Z
```

El log es de solo escritura: nunca se edita ni se borra.

---

## Recorrido completo para la demostración

Sirve como guion de la colección de Postman y de la sustentación.

```bash
A=http://localhost:8080/api/v1

# 1. Un cliente se registra y entra
curl -X POST $A/auth/registro -H 'Content-Type: application/json' \
  -d '{"nombre":"Sofía Ruiz","correo":"sofia@gmail.com","contrasena":"contrasena123"}'
TC=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"sofia@gmail.com","contrasena":"contrasena123"}' | jq -r .token)

# 2. Registra su negocio: nace PENDIENTE y ella pasa a EMPRENDEDOR
curl -X POST $A/negocios -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{ … }'

# 3. El administrador entra, lo ve en la cola y lo rechaza con motivo
TA=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' \
  -d '{"correo":"admin@emprendehub.co","contrasena":"admin12345"}' | jq -r .token)
curl -H "Authorization: Bearer $TA" $A/admin/moderacion/negocios-pendientes
curl -X PATCH $A/admin/moderacion/negocios/1/rechazar -H "Authorization: Bearer $TA" \
  -H 'Content-Type: application/json' -d '{"motivo":"La descripción no explica qué vendes"}'

# 4. Ella lee el motivo en su panel, corrige y reenvía
curl -H "Authorization: Bearer $TC" $A/negocios/mio
curl -X POST $A/negocios/mio/reenviar -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{ … }'

# 5. El administrador aprueba
curl -X PATCH $A/admin/moderacion/negocios/1/aprobar -H "Authorization: Bearer $TA"

# 6. Ella propone cambiar el nombre: el negocio SIGUE publicado con el viejo
curl -X PUT $A/negocios/mio -H "Authorization: Bearer $TC" \
  -H 'Content-Type: application/json' -d '{ … }'

# 7. El administrador aprueba el cambio y ahora sí se publica
curl -X PATCH $A/admin/moderacion/negocios/1/cambio/aprobar -H "Authorization: Bearer $TA"

# 8. Todo quedó registrado
curl -H "Authorization: Bearer $TA" $A/admin/moderacion/log
```

## Lo que todavía no existe

Llega en los PR 9 a 14, según [plan-de-entrega.md](plan-de-entrega.md):

- Directorio público con búsqueda y filtros, y destacados (PR 9)
- Fotos, productos y redes sociales (PR 10)
- Opiniones y denuncias (PR 11)
- Buzón de consultas (PR 12)
- Visitas y notificaciones (PR 13)

**Al añadir endpoints, actualizar este documento en el mismo PR.** Un contrato
que va por detrás del código es peor que no tenerlo.
