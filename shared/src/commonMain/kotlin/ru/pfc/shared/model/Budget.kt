package ru.pfc.shared.model

import ru.pfc.shared.money.Money
import kotlin.time.Instant

/** Лимит трат по категории на календарный месяц. */
data class Budget(
    override val id: String,
    val categoryId: String,
    val monthlyLimit: Money,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(monthlyLimit.minorUnits > 0) { "Лимит бюджета положителен" }
    }
}
