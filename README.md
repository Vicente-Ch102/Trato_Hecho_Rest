# Trato Hecho - Backend de microservicios

Backend de Trato Hecho desarrollado en **Kotlin + Spring Boot + MySQL**. El repositorio contiene cuatro aplicaciones Spring Boot independientes, cada una con su propio puerto, esquema de datos, migraciones Flyway y artefacto ejecutable. `backend-common` comparte autenticación JWT, seguridad HTTP y formato de errores.

> **Alcance funcional:** identidad y usuarios; clientes; cotizaciones, catálogo, impuestos y seguimiento; facturación interna. Los indicadores económicos (USD/UF) quedan fuera de estos servicios: la aplicación Android debe consumir la API externa directamente con Retrofit. Este backend no genera documentos tributarios electrónicos ni se conecta al SII.

## 1. Estructura

```text
trato-hecho-backend/
├── backend-common/       # JWT, filtro de seguridad, principal y errores comunes
├── identity-service/     # Login, refresh, usuarios, roles y tenant
├── customers-service/    # CRUD de clientes
├── commercial-service/   # Cotizaciones, líneas, seguimiento, catálogos e impuestos
├── billing-service/      # Registros internos de facturación
├── docker/               # Dockerfile compartido e inicialización MySQL
├── postman/              # Colección de pruebas HTTP
└── docker-compose.yml
```

Aunque se almacenan en un mismo repositorio, los cuatro servicios tienen clases `main`, configuración, migración y `bootJar` separados; se construyen y ejecutan como procesos y contenedores independientes.

| Servicio | Puerto | Base MySQL | Función |
|---|---:|---|---|
| `identity-service` | 8081 | `trato_identidad_db` | Autenticación, usuarios y roles |
| `customers-service` | 8082 | `trato_clientes_db` | Clientes |
| `commercial-service` | 8083 | `trato_comercial_db` | Cotizaciones, seguimiento, catálogo e impuestos |
| `billing-service` | 8084 | `trato_facturacion_db` | Facturación interna |

## 2. Requisitos técnicos

- Java 21.
- Gradle 8.14.7 (el script `gradlew` utiliza Gradle instalado o descarga esa versión cuando hay conexión).
- Docker Engine y Docker Compose v2 para levantar todo el entorno de una vez.
- MySQL 8.4 (incluido en Docker Compose).

Frameworks y librerías principales: Spring Boot 3.5.16, Kotlin 2.2.21, Spring Web, Spring Security, Spring Data JPA, Jakarta Validation, Flyway, MySQL Connector/J, JJWT, Springdoc OpenAPI, JUnit 5 y MockK.

## 3. Inicio rápido con Docker Compose

Desde esta carpeta:

```bash
cp .env.example .env
```

Edita `.env`. Cambia especialmente `MYSQL_ROOT_PASSWORD`, `JWT_SECRET` y `BOOTSTRAP_ADMIN_PASSWORD`. Para desarrollo local puedes mantener `BOOTSTRAP_ENABLED=true`; en un entorno desplegado desactiva el bootstrap luego de crear al primer administrador.

Luego ejecuta:

```bash
docker compose up --build -d
```

Revisa los contenedores:

```bash
docker compose ps
docker compose logs -f identity-service customers-service commercial-service billing-service
```

Comprobaciones de disponibilidad:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
```

Las migraciones Flyway se ejecutan al iniciar cada servicio. La primera inicialización de MySQL crea cuatro bases de datos. La persistencia queda en el volumen `tratohecho_mysql_data`.

Para detener:

```bash
docker compose down
```

Para borrar también la base local (operación destructiva):

```bash
docker compose down -v
```

### Usuario inicial de desarrollo

Con el bootstrap habilitado, el servicio de identidad crea un tenant de demostración y el administrador configurado en `.env`:

- Correo: `BOOTSTRAP_ADMIN_EMAIL`
- Contraseña: `BOOTSTRAP_ADMIN_PASSWORD`
- Rol: `ADMIN`

No se almacena una contraseña de administrador fija en el código. Cambia las credenciales de ejemplo antes de compartir o desplegar el proyecto.

## 4. Ejecución local sin Docker

Inicia primero MySQL 8.4 y crea las cuatro bases de datos con `docker/init-databases.sql`. Al usar el puerto publicado por el Compose, configura `DB_URL` con `localhost:3307`.

En terminales separadas, desde la raíz:

```bash
./gradlew :identity-service:bootRun
./gradlew :customers-service:bootRun
./gradlew :commercial-service:bootRun
./gradlew :billing-service:bootRun
```

En Windows utiliza `gradlew.bat`. Define en el entorno de cada proceso `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y el mismo `JWT_SECRET`. En identidad también define las variables `BOOTSTRAP_*` para crear el primer usuario.

