package ru.pfc.shared.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth

/**
 * Отрезок календарных дней, обе границы включены.
 * Операция попадает в отрезок по своей `localDate` (KTD8): после смены часового пояса она остаётся в том же дне и месяце.
 */
data class DayRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(start <= endInclusive) { "Начало $start позже конца $endInclusive" }
    }

    val endExclusive: LocalDate get() = endInclusive.plus(1, DateTimeUnit.DAY)
}

/** Календарный месяц, в который попадает [date]. */
fun monthRange(date: LocalDate): DayRange = date.yearMonth.let { DayRange(it.firstDay, it.lastDay) }
