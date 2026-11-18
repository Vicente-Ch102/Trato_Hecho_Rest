package cl.tratohecho.commercial.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class QuoteStatus { BORRADOR, ENVIADA, ACEPTADA, RECHAZADA, VENCIDA }
enum class CatalogItemType { PRODUCTO, SERVICIO }
enum class FollowUpChannel { TELEFONO, CORREO, WHATSAPP, REUNION, OTRO }

@MappedSuperclass
abstract class CommercialAuditedEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
    @PrePersist fun onCreate() { val now = Instant.now(); createdAt = now; updatedAt = now }
    @PreUpdate fun onUpdate() { updatedAt = Instant.now() }
}

@Entity
@Table(name = "catalog_items", indexes = [Index(name = "ix_catalog_tenant", columnList = "tenant_id")])
class CatalogItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(nullable = false, length = 160) var name: String,
    @Column(length = 500) var description: String? = null,
    @Enumerated(EnumType.STRING) @Column(name = "item_type", nullable = false, length = 20) var itemType: CatalogItemType,
    @Column(name = "default_price", nullable = false, precision = 15, scale = 2) var defaultPrice: BigDecimal,
    @Column(nullable = false) var active: Boolean = true
) : CommercialAuditedEntity()

@Entity
@Table(name = "tax_configs", indexes = [Index(name = "ix_tax_tenant", columnList = "tenant_id")])
class TaxConfigEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(nullable = false, length = 100) var name: String,
    @Column(nullable = false, precision = 7, scale = 3) var rate: BigDecimal,
    @Column(nullable = false) var active: Boolean = true
) : CommercialAuditedEntity()

@Entity
@Table(name = "quotes", uniqueConstraints = [UniqueConstraint(name = "uk_quote_tenant_code", columnNames = ["tenant_id", "code"])], indexes = [Index(name = "ix_quote_tenant_created", columnList = "tenant_id, created_at")])
class QuoteEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(name = "customer_id", nullable = false) var customerId: UUID,
    @Column(nullable = false, length = 40) var code: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: QuoteStatus = QuoteStatus.BORRADOR,
    @Column(nullable = false, length = 3) var currency: String,
    @Column(name = "valid_until") var validUntil: LocalDate? = null,
    @Column(nullable = false, precision = 15, scale = 2) var subtotal: BigDecimal,
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2) var taxAmount: BigDecimal,
    @Column(nullable = false, precision = 15, scale = 2) var total: BigDecimal,
    @Column(columnDefinition = "TEXT") var notes: String? = null
) : CommercialAuditedEntity()

@Entity
@Table(name = "quote_lines", indexes = [Index(name = "ix_quote_lines_parent", columnList = "tenant_id, quote_id")])
class QuoteLineEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(name = "quote_id", nullable = false) var quoteId: UUID,
    @Column(name = "catalog_item_id") var catalogItemId: UUID? = null,
    @Column(name = "item_name", nullable = false, length = 160) var itemName: String,
    @Column(length = 500) var description: String? = null,
    @Column(nullable = false, precision = 12, scale = 3) var quantity: BigDecimal,
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2) var unitPrice: BigDecimal,
    @Column(name = "tax_name", length = 100) var taxName: String? = null,
    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 3) var taxRate: BigDecimal,
    @Column(name = "line_net", nullable = false, precision = 15, scale = 2) var lineNet: BigDecimal,
    @Column(name = "line_tax", nullable = false, precision = 15, scale = 2) var lineTax: BigDecimal,
    @Column(name = "line_total", nullable = false, precision = 15, scale = 2) var lineTotal: BigDecimal
)

@Entity
@Table(name = "quote_follow_ups", indexes = [Index(name = "ix_followup_quote", columnList = "tenant_id, quote_id, created_at")])
class QuoteFollowUpEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(name = "quote_id", nullable = false) var quoteId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var channel: FollowUpChannel,
    @Column(nullable = false, columnDefinition = "TEXT") var note: String,
    @Column(name = "next_contact_at") var nextContactAt: OffsetDateTime? = null,
    @Column(length = 300) var outcome: String? = null,
    @Column(name = "created_by", nullable = false) var createdBy: UUID,
    @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.now()
)

@Entity
@Table(name = "idempotency_records", uniqueConstraints = [UniqueConstraint(name = "uk_commercial_idem", columnNames = ["tenant_id", "idem_key", "resource_type"])])
class CommercialIdempotencyEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(name = "idem_key", nullable = false, length = 80) var key: String,
    @Column(name = "resource_type", nullable = false, length = 30) var resourceType: String,
    @Column(name = "resource_id", nullable = false) var resourceId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.now()
)
