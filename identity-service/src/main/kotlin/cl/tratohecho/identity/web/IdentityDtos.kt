package cl.tratohecho.identity.web

import cl.tratohecho.identity.domain.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class RefreshRequest(@field:NotBlank val refreshToken: String)
data class LogoutRequest(@field:NotBlank val refreshToken: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

data class CreateUserRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
    @field:Email @field:NotBlank @field:Size(max = 190) val email: String,
    @field:NotBlank @field:Size(min = 10, max = 100) val password: String,
    @field:NotNull val role: UserRole
)

data class UpdateUserRequest(
    @field:NotBlank @field:Size(max = 120) val name: String,
    @field:Email @field:NotBlank @field:Size(max = 190) val email: String,
    @field:NotNull val role: UserRole,
    val enabled: Boolean = true
)

data class UserResponse(
    val id: UUID,
    val name: String,
    val email: String,
    val role: UserRole,
    val enabled: Boolean,
    val createdAt: Instant
)
