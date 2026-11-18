package cl.tratohecho.billing.repository

import cl.tratohecho.billing.domain.InvoiceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InvoiceRepository : JpaRepository<InvoiceEntity, UUID> {
    fun findAllByTenantIdOrderByIssuedDateDesc(tenantId: UUID): List<InvoiceEntity>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): InvoiceEntity?
}
