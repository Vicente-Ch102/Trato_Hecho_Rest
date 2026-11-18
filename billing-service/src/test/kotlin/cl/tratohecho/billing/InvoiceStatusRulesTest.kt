package cl.tratohecho.billing

import cl.tratohecho.billing.domain.InvoiceStatus
import cl.tratohecho.billing.service.InvoiceStatusRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InvoiceStatusRulesTest {
    @Test
    fun `invoice can be issued and paid in order`() {
        assertTrue(InvoiceStatusRules.canTransition(InvoiceStatus.BORRADOR, InvoiceStatus.EMITIDA))
        assertTrue(InvoiceStatusRules.canTransition(InvoiceStatus.EMITIDA, InvoiceStatus.PAGADA))
        assertFalse(InvoiceStatusRules.canTransition(InvoiceStatus.BORRADOR, InvoiceStatus.PAGADA))
        assertFalse(InvoiceStatusRules.canTransition(InvoiceStatus.PAGADA, InvoiceStatus.ANULADA))
    }
}
