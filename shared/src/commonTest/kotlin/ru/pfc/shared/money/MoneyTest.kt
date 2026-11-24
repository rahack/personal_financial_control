package ru.pfc.shared.money

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
    private val usd = Currency("USD")

    @Test
    fun addsAndSubtractsInSameCurrency() {
        assertEquals(Money(1_500), Money(1_000) + Money(500))
        assertEquals(Money(-500), Money(1_000) - Money(1_500))
        assertEquals(Money(-1_000), -Money(1_000))
    }

    @Test
    fun defaultCurrencyIsRuble() {
        assertEquals(Currency.RUB, Money(100).currency)
    }

    @Test
    fun addingDifferentCurrenciesFails() {
        assertFailsWith<IllegalArgumentException> { Money(100) + Money(100, usd) }
        assertFailsWith<IllegalArgumentException> { Money(100) - Money(100, usd) }
    }

    @Test
    fun comparingDifferentCurrenciesFails() {
        assertFailsWith<IllegalArgumentException> { Money(100) < Money(100, usd) }
    }

    @Test
    fun comparesAmountsInSameCurrency() {
        assertTrue(Money(-1) < Money(0))
        assertTrue(Money(1_000) > Money(999))
    }

    @Test
    fun overflowOnAdditionFails() {
        assertFailsWith<ArithmeticException> { Money(Long.MAX_VALUE) + Money(1) }
        assertFailsWith<ArithmeticException> { Money(Long.MIN_VALUE) + Money(-1) }
    }

    @Test
    fun overflowOnSubtractionAndNegationFails() {
        assertFailsWith<ArithmeticException> { Money(Long.MIN_VALUE) - Money(1) }
        assertFailsWith<ArithmeticException> { -Money(Long.MIN_VALUE) }
    }

    @Test
    fun currencyCodeMustBeThreeUppercaseLetters() {
        assertFailsWith<IllegalArgumentException> { Currency("rub") }
        assertFailsWith<IllegalArgumentException> { Currency("RUBL") }
    }
}
