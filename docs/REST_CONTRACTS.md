# Contratos REST resumidos

- Base común: `/api/v1`
- Content-Type: `application/json`
- Autenticación: `Authorization: Bearer <accessToken>` salvo endpoints públicos indicados en README.
- Tenant: derivado del claim `tenantId` firmado en JWT; nunca recibido como dato de negocio desde el cliente móvil.
- Idempotencia: `Idempotency-Key` opcional en `POST /clientes` y `POST /cotizaciones`; máximo 80 caracteres.
- Identificadores: UUID.
- Fechas de API: `yyyy-MM-dd` para fechas sin hora; ISO-8601 para marcas de tiempo.

Para endpoints, roles, payloads y transiciones consulta `README.md`, secciones 6 y 8.
