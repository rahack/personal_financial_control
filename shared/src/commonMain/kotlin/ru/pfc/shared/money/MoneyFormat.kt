package ru.pfc.shared.money

import kotlin.math.abs

private const val NBSP = ' '
private const val MINUS = '−'

/**
 * Сумма для интерфейса по UX §6: разряды через неразрывный пробел, знак «₽» (у других валют — код),
 * копейки после запятой только если они не нулевые. Например, «12 400 ₽» и «1 234,50 ₽».
 */
fun Money.format(): String {
    val units = abs(minorUnits / 100).toString()
    val cents = abs(minorUnits % 100)
    return buildString {
        if (minorUnits < 0) append(MINUS)
        append(units.reversed().chunked(3).joinToString(NBSP.toString()).reversed())
        if (cents != 0L) append(',').append(cents.toString().padStart(2, '0'))
        append(NBSP).append(if (currency == Currency.RUB) "₽" else currency.code)
    }
}
