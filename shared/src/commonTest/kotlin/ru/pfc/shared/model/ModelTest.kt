package ru.pfc.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private val now = Instant.parse("2026-11-24T09:00:00Z")

class OperationTest {
    private fun operation(
        type: OperationType? = OperationType.EXPENSE,
        amount: Money? = Money(1_000),
        originalAmount: Money? = null,
        accountId: String? = "account-1",
        status: OperationStatus = OperationStatus.CONFIRMED,
        pendingReason: PendingReason? = null,
        transferDirection: TransferDirection? = null,
        transferAccountId: String? = null,
        balanceAfter: Money? = null,
    ) = Operation(
        id = "operation-1",
        type = type,
        amount = amount,
        originalAmount = originalAmount,
        accountId = accountId,
        occurredAt = now,
        localDate = LocalDate(2026, Month.NOVEMBER, 24),
        status = status,
        pendingReason = pendingReason,
        source = OperationSource.MANUAL,
        transferDirection = transferDirection,
        transferAccountId = transferAccountId,
        balanceAfter = balanceAfter,
        updatedAt = now,
    )

    @Test
    fun pendingOperationNeedsReason() {
        assertFailsWith<IllegalArgumentException> { operation(status = OperationStatus.PENDING) }
    }

    @Test
    fun confirmedOperationHasNoPendingReason() {
        assertFailsWith<IllegalArgumentException> { operation(pendingReason = PendingReason.NEW_MERCHANT) }
    }

    @Test
    fun accountMayBeMissingOnlyForUnparsedOrNoAccount() {
        operation(accountId = null, status = OperationStatus.PENDING, pendingReason = PendingReason.UNPARSED)
        operation(accountId = null, status = OperationStatus.PENDING, pendingReason = PendingReason.NO_ACCOUNT)
        assertFailsWith<IllegalArgumentException> {
            operation(accountId = null, status = OperationStatus.PENDING, pendingReason = PendingReason.NEW_MERCHANT)
        }
        assertFailsWith<IllegalArgumentException> { operation(accountId = null) }
    }

    @Test
    fun unparsedNotificationMayLackTypeAndAmount() {
        operation(type = null, amount = null, accountId = null, status = OperationStatus.PENDING, pendingReason = PendingReason.UNPARSED)
        assertFailsWith<IllegalArgumentException> { operation(amount = null) }
        assertFailsWith<IllegalArgumentException> { operation(type = null) }
        assertFailsWith<IllegalArgumentException> {
            operation(type = null, status = OperationStatus.PENDING, pendingReason = PendingReason.NEW_MERCHANT)
        }
    }

    @Test
    fun foreignPurchaseKeepsBankAmountAndWaitsForAccountCurrencyAmount() {
        val dollars = Money(1_599, Currency("USD"))
        val waiting = operation(
            amount = null,
            originalAmount = dollars,
            status = OperationStatus.PENDING,
            pendingReason = PendingReason.UNPARSED,
        )
        assertFalse(waiting.countsInBalance)
        assertEquals(dollars, operation(amount = Money(150_000), originalAmount = dollars).originalAmount)
        assertFailsWith<IllegalArgumentException> { operation(originalAmount = Money(1_000)) }
        assertFailsWith<IllegalArgumentException> { operation(originalAmount = Money(0, Currency("USD"))) }
    }

    @Test
    fun adjustmentStoresBalanceAtItsMoment() {
        operation(type = OperationType.ADJUSTMENT, amount = Money(-700), balanceAfter = Money(12_400))
        assertFailsWith<IllegalArgumentException> { operation(type = OperationType.ADJUSTMENT) }
        assertFailsWith<IllegalArgumentException> { operation(balanceAfter = Money(12_400)) }
    }

    @Test
    fun nonAdjustmentAmountIsPositive() {
        assertFailsWith<IllegalArgumentException> { operation(amount = Money(0)) }
        assertFailsWith<IllegalArgumentException> { operation(amount = Money(-100)) }
    }

    @Test
    fun incomingTransferFromOutsideKeepsReceivingAccount() {
        val incoming = operation(
            type = OperationType.TRANSFER,
            status = OperationStatus.PENDING,
            pendingReason = PendingReason.UNPAIRED_TRANSFER,
            transferDirection = TransferDirection.IN,
        )
        assertEquals("account-1", incoming.accountId)
        assertEquals(null, incoming.transferAccountId)
    }

    @Test
    fun transferBetweenOwnAccountsIsRecordedFromSourceAccount() {
        operation(type = OperationType.TRANSFER, transferDirection = TransferDirection.OUT, transferAccountId = "account-2")
        assertFailsWith<IllegalArgumentException> {
            operation(type = OperationType.TRANSFER, transferDirection = TransferDirection.IN, transferAccountId = "account-2")
        }
        assertFailsWith<IllegalArgumentException> {
            operation(type = OperationType.TRANSFER, transferDirection = TransferDirection.OUT, transferAccountId = "account-1")
        }
    }

    @Test
    fun onlyTransferHasDirection() {
        assertFailsWith<IllegalArgumentException> { operation(type = OperationType.TRANSFER) }
        assertFailsWith<IllegalArgumentException> { operation(transferDirection = TransferDirection.OUT) }
        assertFailsWith<IllegalArgumentException> {
            operation(transferDirection = TransferDirection.OUT, transferAccountId = "account-2")
        }
    }

    @Test
    fun balanceIncludesConfirmedAndAlreadySpentPendingOperations() {
        assertTrue(operation().countsInBalance)
        for (reason in listOf(PendingReason.NEW_MERCHANT, PendingReason.UNPAIRED_TRANSFER, PendingReason.CHOOSE_ACCOUNT)) {
            assertTrue(operation(status = OperationStatus.PENDING, pendingReason = reason).countsInBalance, "$reason")
        }
        for (reason in listOf(PendingReason.OVER_LIMIT, PendingReason.UNPARSED, PendingReason.NO_ACCOUNT)) {
            assertFalse(operation(status = OperationStatus.PENDING, pendingReason = reason).countsInBalance, "$reason")
        }
    }
}

class AccountTest {
    @Test
    fun creditLimitBelongsOnlyToCreditCard() {
        val card = Account("account-1", "Т-Банк кредитка", AccountType.CREDIT_CARD, creditLimit = Money(5_000_000), updatedAt = now)
        assertEquals(Money(5_000_000), card.creditLimit)
        assertFailsWith<IllegalArgumentException> {
            Account("account-2", "Наличные", AccountType.CASH, creditLimit = Money(100), updatedAt = now)
        }
    }

    @Test
    fun creditLimitUsesAccountCurrency() {
        assertFailsWith<IllegalArgumentException> {
            Account("account-1", "Карта", AccountType.CREDIT_CARD, creditLimit = Money(100, Currency("USD")), updatedAt = now)
        }
    }
}

class PlanningEntitiesTest {
    @Test
    fun amountsArePositive() {
        assertFailsWith<IllegalArgumentException> { Budget("b", "category-1", Money(0), updatedAt = now) }
        assertFailsWith<IllegalArgumentException> { Budget("b", "category-1", Money(-100), updatedAt = now) }
        assertFailsWith<IllegalArgumentException> { Goal("g", "Отпуск", Money(0), updatedAt = now) }
        assertFailsWith<IllegalArgumentException> { Goal("g", "Отпуск", Money(-100), updatedAt = now) }
        Budget("budget-1", "category-1", Money(1_500_000), updatedAt = now)
        Goal("goal-1", "Отпуск", Money(9_000_000), updatedAt = now)
    }
}
