package ru.pfc.shared.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class PeriodsTest {
    private val moscow = TimeZone.of("Europe/Moscow")

    @Test
    fun decemberRangeEndsBeforeNextYear() {
        val december = monthRange(LocalDate(2026, Month.DECEMBER, 15))
        assertEquals(LocalDate(2026, Month.DECEMBER, 1), december.start)
        assertEquals(LocalDate(2026, Month.DECEMBER, 31), december.endInclusive)
        assertEquals(LocalDate(2027, Month.JANUARY, 1), december.endExclusive)
    }

    @Test
    fun twentyDaysFromNovemberEleventhToNextMonth() {
        val today = LocalDate(2026, Month.NOVEMBER, 11)
        assertEquals(20, today.daysUntil(monthRange(today).endExclusive))
    }

    @Test
    fun rangeStartAfterEndFails() {
        assertFailsWith<IllegalArgumentException> {
            DayRange(LocalDate(2026, Month.DECEMBER, 2), LocalDate(2026, Month.DECEMBER, 1))
        }
    }

    @Test
    fun todayUsesPhoneTimeZone() {
        val clock = object : AppClock {
            override fun now() = Instant.parse("2026-11-23T21:30:00Z")
            override fun timeZone() = moscow
        }
        assertEquals(LocalDate(2026, Month.NOVEMBER, 24), clock.today())
    }
}