## 5. Seguridad y aislamiento por empresa

1. `identity-service` autentica correo y contraseña y emite un access token JWT firmado (HS256) junto con un refresh token opaco.
2. El refresh token se almacena como hash SHA-256 y se rota cuando se utiliza.
3. Los cuatro servicios validan el mismo JWT con el secreto compartido `JWT_SECRET`.
4. El token contiene `sub` (usuario), `tenantId` y `roles`.
5. Las consultas y modificaciones de datos se filtran por el `tenantId` del principal autenticado. Los DTO de alta no reciben `tenantId` desde Android.
6. Los roles definidos son `ADMIN`, `VENDEDOR` y `FACTURACION`. Los permisos por endpoint aparecen en las tablas de esta documentación.

**Importante:** el valor por defecto incluido en `application.yml` es exclusivamente para facilitar la ejecución local. En producción genera un secreto aleatorio de al menos 32 bytes, almacénalo en un gestor de secretos y nunca lo subas a GitHub. Los tokens access duran 15 minutos por defecto y los refresh 14 días.

## 6. Endpoints REST

Todos los endpoints propios usan `/api/v1`, intercambian JSON y, salvo login/refresh/logout y health checks, requieren:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Los endpoints `POST` de creación de clientes y cotizaciones aceptan además `Idempotency-Key: <uuid>` para reintentos de sincronización offline. La clave es única por empresa y tipo de recurso; repetirla devuelve el recurso ya creado en lugar de crear otro.

### 6.1 Identidad - `http://localhost:8081`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Público | Iniciar sesión |
| POST | `/api/v1/auth/refresh` | Público | Rotar refresh token y emitir nuevos tokens |
| POST | `/api/v1/auth/logout` | Público con refresh token | Revocar refresh token |
| GET | `/api/v1/auth/me` | Autenticado | Obtener perfil actual |
| GET | `/api/v1/usuarios` | ADMIN | Listar usuarios del tenant |
| GET | `/api/v1/usuarios/{id}` | ADMIN | Obtener usuario |
| POST | `/api/v1/usuarios` | ADMIN | Crear usuario |
| PUT | `/api/v1/usuarios/{id}` | ADMIN | Actualizar nombre, correo, rol y estado |
| DELETE | `/api/v1/usuarios/{id}` | ADMIN | Desactivar usuario (baja lógica) |

Login:

```json
{
  "email": "admin@tratohecho.local",
  "password": "<BOOTSTRAP_ADMIN_PASSWORD>"
}
```

Respuesta: `accessToken`, `refreshToken`, `tokenType`, `expiresIn` y perfil `user`. Guardar ambos tokens de forma segura en Android; enviar el access token como Bearer. Para `/auth/refresh`, enviar `{ "refreshToken": "..." }`. `/auth/logout` revoca el refresh token indicado; el access token vigente seguirá siendo válido hasta que expire.

Roles permitidos al crear/actualizar usuario: `ADMIN`, `VENDEDOR`, `FACTURACION`. La contraseña de alta requiere entre 10 y 100 caracteres.

### 6.2 Clientes - `http://localhost:8082`

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| GET | `/api/v1/clientes?q={texto}` | ADMIN, VENDEDOR, FACTURACION | Listar/buscar clientes |
| GET | `/api/v1/clientes/{id}` | ADMIN, VENDEDOR, FACTURACION | Consultar cliente |
| POST | `/api/v1/clientes` | ADMIN, VENDEDOR | Crear cliente; acepta `Idempotency-Key` |
| PUT | `/api/v1/clientes/{id}` | ADMIN, VENDEDOR | Actualizar cliente |
| DELETE | `/api/v1/clientes/{id}` | ADMIN, VENDEDOR | Desactivar cliente |

Ejemplo de alta:

```json
{
  "businessName": "Comercial del Sur SpA",
  "rut": "76.123.456-7",
  "contactName": "María González",
  "email": "contacto@comercialdelsur.cl",
  "phone": "+56 9 1234 5678",
  "address": "Concepción, Chile",
  "notes": "Cliente de ejemplo"
}
```

El RUT se guarda normalizado (sin puntos y en mayúsculas) y es único por tenant. `DELETE` es baja lógica: cambia `status` a `INACTIVE`; el registro no se borra físicamente.

### 6.3 Comercial - `http://localhost:8083`

