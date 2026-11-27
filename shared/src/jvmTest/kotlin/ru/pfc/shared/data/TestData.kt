package ru.pfc.shared.data

import androidx.room3.Room
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import ru.pfc.shared.model.Account
import ru.pfc.shared.model.AccountType
import ru.pfc.shared.model.Operation
import ru.pfc.shared.model.OperationSource
import ru.pfc.shared.model.OperationStatus
import ru.pfc.shared.model.OperationType
import ru.pfc.shared.model.PendingReason
import ru.pfc.shared.model.TransferDirection
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import ru.pfc.shared.time.AppClock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal val moscow = TimeZone.of("Europe/Moscow")

/** Момент корректировки в тестах баланса; операции до и после него сдвинуты на часы. */
internal val t0 = Instant.parse("2026-11-24T09:00:00Z")

internal val testClock = object : AppClock {
    override fun now() = t0
    override fun timeZone() = moscow
}

internal val before = t0 - 1.hours
internal val after = t0 + 1.hours

internal val today = LocalDate(2026, Month.NOVEMBER, 24)

internal fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(factory = { AppDatabaseConstructor.initialize() }).buildAppDatabase(testClock)

internal fun rub(rubles: Long) = Money(rubles * 100)

internal fun account(
    id: String = "card",
    type: AccountType = AccountType.DEBIT_CARD,
    currency: Currency = Currency.RUB,
) = Account(id = id, name = id, type = type, currency = currency, updatedAt = t0)

internal fun operation(
    id: String,
    type: OperationType? = OperationType.EXPENSE,
    amount: Money? = rub(1_000),
    accountId: String? = "card",
    occurredAt: Instant = after,
    localDate: LocalDate = today,
    pendingReason: PendingReason? = null,
    transferDirection: TransferDirection? = null,
    transferAccountId: String? = null,
    balanceAfter: Money? = null,
    originalAmount: Money? = null,
    categoryId: String? = null,
    updatedAt: Instant = occurredAt,
    deletedAt: Instant? = null,
) = Operation(
    id = id,
    type = type,
    amount = amount,
    originalAmount = originalAmount,
    accountId = accountId,
    occurredAt = occurredAt,
    localDate = localDate,
    status = if (pendingReason == null) OperationStatus.CONFIRMED else OperationStatus.PENDING,
    pendingReason = pendingReason,
    source = OperationSource.MANUAL,
    categoryId = categoryId,
    transferDirection = transferDirection,
    transferAccountId = transferAccountId,
    balanceAfter = balanceAfter,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Корректировка: остаток счёта [balance] на момент [occurredAt] (KTD7). */
internal fun adjustment(
    id: String,
    balance: Money,
    accountId: String = "card",
    occurredAt: Instant = t0,
    updatedAt: Instant = occurredAt,
) = operation(
    id = id,
    type = OperationType.ADJUSTMENT,
    amount = balance,
    accountId = accountId,
    occurredAt = occurredAt,
    balanceAfter = balance,
    updatedAt = updatedAt,
)
