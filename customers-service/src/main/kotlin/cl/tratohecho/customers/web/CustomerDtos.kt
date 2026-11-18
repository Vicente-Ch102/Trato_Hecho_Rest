package cl.tratohecho.customers.web

import cl.tratohecho.customers.domain.CustomerStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CustomerRequest(
    @field:NotBlank @field:Size(max = 180) val businessName: String,
    @field:NotBlank @field:Size(max = 15) val rut: String,
    @field:Size(max = 120) val contactName: String? = null,
    @field:Email @field:Size(max = 190) val email: String? = null,
    @field:Size(max = 30) val phone: String? = null,
    @field:Size(max = 250) val address: String? = null,
    val notes: String? = null
)

data class CustomerResponse(
    val id: UUID,
    val businessName: String,
    val rut: String,
    val contactName: String?,
    val email: String?,
    val phone: String?,
    val address: String?,
    val notes: String?,
    val status: CustomerStatus,
    val createdAt: Instant,
    val updatedAt: Instant
)
