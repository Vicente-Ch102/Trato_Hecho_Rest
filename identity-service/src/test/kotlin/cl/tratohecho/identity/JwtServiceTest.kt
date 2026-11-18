package cl.tratohecho.identity

import cl.tratohecho.common.security.JwtService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class JwtServiceTest {
    @Test
    fun `token preserves user tenant and roles`() {
        val jwt = JwtService("test-secret-that-is-long-enough-to-create-a-256-bit-key-123456", 15)
        val userId = UUID.randomUUID()
        val tenantId = UUID.randomUUID()
        val token = jwt.createAccessToken(userId, tenantId, setOf("ADMIN"))
        val principal = jwt.parse(token)
        assertEquals(userId, principal.userId)
        assertEquals(tenantId, principal.tenantId)
        assertEquals(setOf("ADMIN"), principal.roles)
    }
}
