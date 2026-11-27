package ru.pfc.shared.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.data.repository.AccountBalance
import ru.pfc.shared.data.repository.AccountRepository
import ru.pfc.shared.data.repository.BalanceWarning
import ru.pfc.shared.data.repository.OperationRepository
import ru.pfc.shared.model.AccountType
import ru.pfc.shared.model.Operation
import ru.pfc.shared.model.OperationStatus
import ru.pfc.shared.model.OperationType
import ru.pfc.shared.model.PendingReason
import ru.pfc.shared.model.TransferDirection
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class BalanceTest {
    private val database = inMemoryDatabase()
    private val logged = Collections.synchronizedList(mutableListOf<Triple<String, String, String?>>())
    private val accounts = AccountRepository(database) { table, id, rule -> logged += Triple(table, id, rule) }
    private val operations = OperationRepository(database)

    @AfterTest
    fun close() = database.close()

    private fun balance(accountId: String = "card"): AccountBalance =
        runBlocking { accounts.observeBalances().first() }.single { it.account.id == accountId }

    private fun given(vararg accountIds: String) = runBlocking {
        accountIds.forEach { accounts.save(account(it)) }
    }

    private fun save(vararg items: Operation) = runBlocking { operations.saveAll(items.toList()) }

    @Test
    fun initialBalanceIsShown() {
        given("card")
        save(adjustment("start", rub(30_000)))
        assertEquals(rub(30_000), balance().balance)
        assertEquals(emptySet(), balance().warnings)
    }

    @Test
    fun expenseDecreasesAndIncomeIncreasesBalance() {
        given("card")
        save(
            adjustment("start", rub(30_000)),
            operation("coffee", amount = rub(1_000)),
            operation("salary", type = OperationType.INCOME, amount = rub(5_000)),
        )
        assertEquals(rub(34_000), balance().balance)
    }

    @Test
    fun transferBetweenOwnAccountsMovesMoney() {
        given("card", "cash")
        save(
            adjustment("card-start", rub(30_000), accountId = "card"),
            adjustment("cash-start", rub(0), accountId = "cash"),
            operation(
                "atm",
                type = OperationType.TRANSFER,
                amount = rub(5_000),
                transferDirection = TransferDirection.OUT,
                transferAccountId = "cash",
            ),
        )
        assertEquals(rub(25_000), balance("card").balance)
        assertEquals(rub(5_000), balance("cash").balance)
    }

    @Test
    fun transferFromOutsideIncreasesBalance() {
        given("card")
        save(
            adjustment("start", rub(1_000)),
            operation("from-friend", type = OperationType.TRANSFER, amount = rub(500), transferDirection = TransferDirection.IN),
        )
        assertEquals(rub(1_500), balance().balance)
    }

    @Test
    fun adjustmentSetsBalanceToStatedAmount() {
        given("card")
        save(
            adjustment("start", rub(13_100), occurredAt = t0),
            operation("lunch", amount = rub(500), occurredAt = t0 + 10.minutes),
            adjustment("actual", rub(12_400), occurredAt = t0 + 20.minutes),
        )
        assertEquals(rub(12_400), balance().balance)
    }

    @Test
    fun operationBeforeLatestAdjustmentDoesNotChangeBalance() {
        given("card")
        save(operation("from-shade", occurredAt = before), adjustment("start", rub(30_000)))
        assertEquals(rub(30_000), balance().balance)
    }

    @Test
    fun operationWithTheAdjustmentTimeIsNotCountedTwice() {
        given("card")
        save(operation("purchase", amount = rub(500), occurredAt = t0), adjustment("bank", rub(12_400), occurredAt = t0))
        assertEquals(rub(12_400), balance().balance)
    }

    @Test
    fun laterEditedAdjustmentWinsAmongEqualTimes() {
        given("card")
        save(
            adjustment("first", rub(10_000), occurredAt = t0, updatedAt = t0 + 1.minutes),
            adjustment("second", rub(20_000), occurredAt = t0, updatedAt = t0 + 2.minutes),
        )
        assertEquals(rub(20_000), balance().balance)
    }

    @Test
    fun operationWithoutAccountIsNotInAnyBalance() {
        given("card")
        save(
            adjustment("start", rub(1_000)),
            operation("unknown-bank", accountId = null, pendingReason = PendingReason.NO_ACCOUNT),
        )
        assertEquals(rub(1_000), balance().balance)
    }

    @Test
    fun deletedOperationIsNotInBalance() {
        given("card")
        save(adjustment("start", rub(1_000)), operation("mistake", amount = rub(300), deletedAt = after))
        assertEquals(rub(1_000), balance().balance)
    }

    @Test
    fun pendingReasonDecidesWhetherOperationCounts() {
        given("card")
        save(
            adjustment("start", rub(10_000)),
            operation("over-limit", amount = rub(1_000), pendingReason = PendingReason.OVER_LIMIT),
            operation("new-merchant", amount = rub(2_000), pendingReason = PendingReason.NEW_MERCHANT),
        )
        assertEquals(rub(8_000), balance().balance)
    }

    @Test
    fun creditCardDebtIsNegativeBalance() {
        runBlocking {
            accounts.save(account("credit", type = AccountType.CREDIT_CARD).copy(creditLimit = rub(50_000)))
        }
        save(adjustment("debt", rub(-12_000), accountId = "credit"))
        assertEquals(rub(-12_000), balance("credit").balance)
    }

    @Test
    fun unreadableRowIsSkippedAndMarksBalance() {
        given("card")
        save(adjustment("start", rub(1_000)), operation("ok", amount = rub(100)))
        val broken = operation("broken", amount = rub(300)).toRow().copy(status = "PENDING")
        runBlocking { database.operationDao().upsert(broken) }

        val result = balance()
        assertEquals(rub(900), result.balance)
        assertEquals(setOf(BalanceWarning.UNREADABLE_ROWS), result.warnings)
    }

    @Test
    fun unreadableLatestAdjustmentMarksBalance() {
        given("card")
        save(adjustment("start", rub(1_000)))
        val broken = adjustment("actual", rub(5_000), occurredAt = after).toRow().copy(balanceAfterMinor = null)
        runBlocking { database.operationDao().upsert(broken) }

        assertEquals(setOf(BalanceWarning.UNREADABLE_ROWS), balance().warnings)
    }

    @Test
    fun operationInOtherCurrencyIsSkippedAndMarksOnlyItsAccount() {
        given("card", "cash")
        save(
            adjustment("card-start", rub(1_000), accountId = "card"),
            operation("ok", amount = rub(100)),
            adjustment("cash-start", rub(500), accountId = "cash"),
        )
        val dollars = operation("dollars", amount = Money(30_000, Currency("USD"))).toRow()
        runBlocking { database.operationDao().upsert(dollars) }

        val card = balance("card")
        assertEquals(rub(900), card.balance)
        assertEquals(setOf(BalanceWarning.UNREADABLE_ROWS), card.warnings)
        val cash = balance("cash")
        assertEquals(rub(500), cash.balance)
        assertEquals(emptySet(), cash.warnings)
        assertTrue(Triple("operation", "dollars", "Сумма в валюте, отличной от валюты счёта") in logged)
    }

    @Test
    fun overflowingBalanceShowsLatestAdjustmentAndMarksOnlyItsAccount() {
        given("card", "cash")
        save(adjustment("card-start", rub(1_000), accountId = "card"), adjustment("cash-start", rub(500), accountId = "cash"))
        val huge = operation("huge", type = OperationType.INCOME, amount = Money(Long.MAX_VALUE)).toRow()
        runBlocking { database.operationDao().upsert(huge) }

        val card = balance("card")
        assertEquals(rub(1_000), card.balance)
        assertEquals(setOf(BalanceWarning.UNREADABLE_ROWS), card.warnings)
        val cash = balance("cash")
        assertEquals(rub(500), cash.balance)
        assertEquals(emptySet(), cash.warnings)
        assertTrue(Triple("account", "card", "Переполнение суммы баланса") in logged)
    }

    @Test
    fun actualBalanceAfterUnreadableRowsClearsMark() {
        given("card")
        save(adjustment("start", rub(1_000)))
        val broken = operation("broken", amount = rub(300)).toRow().copy(status = "PENDING")
        val dollars = operation("dollars", amount = Money(30_000, Currency("USD"))).toRow()
        runBlocking {
            database.operationDao().upsert(broken)
            database.operationDao().upsert(dollars)
        }
        assertEquals(setOf(BalanceWarning.UNREADABLE_ROWS), balance().warnings)

        save(adjustment("actual", rub(700), occurredAt = after + 10.minutes))
        val result = balance()
        assertEquals(rub(700), result.balance)
        assertEquals(emptySet(), result.warnings)
    }

    @Test
    fun currencyOfAccountWithOperationsCannotChange() {
        given("card", "cash", "wallet")
        save(
            operation(
                "atm",
                type = OperationType.TRANSFER,
                amount = rub(5_000),
                transferDirection = TransferDirection.OUT,
                transferAccountId = "cash",
            ),
        )
        val dollars = Currency("USD")
        for (id in listOf("card", "cash")) {
            val error = assertFailsWith<IllegalArgumentException> { runBlocking { accounts.save(account(id, currency = dollars)) } }
            assertEquals("Нельзя сменить валюту счёта с операциями", error.message)
        }
        runBlocking {
            accounts.save(account("card").copy(name = "Карта"))
            accounts.save(account("wallet", currency = dollars))
        }

        assertEquals(Currency.RUB, balance("cash").account.currency)
        assertEquals("Карта", balance("card").account.name)
        assertEquals(dollars, balance("wallet").account.currency)
    }

    @Test
    fun foreignPurchaseWithoutAccountAmountMarksBalanceUntilAnswered() {
        given("card")
        val dollars = Money(1_599, Currency("USD"))
        val waiting = operation(
            "netflix",
            amount = null,
            originalAmount = dollars,
            pendingReason = PendingReason.UNPARSED,
        )
        save(adjustment("start", rub(20_000)), waiting)
        assertEquals(setOf(BalanceWarning.FOREIGN_AMOUNT_MISSING), balance().warnings)
        assertEquals(rub(20_000), balance().balance)

        save(waiting.copy(amount = rub(1_520), status = OperationStatus.CONFIRMED, pendingReason = null))
        assertEquals(emptySet(), balance().warnings)
        assertEquals(rub(18_480), balance().balance)
    }
}
