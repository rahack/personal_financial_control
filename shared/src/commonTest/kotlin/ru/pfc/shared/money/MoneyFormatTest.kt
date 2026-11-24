package ru.pfc.shared.money

import kotlin.test.Test
import kotlin.test.assertEquals

private const val NBSP = " "

class MoneyFormatTest {
    @Test
    fun groupsThousandsAndOmitsZeroKopecks() {
        assertEquals("12${NBSP}400${NBSP}₽", Money(1_240_000).format())
    }

    @Test
    fun formatsZero() {
        assertEquals("0${NBSP}₽", Money(0).format())
    }

    @Test
    fun showsNonZeroKopecksAfterComma() {
        assertEquals("1${NBSP}234,50${NBSP}₽", Money(123_450).format())
    }

    @Test
    fun padsSingleDigitKopecks() {
        assertEquals("0,05${NBSP}₽", Money(5).format())
    }

    @Test
    fun prefixesNegativeAmountWithMinusSign() {
        assertEquals("−12${NBSP}400${NBSP}₽", Money(-1_240_000).format())
    }

    @Test
    fun keepsMinusForNegativeAmountBelowOneRuble() {
        assertEquals("−0,05${NBSP}₽", Money(-5).format())
        assertEquals("−1${NBSP}234,50${NBSP}₽", Money(-123_450).format())
    }

    @Test
    fun groupsMillions() {
        assertEquals("1${NBSP}000${NBSP}000${NBSP}₽", Money(100_000_000).format())
    }

    @Test
    fun showsCurrencyCodeForNonRubleAmounts() {
        assertEquals("15,99${NBSP}USD", Money(1_599, Currency("USD")).format())
    }
}
