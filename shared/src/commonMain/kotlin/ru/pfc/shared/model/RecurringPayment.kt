package ru.pfc.shared.model

import ru.pfc.shared.money.Money
import kotlin.time.Instant

/**
 * Регулярный платёж раз в месяц. Число 29–31 в коротком месяце переносится на последний день (KTD8, `dueDate`).
 */
data class RecurringPayment(
    override val id: String,
    val name: String,
    val amount: Money,
    val dayOfMonth: Int,
    val accountId: String? = null,
    val categoryId: String? = null,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(dayOfMonth in 1..31) { "Число месяца вне 1..31: $dayOfMonth" }
        require(amount.minorUnits > 0) { "Сумма платежа положительна" }
    }
}
