package cl.tratohecho.commercial.service

import cl.tratohecho.commercial.domain.*
import cl.tratohecho.commercial.repository.*
import cl.tratohecho.commercial.web.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class CatalogService(private val items: CatalogItemRepository) {
    fun list(tenantId: UUID) = items.findAllByTenantIdOrderByNameAsc(tenantId).map { it.toResponse() }
    fun get(tenantId: UUID, id: UUID) = find(tenantId, id).toResponse()
    @Transactional fun create(tenantId: UUID, request: CatalogItemRequest) = items.save(
        CatalogItemEntity(tenantId = tenantId, name = request.name.trim(), description = request.description?.trim(), itemType = request.itemType, defaultPrice = money(request.defaultPrice), active = request.active)
    ).toResponse()
    @Transactional fun update(tenantId: UUID, id: UUID, request: CatalogItemRequest): CatalogItemResponse {
        val item = find(tenantId, id)
        item.name = request.name.trim(); item.description = request.description?.trim()
        item.itemType = request.itemType; item.defaultPrice = money(request.defaultPrice); item.active = request.active
        return item.toResponse()
    }
    @Transactional fun delete(tenantId: UUID, id: UUID) { find(tenantId, id).active = false }
    private fun find(tenantId: UUID, id: UUID) = items.findByIdAndTenantId(id, tenantId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ítem de catálogo no encontrado")
}

@Service
class TaxService(private val taxes: TaxConfigRepository) {
    fun list(tenantId: UUID) = taxes.findAllByTenantIdOrderByNameAsc(tenantId).map { it.toResponse() }
    fun get(tenantId: UUID, id: UUID) = find(tenantId, id).toResponse()
    @Transactional fun create(tenantId: UUID, request: TaxRequest) = taxes.save(
        TaxConfigEntity(tenantId = tenantId, name = request.name.trim(), rate = request.rate.setScale(3, RoundingMode.HALF_UP), active = request.active)
    ).toResponse()
    @Transactional fun update(tenantId: UUID, id: UUID, request: TaxRequest): TaxResponse {
        val tax = find(tenantId, id)
        tax.name = request.name.trim(); tax.rate = request.rate.setScale(3, RoundingMode.HALF_UP); tax.active = request.active
        return tax.toResponse()
    }
    @Transactional fun delete(tenantId: UUID, id: UUID) { find(tenantId, id).active = false }
    private fun find(tenantId: UUID, id: UUID) = taxes.findByIdAndTenantId(id, tenantId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Impuesto no encontrado")
}

object QuoteStatusRules {
    fun canTransition(from: QuoteStatus, to: QuoteStatus): Boolean = when (from) {
        QuoteStatus.BORRADOR -> to == QuoteStatus.ENVIADA
        QuoteStatus.ENVIADA -> to == QuoteStatus.ACEPTADA || to == QuoteStatus.RECHAZADA || to == QuoteStatus.VENCIDA
        else -> false
    }
}

data class CalculatedLine(
    val net: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val rate: BigDecimal
)
object QuoteCalculator {
    fun calculate(quantity: BigDecimal, unitPrice: BigDecimal, taxRate: BigDecimal): CalculatedLine {
        val net = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
        val tax = net.multiply(taxRate).divide(BigDecimal("100"), 2, RoundingMode.HALF_UP)
        return CalculatedLine(net, tax, net.add(tax).setScale(2, RoundingMode.HALF_UP), taxRate)
    }
}

@Service
class QuoteService(
    private val quotes: QuoteRepository,
    private val lines: QuoteLineRepository,
    private val taxes: TaxConfigRepository,
    private val catalogItems: CatalogItemRepository,
    private val followUps: QuoteFollowUpRepository,
    private val idempotency: CommercialIdempotencyRepository,
    private val customerLookup: CustomerLookupService
) {
    fun list(tenantId: UUID) = quotes.findAllByTenantIdOrderByCreatedAtDesc(tenantId).map { it.toResponse(lines.findAllByQuoteIdAndTenantIdOrderByIdAsc(it.id!!, tenantId)) }
    fun get(tenantId: UUID, id: UUID): QuoteResponse {
        val quote = find(tenantId, id)
        return quote.toResponse(lines.findAllByQuoteIdAndTenantIdOrderByIdAsc(id, tenantId))
    }

    @Transactional
    fun create(tenantId: UUID, request: QuoteRequest, authorization: String, idempotencyKey: String?): QuoteResponse {
        val key = idempotencyKey?.trim()?.takeIf { it.isNotEmpty() }
        if (key != null) {
            if (key.length > 80) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key no puede superar 80 caracteres")
            idempotency.findByTenantIdAndKeyAndResourceType(tenantId, key, "QUOTE")?.let { record ->
                return get(tenantId, record.resourceId)
            }
        }
        customerLookup.verify(request.customerId, authorization)
        val prepared = prepareLines(tenantId, request.items)
        val totals = totals(prepared)
        val code = "COT-${LocalDate.now(ZoneId.of("America/Santiago")).format(DateTimeFormatter.BASIC_ISO_DATE)}-${UUID.randomUUID().toString().take(6).uppercase()}"
        val quote = quotes.save(QuoteEntity(
            tenantId = tenantId, customerId = request.customerId, code = code,
            currency = request.currency, validUntil = request.validUntil,
            subtotal = totals.first, taxAmount = totals.second, total = totals.third,
            notes = request.notes?.trim()
        ))
        val quoteId = quote.id ?: error("Cotización sin identificador")
        lines.saveAll(prepared.map { it.forQuote(tenantId, quoteId) })
        if (key != null) idempotency.save(CommercialIdempotencyEntity(
            tenantId = tenantId, key = key, resourceType = "QUOTE", resourceId = quoteId
        ))
        return quote.toResponse(lines.findAllByQuoteIdAndTenantIdOrderByIdAsc(quoteId, tenantId))
    }

    @Transactional
    fun update(tenantId: UUID, id: UUID, request: QuoteRequest, authorization: String): QuoteResponse {
        val quote = find(tenantId, id)
        if (quote.status != QuoteStatus.BORRADOR) throw ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden modificar cotizaciones en borrador")
        customerLookup.verify(request.customerId, authorization)
        val prepared = prepareLines(tenantId, request.items)
        val totals = totals(prepared)
        quote.customerId = request.customerId; quote.currency = request.currency
        quote.validUntil = request.validUntil; quote.notes = request.notes?.trim()
        quote.subtotal = totals.first; quote.taxAmount = totals.second; quote.total = totals.third
        lines.deleteAllByQuoteIdAndTenantId(id, tenantId)
        lines.saveAll(prepared.map { it.forQuote(tenantId, id) })
        return quote.toResponse(lines.findAllByQuoteIdAndTenantIdOrderByIdAsc(id, tenantId))
    }

    @Transactional
    fun delete(tenantId: UUID, id: UUID) {
        val quote = find(tenantId, id)
        if (quote.status != QuoteStatus.BORRADOR) throw ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden eliminar cotizaciones en borrador")
        lines.deleteAllByQuoteIdAndTenantId(id, tenantId)
        quotes.delete(quote)
    }

    @Transactional
    fun changeStatus(tenantId: UUID, id: UUID, newStatus: QuoteStatus): QuoteResponse {
        val quote = find(tenantId, id)
        if (!QuoteStatusRules.canTransition(quote.status, newStatus)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Transición de estado no permitida: ${quote.status} -> $newStatus")
        }
        quote.status = newStatus
        return quote.toResponse(lines.findAllByQuoteIdAndTenantIdOrderByIdAsc(id, tenantId))
    }

    @Transactional
    fun addFollowUp(tenantId: UUID, quoteId: UUID, userId: UUID, request: FollowUpRequest): FollowUpResponse {
        val quote = find(tenantId, quoteId)
        if (quote.status == QuoteStatus.RECHAZADA) throw ResponseStatusException(HttpStatus.CONFLICT, "No se puede realizar seguimiento a una cotización rechazada")
        return followUps.save(QuoteFollowUpEntity(
            tenantId = tenantId, quoteId = quoteId, channel = request.channel,
            note = request.note.trim(), nextContactAt = request.nextContactAt,
            outcome = request.outcome?.trim(), createdBy = userId
        )).toResponse()
    }

    fun listFollowUps(tenantId: UUID, quoteId: UUID): List<FollowUpResponse> {
        find(tenantId, quoteId)
        return followUps.findAllByQuoteIdAndTenantIdOrderByCreatedAtDesc(quoteId, tenantId).map { it.toResponse() }
    }

    private fun prepareLines(tenantId: UUID, requests: List<QuoteLineRequest>): List<QuoteLineEntity> = requests.map { request ->
        val catalogItem = request.catalogItemId?.let { id ->
            catalogItems.findByIdAndTenantId(id, tenantId)?.takeIf { it.active }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El ítem de catálogo indicado no existe o está inactivo")
        }
        val itemName = request.itemName?.trim()?.takeIf { it.isNotEmpty() } ?: catalogItem?.name
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada línea requiere itemName o catalogItemId")
        val unitPrice = request.unitPrice ?: catalogItem?.defaultPrice
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada línea requiere unitPrice o un ítem de catálogo con precio")
        val tax = request.taxId?.let { id ->
            taxes.findByIdAndTenantId(id, tenantId)?.takeIf { it.active }
                ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El impuesto indicado no existe o está inactivo")
        }
        val rate = tax?.rate ?: BigDecimal.ZERO
        val amounts = QuoteCalculator.calculate(request.quantity, unitPrice, rate)
        QuoteLineEntity(
            tenantId = tenantId, quoteId = UUID(0, 0), catalogItemId = catalogItem?.id, itemName = itemName,
            description = request.description?.trim(), quantity = request.quantity,
            unitPrice = money(unitPrice), taxName = tax?.name, taxRate = rate,
            lineNet = amounts.net, lineTax = amounts.tax, lineTotal = amounts.total
        )
    }

    private fun totals(prepared: List<QuoteLineEntity>): Triple<BigDecimal, BigDecimal, BigDecimal> {
        val net = prepared.fold(BigDecimal.ZERO) { sum, item -> sum + item.lineNet }.setScale(2, RoundingMode.HALF_UP)
        val tax = prepared.fold(BigDecimal.ZERO) { sum, item -> sum + item.lineTax }.setScale(2, RoundingMode.HALF_UP)
        return Triple(net, tax, (net + tax).setScale(2, RoundingMode.HALF_UP))
    }

    private fun find(tenantId: UUID, id: UUID) = quotes.findByIdAndTenantId(id, tenantId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada")
}

fun CatalogItemEntity.toResponse() = CatalogItemResponse(id!!, name, description, itemType, defaultPrice, active, createdAt, updatedAt)
fun TaxConfigEntity.toResponse() = TaxResponse(id!!, name, rate, active, createdAt, updatedAt)
fun QuoteEntity.toResponse(lines: List<QuoteLineEntity>) = QuoteResponse(
    id = id!!, customerId = customerId, code = code, status = status, currency = currency,
    validUntil = validUntil, subtotal = subtotal, taxAmount = taxAmount, total = total, notes = notes,
    items = lines.map { it.toResponse() }, createdAt = createdAt, updatedAt = updatedAt
)
fun QuoteLineEntity.toResponse() = QuoteLineResponse(id!!, catalogItemId, itemName, description, quantity, unitPrice, taxName, taxRate, lineNet, lineTax, lineTotal)
fun QuoteFollowUpEntity.toResponse() = FollowUpResponse(id!!, quoteId, channel, note, nextContactAt, outcome, createdBy, createdAt)
private fun money(value: BigDecimal) = value.setScale(2, RoundingMode.HALF_UP)
private fun QuoteLineEntity.forQuote(tenantId: UUID, quoteId: UUID) = QuoteLineEntity(
    tenantId = tenantId, quoteId = quoteId, catalogItemId = catalogItemId, itemName = itemName, description = description,
    quantity = quantity, unitPrice = unitPrice, taxName = taxName, taxRate = taxRate,
    lineNet = lineNet, lineTax = lineTax, lineTotal = lineTotal
)
