package cl.tratohecho.customers.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class CustomerStatus { ACTIVE, INACTIVE }

@Entity
@Table(
    name = "customers",
    uniqueConstraints = [UniqueConstraint(name = "uk_customer_tenant_rut", columnNames = ["tenant_id", "rut"])],
    indexes = [Index(name = "ix_customer_tenant", columnList = "tenant_id")]
)
class CustomerEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID,
    @Column(name = "business_name", nullable = false, length = 180)
    var businessName: String,
    @Column(nullable = false, length = 15)
    var rut: String,
    @Column(name = "contact_name", length = 120)
    var contactName: String? = null,
    @Column(length = 190)
    var email: String? = null,
    @Column(length = 30)
    var phone: String? = null,
    @Column(length = 250)
    var address: String? = null,
    @Column(columnDefinition = "TEXT")
    var notes: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    var status: CustomerStatus = CustomerStatus.ACTIVE,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PrePersist fun onCreate() { val now = Instant.now(); createdAt = now; updatedAt = now }
    @PreUpdate fun onUpdate() { updatedAt = Instant.now() }
}

@Entity
@Table(name = "idempotency_records", uniqueConstraints = [UniqueConstraint(name = "uk_customer_idem", columnNames = ["tenant_id", "idem_key", "resource_type"])])
class CustomerIdempotencyEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) var id: UUID? = null,
    @Column(name = "tenant_id", nullable = false) var tenantId: UUID,
    @Column(name = "idem_key", nullable = false, length = 80) var key: String,
    @Column(name = "resource_type", nullable = false, length = 30) var resourceType: String,
    @Column(name = "resource_id", nullable = false) var resourceId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.now()
)
