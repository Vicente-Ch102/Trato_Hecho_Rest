package cl.tratohecho.billing.service

import cl.tratohecho.billing.domain.*
import cl.tratohecho.billing.repository.InvoiceRepository
import cl.tratohecho.billing.web.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.UUID

object InvoiceStatusRules {
    fun canTransition(from: InvoiceStatus, to: InvoiceStatus): Boolean = when (from) {
        InvoiceStatus.BORRADOR -> to == InvoiceStatus.EMITIDA || to == InvoiceStatus.ANULADA
        InvoiceStatus.EMITIDA -> to == InvoiceStatus.PAGADA || to == InvoiceStatus.ANULADA
        else -> false
    }
}

@Service
class InvoiceService(
    private val invoices: InvoiceRepository,
    private val commercialRestClient: RestClient
) {
    fun list(tenantId: UUID) = invoices.findAllByTenantIdOrderByIssuedDateDesc(tenantId).map { it.toResponse() }
    fun get(tenantId: UUID, id: UUID) = find(tenantId, id).toResponse()

    @Transactional
    fun create(tenantId: UUID, request: CreateInvoiceRequest, authorization: String): InvoiceResponse {
        val quote = fetchQuote(request.quoteId, authorization)
        if (quote.status != "ACEPTADA") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede facturar una cotización aceptada")
        }
        if (request.dueDate != null && request.dueDate.isBefore(request.issuedDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento no puede ser anterior a la emisión")
        }
        val folio = "FAC-${LocalDate.now(ZoneId.of("America/Santiago")).format(DateTimeFormatter.BASIC_ISO_DATE)}-${UUID.randomUUID().toString().take(6).uppercase()}"
        return invoices.save(InvoiceEntity(
            tenantId = tenantId, quoteId = request.quoteId, folio = folio,
            issuedDate = request.issuedDate, dueDate = request.dueDate,
            currency = quote.currency, subtotal = quote.subtotal, taxAmount = quote.taxAmount,
            total = quote.total, notes = request.notes?.trim()
        )).toResponse()
    }

    @Transactional
    fun update(tenantId: UUID, id: UUID, request: UpdateInvoiceRequest): InvoiceResponse {
        val invoice = find(tenantId, id)
        if (invoice.status != InvoiceStatus.BORRADOR) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede editar una factura en borrador")
        }
        if (request.dueDate != null && request.dueDate.isBefore(invoice.issuedDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de vencimiento no puede ser anterior a la emisión")
        }
        invoice.dueDate = request.dueDate
        invoice.notes = request.notes?.trim()
        return invoice.toResponse()
    }

    @Transactional
    fun changeStatus(tenantId: UUID, id: UUID, status: InvoiceStatus): InvoiceResponse {
        val invoice = find(tenantId, id)
        if (!InvoiceStatusRules.canTransition(invoice.status, status)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Transición de estado no permitida: ${invoice.status} -> $status")
        }
        invoice.status = status
        return invoice.toResponse()
    }

    @Transactional
    fun cancel(tenantId: UUID, id: UUID) {
        val invoice = find(tenantId, id)
        if (!InvoiceStatusRules.canTransition(invoice.status, InvoiceStatus.ANULADA)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La factura no se puede anular en su estado actual")
        }
        invoice.status = InvoiceStatus.ANULADA
    }

    private fun fetchQuote(id: UUID, authorization: String): CommercialQuoteSnapshot {
        try {
            return commercialRestClient.get()
                .uri("/api/v1/cotizaciones/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .body(CommercialQuoteSnapshot::class.java)
                ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo validar la cotización")
        } catch (ex: HttpClientErrorException.NotFound) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada para la empresa autenticada")
        } catch (ex: HttpClientErrorException) {
            throw ResponseStatusException(ex.statusCode, "El servicio comercial rechazó la validación de la cotización")
        } catch (ex: ResourceAccessException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio comercial no está disponible")
        }
    }

    private fun find(tenantId: UUID, id: UUID) = invoices.findByIdAndTenantId(id, tenantId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada")
}

fun InvoiceEntity.toResponse() = InvoiceResponse(
    id = id ?: error("Factura sin identificador"), quoteId = quoteId, folio = folio,
    issuedDate = issuedDate, dueDate = dueDate, currency = currency, subtotal = subtotal,
    taxAmount = taxAmount, total = total, status = status, notes = notes,
    createdAt = createdAt, updatedAt = updatedAt
)
