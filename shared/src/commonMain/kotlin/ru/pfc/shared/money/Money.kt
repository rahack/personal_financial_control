package ru.pfc.shared.money

/** Код валюты ISO 4217. В MVP везде рубль (R11), другая валюта приходит только из уведомлений (A13). */
@JvmInline
value class Currency(val code: String) {
    init {
        require(code.length == 3 && code.all { it in 'A'..'Z' }) { "Код валюты — три заглавные латинские буквы: $code" }
    }

    companion object {
        val RUB = Currency("RUB")
    }
}

/**
 * Сумма в сотых долях единицы валюты: в копейках для рубля (T1).
 * Сотые доли берутся для любой валюты, и для иены без дробной части тоже: 1 500 JPY хранится как 150 000,
 * иначе [format] показал бы сумму в 100 раз меньше.
 * Сложение и сравнение сумм в разных валютах и переполнение `Long` — ошибки, а не тихий неверный результат.
 */
data class Money(val minorUnits: Long, val currency: Currency = Currency.RUB) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        val result = minorUnits + other.minorUnits
        // Переполнение: оба слагаемых одного знака, а сумма — другого.
        if ((minorUnits xor result) and (other.minorUnits xor result) < 0) throw ArithmeticException("Переполнение суммы")
        return Money(result, currency)
    }

    operator fun minus(other: Money): Money = this + -other

    operator fun unaryMinus(): Money {
        if (minorUnits == Long.MIN_VALUE) throw ArithmeticException("Переполнение суммы")
        return Money(-minorUnits, currency)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) { "Разные валюты: ${currency.code} и ${other.currency.code}" }
    }
}
