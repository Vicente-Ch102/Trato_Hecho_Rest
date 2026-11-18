package cl.tratohecho.commercial.web

import cl.tratohecho.common.security.currentUser
import cl.tratohecho.commercial.service.*
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/cotizaciones")
class QuotesController(private val service: QuoteService) {
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun list() = service.list(currentUser().tenantId)
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun get(@PathVariable id: UUID) = service.get(currentUser().tenantId, id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun create(
        @Valid @RequestBody request: QuoteRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(name = "Idempotency-Key", required = false) key: String?
    ) = service.create(currentUser().tenantId, request, authorization, key)
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: QuoteRequest, @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String) =
        service.update(currentUser().tenantId, id, request, authorization)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun delete(@PathVariable id: UUID) = service.delete(currentUser().tenantId, id)
    @PatchMapping("/{id}/estado") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun status(@PathVariable id: UUID, @Valid @RequestBody request: QuoteStatusRequest) =
        service.changeStatus(currentUser().tenantId, id, request.status)
    @GetMapping("/{id}/seguimientos") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun followUps(@PathVariable id: UUID) = service.listFollowUps(currentUser().tenantId, id)
    @PostMapping("/{id}/seguimientos") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun addFollowUp(@PathVariable id: UUID, @Valid @RequestBody request: FollowUpRequest) =
        service.addFollowUp(currentUser().tenantId, id, currentUser().userId, request)
}

@RestController
@RequestMapping("/api/v1/catalogos")
class CatalogController(private val service: CatalogService) {
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun list() = service.list(currentUser().tenantId)
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun get(@PathVariable id: UUID) = service.get(currentUser().tenantId, id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: CatalogItemRequest) = service.create(currentUser().tenantId, request)
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: CatalogItemRequest) = service.update(currentUser().tenantId, id, request)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: UUID) = service.delete(currentUser().tenantId, id)
}

@RestController
@RequestMapping("/api/v1/impuestos")
class TaxController(private val service: TaxService) {
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun list() = service.list(currentUser().tenantId)
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun get(@PathVariable id: UUID) = service.get(currentUser().tenantId, id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: TaxRequest) = service.create(currentUser().tenantId, request)
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: TaxRequest) = service.update(currentUser().tenantId, id, request)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: UUID) = service.delete(currentUser().tenantId, id)
}
