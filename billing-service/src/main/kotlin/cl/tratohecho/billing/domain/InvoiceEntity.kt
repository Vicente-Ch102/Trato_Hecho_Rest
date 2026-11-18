package cl.tratohecho.billing.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class InvoiceStatus { BORRADOR, EMITIDA, PAGADA, ANULADA }

@Entity
@Table(name = "invoices", uniqueConstraints = [UniqueConstraint(name = "uk_invoice_tenant_folio", columnNames = ["tenant_id", "folio"])], indexes = [Index(name = "ix_invoice_tenant_issued", columnList = "tenant_id, issued_date")])
class InvoiceEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(name = "quote_id", nullable = false) var quoteId: UUID,
    @Column(nullable = false, length = 40) var folio: String,
    @Column(name = "issued_date", nullable = false) var issuedDate: LocalDate,
    @Column(name = "due_date") var dueDate: LocalDate? = null,
    @Column(nullable = false, length = 3) var currency: String,
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2) var subtotal: BigDecimal,
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2) var taxAmount: BigDecimal,
    @Column(nullable = false, precision = 15, scale = 2) var total: BigDecimal,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: InvoiceStatus = InvoiceStatus.BORRADOR,
    @Column(columnDefinition = "TEXT") var notes: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now()
) {
    @PrePersist fun onCreate() { val now = Instant.now(); createdAt = now; updatedAt = now }
    @PreUpdate fun onUpdate() { updatedAt = Instant.now() }
}
