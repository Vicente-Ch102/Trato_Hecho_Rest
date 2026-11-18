package cl.tratohecho.identity.repository

import cl.tratohecho.identity.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantRepository : JpaRepository<TenantEntity, UUID>
interface UserAccountRepository : JpaRepository<UserAccountEntity, UUID> {
    fun findByEmailIgnoreCase(email: String): UserAccountEntity?
    fun findAllByTenantIdOrderByNameAsc(tenantId: UUID): List<UserAccountEntity>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): UserAccountEntity?
    fun existsByEmailIgnoreCase(email: String): Boolean
}
interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?
}
