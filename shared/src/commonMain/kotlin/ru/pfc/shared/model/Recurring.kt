package ru.pfc.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plus
import ru.pfc.shared.money.Money
import kotlin.time.Instant

enum class RecurringKind { PAYMENT, INCOME }

/** Период повторения, те же, что ищет автопоиск (KTD11). */
enum class RecurringPeriod(val unit: DateTimeUnit.DateBased) {
    WEEK(DateTimeUnit.WEEK),
    TWO_WEEKS(DateTimeUnit.WEEK * 2),
    MONTH(DateTimeUnit.MONTH),
    QUARTER(DateTimeUnit.QUARTER),
    YEAR(DateTimeUnit.YEAR),
}

/**
 * Регулярный платёж или доход (R15): аренда, связь, подписка, аванс, зарплата.
 * Даты повторяются с периодом [period] от даты отсчёта [anchorDate] — дня, когда запись была или будет.
 */
data class Recurring(
    override val id: String,
    val name: String,
    val kind: RecurringKind,
    val amount: Money,
    val period: RecurringPeriod,
    val anchorDate: LocalDate,
    val accountId: String? = null,
    val categoryId: String? = null,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(amount.minorUnits > 0) { "Сумма регулярной записи положительна" }
    }

    /**
     * Ближайшая дата не раньше [onOrAfter]. Каждая дата считается от даты отсчёта, а не от предыдущей,
     * поэтому число 29–31 не уползает: 31 января → 28 февраля → 31 марта (KTD8).
     */
    fun nextDate(onOrAfter: LocalDate): LocalDate {
        if (onOrAfter <= anchorDate) return anchorDate
        val unit = period.unit
        var index = when (unit) {
            is DateTimeUnit.DayBased -> anchorDate.daysUntil(onOrAfter) / unit.days
            is DateTimeUnit.MonthBased -> anchorDate.monthsUntil(onOrAfter) / unit.months
        }
        while (anchorDate.plus(index, unit) < onOrAfter) index++
        return anchorDate.plus(index, unit)
    }
}
