package cl.tratohecho.customers.repository

import cl.tratohecho.customers.domain.CustomerEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CustomerRepository : JpaRepository<CustomerEntity, UUID> {
    fun findAllByTenantIdOrderByBusinessNameAsc(tenantId: UUID): List<CustomerEntity>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): CustomerEntity?
    fun existsByTenantIdAndRutIgnoreCase(tenantId: UUID, rut: String): Boolean
    fun existsByTenantIdAndRutIgnoreCaseAndIdNot(tenantId: UUID, rut: String, id: UUID): Boolean
}

interface CustomerIdempotencyRepository : JpaRepository<cl.tratohecho.customers.domain.CustomerIdempotencyEntity, UUID> {
    fun findByTenantIdAndKeyAndResourceType(tenantId: UUID, key: String, resourceType: String): cl.tratohecho.customers.domain.CustomerIdempotencyEntity?
}
