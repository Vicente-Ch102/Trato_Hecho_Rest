package cl.tratohecho.commercial.repository

import cl.tratohecho.commercial.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CatalogItemRepository : JpaRepository<CatalogItemEntity, UUID> {
    fun findAllByTenantIdOrderByNameAsc(tenantId: UUID): List<CatalogItemEntity>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): CatalogItemEntity?
}
interface TaxConfigRepository : JpaRepository<TaxConfigEntity, UUID> {
    fun findAllByTenantIdOrderByNameAsc(tenantId: UUID): List<TaxConfigEntity>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): TaxConfigEntity?
}
interface QuoteRepository : JpaRepository<QuoteEntity, UUID> {
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<QuoteEntity>
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): QuoteEntity?
}
interface QuoteLineRepository : JpaRepository<QuoteLineEntity, UUID> {
    fun findAllByQuoteIdAndTenantIdOrderByIdAsc(quoteId: UUID, tenantId: UUID): List<QuoteLineEntity>
    fun deleteAllByQuoteIdAndTenantId(quoteId: UUID, tenantId: UUID)
}
interface QuoteFollowUpRepository : JpaRepository<QuoteFollowUpEntity, UUID> {
    fun findAllByQuoteIdAndTenantIdOrderByCreatedAtDesc(quoteId: UUID, tenantId: UUID): List<QuoteFollowUpEntity>
}

interface CommercialIdempotencyRepository : JpaRepository<CommercialIdempotencyEntity, UUID> {
    fun findByTenantIdAndKeyAndResourceType(tenantId: UUID, key: String, resourceType: String): CommercialIdempotencyEntity?
}
