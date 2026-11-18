package cl.tratohecho.billing.web

import cl.tratohecho.billing.domain.InvoiceStatus
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class CreateInvoiceRequest(
    @field:NotNull val quoteId: UUID,
    val issuedDate: LocalDate = LocalDate.now(ZoneId.of("America/Santiago")),
    val dueDate: LocalDate? = null,
    @field:Size(max = 3000) val notes: String? = null
)
data class UpdateInvoiceRequest(
    val dueDate: LocalDate? = null,
    @field:Size(max = 3000) val notes: String? = null
)
data class InvoiceStatusRequest(@field:NotNull val status: InvoiceStatus)
data class InvoiceResponse(
    val id: UUID, val quoteId: UUID, val folio: String, val issuedDate: LocalDate,
    val dueDate: LocalDate?, val currency: String, val subtotal: BigDecimal,
    val taxAmount: BigDecimal, val total: BigDecimal, val status: InvoiceStatus,
    val notes: String?, val createdAt: Instant, val updatedAt: Instant
)

data class CommercialQuoteSnapshot(
    val id: UUID,
    val status: String,
    val currency: String,
    val subtotal: BigDecimal,
    val taxAmount: BigDecimal,
    val total: BigDecimal
)
