package cl.tratohecho.common.security

import java.io.Serializable
import java.util.UUID

data class AppPrincipal(
    val userId: UUID,
    val tenantId: UUID,
    val roles: Set<String>
) : Serializable
