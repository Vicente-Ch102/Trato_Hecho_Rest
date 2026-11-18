package cl.tratohecho.identity.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class UserRole { ADMIN, VENDEDOR, FACTURACION }

@MappedSuperclass
abstract class IdentityAuditedEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    @PrePersist
    fun onCreate() { val now = Instant.now(); createdAt = now; updatedAt = now }

    @PreUpdate
    fun onUpdate() { updatedAt = Instant.now() }
}

@Entity
@Table(name = "tenants")
class TenantEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(nullable = false, length = 150)
    var name: String
) : IdentityAuditedEntity()

@Entity
@Table(name = "user_accounts", uniqueConstraints = [UniqueConstraint(name = "uk_user_email", columnNames = ["email"])])
class UserAccountEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(nullable = false, length = 120)
    var name: String,
    @Column(nullable = false, length = 190)
    var email: String,
    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    var role: UserRole,
    @Column(nullable = false)
    var enabled: Boolean = true
) : IdentityAuditedEntity()

@Entity
@Table(name = "refresh_tokens", indexes = [Index(name = "ix_refresh_user", columnList = "user_id")])
class RefreshTokenEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String,
    @Column(name = "user_id", nullable = false)
    var userId: UUID,
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
