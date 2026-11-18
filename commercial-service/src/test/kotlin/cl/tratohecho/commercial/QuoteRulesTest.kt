package cl.tratohecho.commercial

import cl.tratohecho.commercial.domain.QuoteStatus
import cl.tratohecho.commercial.service.QuoteCalculator
import cl.tratohecho.commercial.service.QuoteStatusRules
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class QuoteRulesTest {
    @Test
    fun `calculates line net tax and total with two decimal places`() {
        val result = QuoteCalculator.calculate(BigDecimal("2.5"), BigDecimal("1000"), BigDecimal("19"))
        assertEquals(BigDecimal("2500.00"), result.net)
        assertEquals(BigDecimal("475.00"), result.tax)
        assertEquals(BigDecimal("2975.00"), result.total)
    }

    @Test
    fun `only permits defined quote transitions`() {
        assertTrue(QuoteStatusRules.canTransition(QuoteStatus.BORRADOR, QuoteStatus.ENVIADA))
        assertTrue(QuoteStatusRules.canTransition(QuoteStatus.ENVIADA, QuoteStatus.ACEPTADA))
        assertFalse(QuoteStatusRules.canTransition(QuoteStatus.ACEPTADA, QuoteStatus.RECHAZADA))
    }
}
