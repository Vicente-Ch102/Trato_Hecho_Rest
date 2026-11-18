package cl.tratohecho.commercial.web

import cl.tratohecho.commercial.domain.*
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CatalogItemRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:Size(max = 500) val description: String? = null,
    @field:NotNull val itemType: CatalogItemType,
    @field:DecimalMin("0.00") val defaultPrice: BigDecimal,
    val active: Boolean = true
)
data class CatalogItemResponse(
    val id: UUID, val name: String, val description: String?, val itemType: CatalogItemType,
    val defaultPrice: BigDecimal, val active: Boolean, val createdAt: Instant, val updatedAt: Instant
)

data class TaxRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:DecimalMin("0.000") @field:DecimalMax("100.000") val rate: BigDecimal,
    val active: Boolean = true
)
data class TaxResponse(
    val id: UUID, val name: String, val rate: BigDecimal, val active: Boolean,
    val createdAt: Instant, val updatedAt: Instant
)

data class QuoteLineRequest(
    val catalogItemId: UUID? = null,
    @field:Size(max = 160) val itemName: String? = null,
    @field:Size(max = 500) val description: String? = null,
    @field:DecimalMin("0.001") val quantity: BigDecimal,
    @field:DecimalMin("0.00") val unitPrice: BigDecimal? = null,
    val taxId: UUID? = null
)
data class QuoteRequest(
    @field:NotNull val customerId: UUID,
    @field:Pattern(regexp = "CLP|USD|UF") val currency: String = "CLP",
    val validUntil: LocalDate? = null,
    @field:Size(max = 3000) val notes: String? = null,
    @field:Size(min = 1) @field:Valid val items: List<QuoteLineRequest>
)
data class QuoteStatusRequest(@field:NotNull val status: QuoteStatus)
data class QuoteLineResponse(
    val id: UUID, val catalogItemId: UUID?, val itemName: String, val description: String?, val quantity: BigDecimal,
    val unitPrice: BigDecimal, val taxName: String?, val taxRate: BigDecimal,
    val lineNet: BigDecimal, val lineTax: BigDecimal, val lineTotal: BigDecimal
)
data class QuoteResponse(
    val id: UUID, val customerId: UUID, val code: String, val status: QuoteStatus,
    val currency: String, val validUntil: LocalDate?, val subtotal: BigDecimal,
    val taxAmount: BigDecimal, val total: BigDecimal, val notes: String?,
    val items: List<QuoteLineResponse>, val createdAt: Instant, val updatedAt: Instant
)
data class FollowUpRequest(
    @field:NotNull val channel: FollowUpChannel,
    @field:NotBlank @field:Size(max = 3000) val note: String,
    val nextContactAt: OffsetDateTime? = null,
    @field:Size(max = 300) val outcome: String? = null
)
data class FollowUpResponse(
    val id: UUID, val quoteId: UUID, val channel: FollowUpChannel, val note: String,
    val nextContactAt: OffsetDateTime?, val outcome: String?, val createdBy: UUID, val createdAt: Instant
)

data class CustomerSnapshot(
    val id: UUID,
    val businessName: String = "",
    val status: String = "ACTIVE"
)
