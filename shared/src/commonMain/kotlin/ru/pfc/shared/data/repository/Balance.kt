package ru.pfc.shared.data.repository

import ru.pfc.shared.model.Account
import ru.pfc.shared.model.Operation
import ru.pfc.shared.model.OperationType
import ru.pfc.shared.model.TransferDirection
import ru.pfc.shared.money.Money

/** Почему баланс счёта может быть неточным; главный экран (U8) и счета (U10) показывают пометку и действие. */
enum class BalanceWarning {
    /** Часть записей счёта не прочитана. Действие — «Указать фактический остаток» (R40). */
    UNREADABLE_ROWS,

    /** Покупка в иностранной валюте ждёт суммы в валюте счёта (A13). Действие — «Указать сумму в рублях». */
    FOREIGN_AMOUNT_MISSING,
}

data class AccountBalance(val account: Account, val balance: Money, val warnings: Set<BalanceWarning>)

/**
 * Баланс счёта (KTD7): остаток последней корректировки плюс операции строго позже неё, которые входят в баланс (A2).
 * Без корректировок баланс считается от нуля.
 */
internal fun balanceOf(account: Account, operations: List<Operation>): Money {
    val own = operations.filter {
        it.deletedAt == null && (it.accountId == account.id || it.transferAccountId == account.id)
    }
    val anchor = latestAdjustment(account, own)
    var balance = anchor?.balanceAfter ?: Money(0, account.currency)
    for (operation in own) {
        if (anchor != null && operation.occurredAt <= anchor.occurredAt) continue
        if (!operation.countsInBalance) continue
        balance += operation.signedAmountFor(account.id) ?: continue
    }
    return balance
}

/**
 * Корректировка, от которой считается баланс счёта (KTD7): последняя по времени операции,
 * из корректировок с одним временем — изменённая позже. null — корректировок нет.
 */
internal fun latestAdjustment(account: Account, operations: List<Operation>): Operation? =
    operations
        .filter { it.deletedAt == null && it.type == OperationType.ADJUSTMENT && it.accountId == account.id }
        .maxWithOrNull(compareBy<Operation> { it.occurredAt }.thenBy { it.updatedAt })

/** Как операция меняет баланс счёта [accountId]; null — не меняет. */
private fun Operation.signedAmountFor(accountId: String): Money? {
    val amount = amount ?: return null
    return when (type) {
        OperationType.EXPENSE -> -amount
        OperationType.INCOME -> amount
        OperationType.TRANSFER -> when {
            accountId == transferAccountId -> amount
            transferDirection == TransferDirection.OUT -> -amount
            else -> amount
        }
        OperationType.ADJUSTMENT, null -> null
    }
}
