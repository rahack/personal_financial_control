package ru.pfc.shared.model

import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import kotlin.time.Instant

enum class AccountType { DEBIT_CARD, CREDIT_CARD, CASH }

/** Банки, уведомления которых приложение читает в MVP. */
enum class Bank { TBANK, VTB, OZON }

/**
 * Счёт пользователя (R11). Баланс не хранится: он считается от последней корректировки (KTD7).
 * У кредитной карты хранится лимит; задолженность — это отрицательный баланс.
 */
data class Account(
    override val id: String,
    val name: String,
    val type: AccountType,
    val currency: Currency = Currency.RUB,
    val bank: Bank? = null,
    val creditLimit: Money? = null,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(creditLimit == null || type == AccountType.CREDIT_CARD) { "Лимит бывает только у кредитной карты" }
        require(creditLimit == null || creditLimit.currency == currency) { "Лимит в валюте, отличной от валюты счёта" }
    }
}

/** Последние четыре цифры карты: по ним конвейер находит счёт операции из уведомления (KTD14, шаг 2). */
data class CardMask(
    override val id: String,
    val accountId: String,
    val lastDigits: String,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(lastDigits.length == 4 && lastDigits.all { it in '0'..'9' }) { "Маска карты — четыре цифры" }
    }
}

/** Остаток из уведомления банка [bankBalance], который разошёлся с балансом счёта на момент уведомления (R10). */
data class BalanceDiscrepancy(
    override val id: String,
    val accountId: String,
    val bankBalance: Money,
    val notifiedAt: Instant,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity
