package cl.tratohecho.customers

import cl.tratohecho.customers.domain.CustomerEntity
import cl.tratohecho.customers.domain.CustomerStatus
import cl.tratohecho.customers.repository.CustomerRepository
import cl.tratohecho.customers.repository.CustomerIdempotencyRepository
import cl.tratohecho.customers.service.CustomerService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class CustomerServiceTest {
    @Test
    fun `list filters by tenant repository result and text query`() {
        val tenantId = UUID.randomUUID()
        val repository = mockk<CustomerRepository>()
        val customer = CustomerEntity(
            id = UUID.randomUUID(), tenantId = tenantId, businessName = "Comercial del Sur", rut = "12345678-9",
            status = CustomerStatus.ACTIVE
        )
        every { repository.findAllByTenantIdOrderByBusinessNameAsc(tenantId) } returns listOf(customer)
        val service = CustomerService(repository, mockk<CustomerIdempotencyRepository>())
        assertEquals(1, service.list(tenantId, "sur").size)
        assertEquals(0, service.list(tenantId, "norte").size)
    }
}
