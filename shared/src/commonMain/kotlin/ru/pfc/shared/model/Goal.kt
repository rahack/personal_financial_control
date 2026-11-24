package ru.pfc.shared.model

import kotlinx.datetime.LocalDate
import ru.pfc.shared.money.Money
import kotlin.time.Instant

/** Цель накопления: сумма и желаемая дата. */
data class Goal(
    override val id: String,
    val name: String,
    val target: Money,
    val targetDate: LocalDate? = null,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(target.minorUnits > 0) { "Сумма цели положительна" }
    }
}
