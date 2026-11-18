package cl.tratohecho.billing.web

import cl.tratohecho.common.security.currentUser
import cl.tratohecho.billing.service.InvoiceService
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/facturas")
@PreAuthorize("hasAnyRole('ADMIN','FACTURACION')")
class InvoicesController(private val service: InvoiceService) {
    @GetMapping fun list() = service.list(currentUser().tenantId)
    @GetMapping("/{id}") fun get(@PathVariable id: UUID) = service.get(currentUser().tenantId, id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateInvoiceRequest, @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String) =
        service.create(currentUser().tenantId, request, authorization)
    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: UpdateInvoiceRequest) =
        service.update(currentUser().tenantId, id, request)
    @PatchMapping("/{id}/estado")
    fun status(@PathVariable id: UUID, @Valid @RequestBody request: InvoiceStatusRequest) =
        service.changeStatus(currentUser().tenantId, id, request.status)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancel(@PathVariable id: UUID) = service.cancel(currentUser().tenantId, id)
}
