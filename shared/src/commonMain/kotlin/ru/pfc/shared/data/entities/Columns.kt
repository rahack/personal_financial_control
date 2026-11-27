package ru.pfc.shared.data.entities

import kotlinx.datetime.LocalDate
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import kotlin.time.Instant

// Строки базы хранят только простые значения. Перевод в сущности модели бросает IllegalArgumentException,
// если строка нарушает правила модели: чтение пропускает такую строку (U3, шаг 7).

/** Строка любой таблицы; её id попадает в журнал, если строку не удалось прочитать. */
internal interface Row {
    val id: String
}

internal fun Instant.toColumn(): Long = toEpochMilliseconds()

internal fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun LocalDate.toColumn(): String = toString()

internal fun String.toLocalDate(): LocalDate = LocalDate.parse(this)

internal fun money(minorUnits: Long?, currency: String?): Money? =
    minorUnits?.let { Money(it, Currency(requireNotNull(currency) { "Сумма без валюты" })) }
