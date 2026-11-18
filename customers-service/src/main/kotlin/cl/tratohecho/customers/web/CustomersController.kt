package cl.tratohecho.customers.web

import cl.tratohecho.common.security.currentUser
import cl.tratohecho.customers.service.CustomerService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/clientes")
class CustomersController(private val service: CustomerService) {
    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun list(@RequestParam(required = false) q: String?) = service.list(currentUser().tenantId, q)
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','FACTURACION')")
    fun get(@PathVariable id: UUID) = service.get(currentUser().tenantId, id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun create(@Valid @RequestBody request: CustomerRequest, @RequestHeader(name = "Idempotency-Key", required = false) key: String?) = service.create(currentUser().tenantId, request, key)
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: CustomerRequest) = service.update(currentUser().tenantId, id, request)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    fun deactivate(@PathVariable id: UUID) = service.deactivate(currentUser().tenantId, id)
}
