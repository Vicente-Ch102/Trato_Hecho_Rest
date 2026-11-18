package cl.tratohecho.customers.service

import cl.tratohecho.customers.domain.CustomerEntity
import cl.tratohecho.customers.domain.CustomerStatus
import cl.tratohecho.customers.domain.CustomerIdempotencyEntity
import cl.tratohecho.customers.repository.CustomerRepository
import cl.tratohecho.customers.web.CustomerRequest
import cl.tratohecho.customers.web.CustomerResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class CustomerService(private val customers: CustomerRepository, private val idempotency: cl.tratohecho.customers.repository.CustomerIdempotencyRepository) {
    fun list(tenantId: UUID, query: String?): List<CustomerResponse> {
        val all = customers.findAllByTenantIdOrderByBusinessNameAsc(tenantId)
        val filtered = query?.trim()?.takeIf { it.isNotEmpty() }?.let { q ->
            all.filter {
                it.businessName.contains(q, true) || it.rut.contains(q, true) ||
                    (it.contactName?.contains(q, true) == true) || (it.email?.contains(q, true) == true)
            }
        } ?: all
        return filtered.map { it.toResponse() }
    }

    fun get(tenantId: UUID, id: UUID): CustomerResponse = find(tenantId, id).toResponse()

    @Transactional
    fun create(tenantId: UUID, request: CustomerRequest, idempotencyKey: String?): CustomerResponse {
        val key = idempotencyKey?.trim()?.takeIf { it.isNotEmpty() }
        if (key != null) {
            if (key.length > 80) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key no puede superar 80 caracteres")
            idempotency.findByTenantIdAndKeyAndResourceType(tenantId, key, "CUSTOMER")?.let { record ->
                return find(tenantId, record.resourceId).toResponse()
            }
        }
        val rut = normalizeRut(request.rut)
        if (customers.existsByTenantIdAndRutIgnoreCase(tenantId, rut)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese RUT")
        }
        val saved = customers.save(CustomerEntity(
            tenantId = tenantId,
            businessName = request.businessName.trim(), rut = rut,
            contactName = request.contactName?.trimOrNull(), email = request.email?.trimOrNull()?.lowercase(),
            phone = request.phone?.trimOrNull(), address = request.address?.trimOrNull(), notes = request.notes?.trimOrNull()
        ))
        if (key != null) idempotency.save(CustomerIdempotencyEntity(
            tenantId = tenantId, key = key, resourceType = "CUSTOMER", resourceId = saved.id!!
        ))
        return saved.toResponse()
    }

    @Transactional
    fun update(tenantId: UUID, id: UUID, request: CustomerRequest): CustomerResponse {
        val customer = find(tenantId, id)
        val rut = normalizeRut(request.rut)
        if (customers.existsByTenantIdAndRutIgnoreCaseAndIdNot(tenantId, rut, id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Otro cliente ya utiliza ese RUT")
        }
        customer.businessName = request.businessName.trim()
        customer.rut = rut
        customer.contactName = request.contactName?.trimOrNull()
        customer.email = request.email?.trimOrNull()?.lowercase()
        customer.phone = request.phone?.trimOrNull()
        customer.address = request.address?.trimOrNull()
        customer.notes = request.notes?.trimOrNull()
        return customer.toResponse()
    }

    @Transactional
    fun deactivate(tenantId: UUID, id: UUID) {
        val customer = find(tenantId, id)
        customer.status = CustomerStatus.INACTIVE
    }

    private fun find(tenantId: UUID, id: UUID) = customers.findByIdAndTenantId(id, tenantId)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado")

    private fun normalizeRut(value: String) = value.trim().uppercase().replace(".", "").replace(" ", "")
}

private fun String.trimOrNull(): String? = trim().takeIf { it.isNotEmpty() }

fun CustomerEntity.toResponse() = CustomerResponse(
    id = id ?: error("Cliente sin identificador"), businessName = businessName, rut = rut,
    contactName = contactName, email = email, phone = phone, address = address, notes = notes,
    status = status, createdAt = createdAt, updatedAt = updatedAt
)