**Cotizaciones** (ADMIN y VENDEDOR):

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/cotizaciones` | Listar cotizaciones del tenant |
| GET | `/api/v1/cotizaciones/{id}` | Consultar cotización con sus líneas |
| POST | `/api/v1/cotizaciones` | Crear cotización; acepta `Idempotency-Key` |
| PUT | `/api/v1/cotizaciones/{id}` | Actualizar cotización en borrador |
| DELETE | `/api/v1/cotizaciones/{id}` | Eliminar cotización en borrador |
| PATCH | `/api/v1/cotizaciones/{id}/estado` | Cambiar estado permitido |
| GET | `/api/v1/cotizaciones/{id}/seguimientos` | Listar seguimientos |
| POST | `/api/v1/cotizaciones/{id}/seguimientos` | Agregar seguimiento |

Ejemplo de cotización:

```json
{
  "customerId": "00000000-0000-0000-0000-000000000001",
  "currency": "CLP",
  "validUntil": "2026-12-15",
  "notes": "Cotización de ejemplo",
  "items": [
    {
      "itemName": "Servicio de instalación",
      "description": "Instalación inicial",
      "quantity": 1,
      "unitPrice": 150000,
      "taxId": "00000000-0000-0000-0000-000000000002"
    }
  ]
}
```

`catalogItemId` es opcional: si se informa, debe ser un ítem activo del tenant y puede omitirse `itemName`/`unitPrice` para tomar nombre y precio por defecto. Si no se usa catálogo, informa `itemName` y `unitPrice`. `taxId` es opcional; sin impuesto asociado la tasa aplicada es 0%. Si se informa, debe pertenecer al tenant y estar activo. La cotización verifica el cliente consultando `customers-service` con el mismo Bearer token. Las monedas admitidas en esta versión son `CLP`, `USD` y `UF`.

Estados y transiciones permitidas:

- `BORRADOR` → `ENVIADA`.
- `ENVIADA` → `ACEPTADA`, `RECHAZADA` o `VENCIDA`.
- Los estados finales no se pueden reabrir desde este endpoint.

Para cambiar estado, enviar `{ "status": "ENVIADA" }` a `/api/v1/cotizaciones/{id}/estado`.

Seguimiento:

```json
{
  "channel": "TELEFONO",
  "note": "Se contactó al cliente para revisar la propuesta",
  "nextContactAt": "2026-11-20T10:00:00-03:00",
  "outcome": "Solicita nueva llamada"
}
```

Canales: `TELEFONO`, `CORREO`, `WHATSAPP`, `REUNION`, `OTRO`. El usuario creador se obtiene del JWT.

**Catálogos** (consulta para los roles de negocio; modificación ADMIN):

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/catalogos` | Listar productos/servicios |
| GET | `/api/v1/catalogos/{id}` | Consultar ítem |
| POST | `/api/v1/catalogos` | Crear ítem |
| PUT | `/api/v1/catalogos/{id}` | Actualizar ítem |
| DELETE | `/api/v1/catalogos/{id}` | Desactivar ítem |

Ejemplo: `{ "name": "Instalación", "description": "Servicio", "itemType": "SERVICIO", "defaultPrice": 150000, "active": true }`. `itemType`: `PRODUCTO` o `SERVICIO`.

**Impuestos** (consulta para roles de negocio; modificación ADMIN):

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/impuestos` | Listar impuestos |
| GET | `/api/v1/impuestos/{id}` | Consultar impuesto |
| POST | `/api/v1/impuestos` | Crear impuesto |
| PUT | `/api/v1/impuestos/{id}` | Actualizar impuesto |
| DELETE | `/api/v1/impuestos/{id}` | Desactivar impuesto |

Ejemplo: `{ "name": "IVA", "rate": 19, "active": true }`. La tasa se expresa en porcentaje y admite entre 0 y 100.

### 6.4 Facturación - `http://localhost:8084`

