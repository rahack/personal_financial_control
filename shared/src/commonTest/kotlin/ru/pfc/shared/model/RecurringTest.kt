package ru.pfc.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import ru.pfc.shared.money.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class RecurringTest {
    private fun recurring(period: RecurringPeriod, anchorDate: LocalDate, amount: Money = Money(3_000_000)) = Recurring(
        id = "recurring-1",
        name = "Аренда",
        kind = RecurringKind.PAYMENT,
        amount = amount,
        period = period,
        anchorDate = anchorDate,
        updatedAt = Instant.parse("2026-11-24T09:00:00Z"),
    )

    @Test
    fun amountIsPositive() {
        assertFailsWith<IllegalArgumentException> { recurring(RecurringPeriod.MONTH, LocalDate(2026, Month.DECEMBER, 1), Money(0)) }
        assertFailsWith<IllegalArgumentException> { recurring(RecurringPeriod.MONTH, LocalDate(2026, Month.DECEMBER, 1), Money(-100)) }
    }

    @Test
    fun dateBeforeAnchorGivesAnchor() {
        val rent = recurring(RecurringPeriod.MONTH, LocalDate(2026, Month.DECEMBER, 5))
        assertEquals(LocalDate(2026, Month.DECEMBER, 5), rent.nextDate(LocalDate(2026, Month.NOVEMBER, 24)))
    }

    @Test
    fun dueDayItselfIsNextDate() {
        val rent = recurring(RecurringPeriod.MONTH, LocalDate(2026, Month.OCTOBER, 5))
        assertEquals(LocalDate(2026, Month.DECEMBER, 5), rent.nextDate(LocalDate(2026, Month.DECEMBER, 5)))
        assertEquals(LocalDate(2027, Month.JANUARY, 5), rent.nextDate(LocalDate(2026, Month.DECEMBER, 6)))
    }

    @Test
    fun monthlyDayAfterMonthEndMovesToLastDayAndDoesNotDrift() {
        val rent = recurring(RecurringPeriod.MONTH, LocalDate(2027, Month.JANUARY, 31))
        assertEquals(LocalDate(2027, Month.FEBRUARY, 28), rent.nextDate(LocalDate(2027, Month.FEBRUARY, 1)))
        assertEquals(LocalDate(2027, Month.MARCH, 31), rent.nextDate(LocalDate(2027, Month.MARCH, 1)))
        assertEquals(LocalDate(2028, Month.FEBRUARY, 29), rent.nextDate(LocalDate(2028, Month.FEBRUARY, 1)))
    }

    @Test
    fun weeklyPeriodsCountDays() {
        val anchor = LocalDate(2026, Month.NOVEMBER, 2)
        val after = LocalDate(2026, Month.NOVEMBER, 17)
        assertEquals(LocalDate(2026, Month.NOVEMBER, 23), recurring(RecurringPeriod.WEEK, anchor).nextDate(after))
        assertEquals(LocalDate(2026, Month.NOVEMBER, 30), recurring(RecurringPeriod.TWO_WEEKS, anchor).nextDate(after))
    }

    @Test
    fun quarterAndYearCountMonths() {
        val insurance = recurring(RecurringPeriod.QUARTER, LocalDate(2027, Month.JANUARY, 15))
        assertEquals(LocalDate(2027, Month.APRIL, 15), insurance.nextDate(LocalDate(2027, Month.JANUARY, 16)))

        val subscription = recurring(RecurringPeriod.YEAR, LocalDate(2028, Month.FEBRUARY, 29))
        assertEquals(LocalDate(2029, Month.FEBRUARY, 28), subscription.nextDate(LocalDate(2028, Month.MARCH, 1)))
        assertEquals(LocalDate(2032, Month.FEBRUARY, 29), subscription.nextDate(LocalDate(2031, Month.MARCH, 1)))
    }
}
