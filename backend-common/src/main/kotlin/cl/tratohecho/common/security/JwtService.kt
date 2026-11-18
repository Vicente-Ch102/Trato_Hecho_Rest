package cl.tratohecho.common.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JwtService(
    @Value("\${app.security.jwt-secret}") secret: String,
    @Value("\${app.security.access-token-minutes:15}") private val accessTokenMinutes: Long
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    fun createAccessToken(userId: UUID, tenantId: UUID, roles: Set<String>): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("roles", roles.toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60)))
            .signWith(key)
            .compact()
    }

    fun parse(token: String): AppPrincipal {
        val claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        val userId = UUID.fromString(claims.subject)
        val tenantId = UUID.fromString(claims["tenantId"].toString())
        val roles = (claims["roles"] as? Collection<*>)
            ?.mapNotNull { it?.toString() }
            ?.toSet()
            ?: emptySet()
        return AppPrincipal(userId, tenantId, roles)
    }
}