Acceso: ADMIN y FACTURACION.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/facturas` | Listar registros de facturación |
| GET | `/api/v1/facturas/{id}` | Consultar registro |
| POST | `/api/v1/facturas` | Crear registro desde cotización aceptada |
| PUT | `/api/v1/facturas/{id}` | Actualizar vencimiento/notas en borrador |
| PATCH | `/api/v1/facturas/{id}/estado` | Cambiar estado permitido |
| DELETE | `/api/v1/facturas/{id}` | Anular según estado |

Ejemplo:

```json
{
  "quoteId": "00000000-0000-0000-0000-000000000003",
  "issuedDate": "2026-11-16",
  "dueDate": "2026-12-16",
  "notes": "Registro interno de facturación"
}
```

El monto y la moneda se obtienen desde `commercial-service`; no se aceptan montos enviados por Android para evitar que se manipule el total. Solo se puede crear desde cotización `ACEPTADA`. Estados: `BORRADOR` → `EMITIDA` → `PAGADA`, o `BORRADOR/EMITIDA` → `ANULADA`. `PAGADA` no puede anularse desde este flujo.

Estos endpoints administran **registros internos de facturación**. No emiten factura electrónica tributaria ni reemplazan una integración autorizada con el SII.

## 7. API externa de indicadores económicos

No existe endpoint de indicadores en este backend. Android debe conservar un cliente Retrofit independiente para la API externa seleccionada por el proyecto (USD/UF), mostrar esos valores en la interfaz y manejar sus errores/indisponibilidad separadamente de los datos y servicios propios.

## 8. Códigos HTTP y errores

El manejador común devuelve, para errores de validación y negocio, una estructura como esta:

```json
{
  "timestamp": "2026-11-16T18:30:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Revisa los datos ingresados",
  "path": "/api/v1/clientes",
  "details": ["businessName: no debe estar vacío"]
}
```

| Código | Uso |
|---:|---|
| 200 | Consulta o actualización exitosa |
| 201 | Recurso creado |
| 204 | Operación sin cuerpo (logout, baja/anulación) |
| 400 | JSON o validación inválida |
| 401 | Token/credenciales inválidas o ausentes |
| 403 | Rol sin permiso |
| 404 | Recurso no encontrado dentro del tenant autenticado |
| 409 | Conflicto, duplicado o transición de estado no permitida |
| 503 | Microservicio dependiente no disponible |

Cada servicio expone Swagger UI en `/swagger-ui.html` y OpenAPI JSON en `/v3/api-docs`.

## 9. Pruebas

Ejecuta todos los tests:

```bash
./gradlew test
```

Genera reportes JaCoCo:

```bash
./gradlew test jacocoTestReport
```

Reportes HTML:

- `identity-service/build/reports/jacoco/test/html/index.html`
- `customers-service/build/reports/jacoco/test/html/index.html`
- `commercial-service/build/reports/jacoco/test/html/index.html`
- `billing-service/build/reports/jacoco/test/html/index.html`

Hay pruebas unitarias de JWT, búsqueda de clientes, cálculo de cotizaciones y transiciones de estados. **No se declara una cobertura del 80% alcanzada**: debe medirse ejecutando el reporte y, además, la rúbrica pide pruebas en ViewModel/Repository de la aplicación móvil. Esa parte se implementará al adaptar Android.

## 10. Integración Android (siguiente etapa)

Android deberá configurar cuatro instancias Retrofit (una por base URL) y enviar el Bearer token en las operaciones protegidas. Para el emulador Android estándar, las direcciones locales de host son `http://10.0.2.2:8081/`, `:8082/`, `:8083/` y `:8084/`. En un dispositivo físico utiliza la IP LAN del computador que ejecuta Docker, siempre que la red permita la conexión.

Para los envíos offline de clientes y cotizaciones:

1. Mantén el UUID local de la operación.
2. En el primer intento de creación usa ese UUID como valor de `Idempotency-Key`.
3. Si la solicitud queda sin respuesta y se reintenta, repite la misma clave.
4. Marca como sincronizado solo cuando el servidor responda exitosamente.
5. Para un `409`, muestra el conflicto y evita reemplazar datos locales silenciosamente.

Los endpoints `PUT` actualizan recursos existentes; no uses `POST` para editar. Los IDs de las respuestas son UUID y deben guardarse en Room para futuras modificaciones.

## 11. Limitaciones y verificación previa a entrega

- El backend define contratos propuestos a partir de la planificación de Trato Hecho y los módulos descritos en este repositorio. Antes de conectarlo, hay que cotejar los nombres/campos con la matriz Excel de requerimientos y los formularios finales de Android.
- Los nombres exactos de perfiles operativos deben confirmarse con el caso; esta versión define `ADMIN`, `VENDEDOR` y `FACTURACION` como perfiles de trabajo.
- Los catálogos e impuestos se crean por administración; no se carga automáticamente un catálogo real ni una tasa tributaria predefinida.
- La facturación modelada es interna, no tributaria.
- Las llamadas entre servicios propagan el token del usuario y validan el tenant mediante el servicio destino.
- La sincronización offline se maneja en Android; el backend aporta idempotencia a las altas de clientes y cotizaciones.
- Para producción aún se deben agregar TLS, secretos administrados, respaldos, monitoreo, rotación de claves y políticas operacionales específicas.

## 12. GitHub y trabajo colaborativo

Sube este backend como repositorio separado del proyecto Android. No publiques `.env`, contraseñas reales ni claves JWT. El `.env.example` sí es seguro como plantilla local. Mantén commits por funcionalidad (identidad, clientes, comercial, facturación, pruebas, documentación) y distribuye las tareas en Trello de acuerdo con los integrantes del equipo.
