package ru.pfc.shared.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** Источник текущего момента и часового пояса телефона (KTD8). В тестах подменяется. */
interface AppClock {
    fun now(): Instant
    fun timeZone(): TimeZone
}

/** Сегодняшняя дата по местному времени телефона. */
fun AppClock.today(): LocalDate = now().toLocalDateTime(timeZone()).date

object SystemAppClock : AppClock {
    override fun now(): Instant = Clock.System.now()
    override fun timeZone(): TimeZone = TimeZone.currentSystemDefault()
}
