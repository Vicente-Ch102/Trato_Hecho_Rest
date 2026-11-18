package cl.tratohecho.identity.service

import cl.tratohecho.common.security.AppPrincipal
import cl.tratohecho.common.security.JwtService
import cl.tratohecho.identity.domain.*
import cl.tratohecho.identity.repository.*
import cl.tratohecho.identity.web.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID

@Service
class AuthenticationService(
    private val users: UserAccountRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @Value("\${app.security.refresh-token-days:14}") private val refreshTokenDays: Long,
    @Value("\${app.security.access-token-minutes:15}") private val accessTokenMinutes: Long
) {
    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = users.findByEmailIgnoreCase(request.email.trim())
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas")
        if (!user.enabled || !passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas")
        }
        return issueTokens(user)
    }

    @Transactional
    fun refresh(request: RefreshRequest): AuthResponse {
        val stored = refreshTokens.findByTokenHash(hash(request.refreshToken))
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido")
        if (stored.revokedAt != null || stored.expiresAt.isBefore(Instant.now())) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado o revocado")
        }
        stored.revokedAt = Instant.now()
        val user = users.findByIdAndTenantId(stored.userId, stored.tenantId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no disponible")
        if (!user.enabled) throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario deshabilitado")
        return issueTokens(user)
    }

    @Transactional
    fun logout(request: LogoutRequest) {
        refreshTokens.findByTokenHash(hash(request.refreshToken))?.let { it.revokedAt = Instant.now() }
    }

    fun current(principal: AppPrincipal): UserResponse {
        val user = users.findByIdAndTenantId(principal.userId, principal.tenantId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")
        return user.toResponse()
    }

    private fun issueTokens(user: UserAccountEntity): AuthResponse {
        val id = user.id ?: throw IllegalStateException("Usuario sin identificador")
        val tenantId = user.tenantId
        val role = user.role.name
        val access = jwtService.createAccessToken(id, tenantId, setOf(role))
        val refresh = newRefreshToken()
        refreshTokens.save(RefreshTokenEntity(
            tokenHash = hash(refresh), userId = id, tenantId = tenantId,
            expiresAt = Instant.now().plusSeconds(refreshTokenDays * 24 * 60 * 60)
        ))
        return AuthResponse(access, refresh, expiresIn = accessTokenMinutes * 60, user = user.toResponse())
    }

    private fun newRefreshToken(): String {
        val bytes = ByteArray(48)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

@Service
class UserManagementService(
    private val users: UserAccountRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun list(tenantId: UUID): List<UserResponse> = users.findAllByTenantIdOrderByNameAsc(tenantId).map { it.toResponse() }

    fun get(tenantId: UUID, id: UUID): UserResponse = find(tenantId, id).toResponse()

    @Transactional
    fun create(tenantId: UUID, request: CreateUserRequest): UserResponse {
        if (users.existsByEmailIgnoreCase(request.email.trim())) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo")
        }
        return users.save(UserAccountEntity(
            tenantId = tenantId, name = request.name.trim(), email = request.email.trim().lowercase(),
            passwordHash = passwordEncoder.encode(request.password), role = request.role
        )).toResponse()
    }

    @Transactional
    fun update(tenantId: UUID, id: UUID, request: UpdateUserRequest): UserResponse {
        val user = find(tenantId, id)
        val existing = users.findByEmailIgnoreCase(request.email.trim())
        if (existing != null && existing.id != id) throw ResponseStatusException(HttpStatus.CONFLICT, "Correo ya utilizado")
        user.name = request.name.trim()
        user.email = request.email.trim().lowercase()
        user.role = request.role
        user.enabled = request.enabled
        return user.toResponse()
    }

    @Transactional
    fun disable(tenantId: UUID, id: UUID) {
        val user = find(tenantId, id)
        user.enabled = false
    }

    private fun find(tenantId: UUID, id: UUID) = users.findByIdAndTenantId(id, tenantId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado")
}

fun UserAccountEntity.toResponse() = UserResponse(
    id = id ?: throw IllegalStateException("Usuario sin identificador"),
    name = name, email = email, role = role, enabled = enabled, createdAt = createdAt
)
