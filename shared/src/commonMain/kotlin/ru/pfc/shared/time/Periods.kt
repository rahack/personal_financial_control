package ru.pfc.shared.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.yearMonth
import kotlin.time.Instant

/** Отрезок календарных дней по местному времени, обе границы включены. */
data class DayRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(start <= endInclusive) { "Начало $start позже конца $endInclusive" }
    }

    val endExclusive: LocalDate get() = endInclusive.plus(1, DateTimeUnit.DAY)

    /** Момент начала первого дня в часовом поясе телефона. */
    fun startIn(timeZone: TimeZone): Instant = start.atStartOfDayIn(timeZone)

    /** Момент начала дня, следующего за последним: граница не входит в отрезок. */
    fun endExclusiveIn(timeZone: TimeZone): Instant = endExclusive.atStartOfDayIn(timeZone)
}

/** Календарный месяц, в который попадает [date]. */
fun monthRange(date: LocalDate): DayRange = date.yearMonth.let { DayRange(it.firstDay, it.lastDay) }

/**
 * Дата платежа с числом [dayOfMonth] в указанном месяце (KTD8).
 * Если в месяце меньше дней, платёж переносится на последний день: 31-е в феврале 2027 года — 28 февраля.
 */
fun dueDate(year: Int, month: Month, dayOfMonth: Int): LocalDate {
    require(dayOfMonth in 1..31) { "Число месяца вне 1..31: $dayOfMonth" }
    return LocalDate(year, month, minOf(dayOfMonth, YearMonth(year, month).numberOfDays))
}
