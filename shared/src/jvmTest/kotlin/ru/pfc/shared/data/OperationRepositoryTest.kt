package ru.pfc.shared.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.data.repository.AccountRepository
import ru.pfc.shared.data.repository.OperationRepository
import ru.pfc.shared.model.Operation
import ru.pfc.shared.model.OperationType
import ru.pfc.shared.model.PendingReason
import ru.pfc.shared.model.TransferDirection
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import ru.pfc.shared.time.DayRange
import ru.pfc.shared.time.monthRange
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class OperationRepositoryTest {
    private val database = inMemoryDatabase()
    private val accounts = AccountRepository(database)
    private val operations = OperationRepository(database)
    private val november = monthRange(today)

    init {
        runBlocking { accounts.save(account("card")) }
    }

    @AfterTest
    fun close() = database.close()

    private fun feed(range: DayRange = november): List<Operation> =
        runBlocking { operations.observeOperations(range).first() }

    private fun analytics(): List<Operation> = runBlocking { operations.observeIncomeAndExpenses(november).first() }

    @Test
    fun operationInAnotherCurrencyIsRejected() {
        val dollars = operation("usd", amount = Money(1_599, Currency("USD")))
        assertFailsWith<IllegalArgumentException> { runBlocking { operations.save(dollars) } }
        assertEquals(emptyList(), feed())
    }

    @Test
    fun adjustmentBalanceInAnotherCurrencyIsRejected() {
        val wrong = adjustment("start", rub(100)).copy(balanceAfter = Money(100, Currency("USD")))
        assertFailsWith<IllegalArgumentException> { runBlocking { operations.save(wrong) } }
    }

    @Test
    fun transferToAccountInAnotherCurrencyIsRejected() {
        runBlocking { accounts.save(account("usd", currency = Currency("USD"))) }
        val transfer = operation(
            "to-usd",
            type = OperationType.TRANSFER,
            amount = rub(100),
            transferDirection = TransferDirection.OUT,
            transferAccountId = "usd",
        )
        assertFailsWith<IllegalArgumentException> { runBlocking { operations.save(transfer) } }
        assertEquals(emptyList(), feed())
    }

    @Test
    fun operationOnUnknownAccountIsRejected() {
        assertFailsWith<IllegalArgumentException> { runBlocking { operations.save(operation("lost", accountId = "nowhere")) } }
    }

    @Test
    fun severalOperationsAreWrittenAllOrNothing() {
        val good = operation("good")
        val bad = operation("bad", amount = Money(100, Currency("EUR")))
        assertFailsWith<IllegalArgumentException> { runBlocking { operations.saveAll(listOf(good, bad)) } }
        assertEquals(emptyList(), feed())
    }

    @Test
    fun feedSkipsDeletedOperationsAndShowsPendingOnesNewestFirst() {
        runBlocking {
            operations.saveAll(
                listOf(
                    operation("older", occurredAt = before),
                    operation("newer", occurredAt = after),
                    operation("no-account", accountId = null, pendingReason = PendingReason.NO_ACCOUNT, occurredAt = t0),
                    operation("deleted", deletedAt = after),
                ),
            )
        }
        assertEquals(listOf("newer", "no-account", "older"), feed().map { it.id })
    }

    @Test
    fun unreadableRowDoesNotBreakFeed() {
        runBlocking {
            operations.save(operation("ok"))
            database.operationDao().upsert(operation("broken").toRow().copy(type = "REFUND"))
        }
        val skipped = mutableListOf<String>()
        val logged = OperationRepository(database) { table, id, _ -> skipped += "$table/$id" }
        val result = runBlocking { logged.observeOperations(november).first() }
        assertEquals(listOf("ok"), result.map { it.id })
        assertEquals(listOf("operation/broken"), skipped)
    }

    @Test
    fun analyticsHasIncomeAndExpensesButNoAdjustmentsOrTransfers() {
        runBlocking {
            accounts.save(account("cash"))
            operations.saveAll(
                listOf(
                    operation("before-start", occurredAt = before),
                    adjustment("start", rub(30_000)),
                    operation("salary", type = OperationType.INCOME),
                    operation(
                        "atm",
                        type = OperationType.TRANSFER,
                        transferDirection = TransferDirection.OUT,
                        transferAccountId = "cash",
                    ),
                ),
            )
        }
        assertEquals(setOf("before-start", "salary"), analytics().map { it.id }.toSet())
        assertEquals(setOf("before-start", "start", "salary", "atm"), feed().map { it.id }.toSet())
    }

    @Test
    fun purchaseStaysInItsLocalMonthAfterTimeZoneChange() {
        val moment = Instant.parse("2026-11-30T20:30:00Z")
        val novosibirsk = TimeZone.of("Asia/Novosibirsk")
        assertEquals(LocalDate(2026, Month.DECEMBER, 1), moment.toLocalDateTime(novosibirsk).date)

        val localDate = moment.toLocalDateTime(moscow).date
        runBlocking { operations.save(operation("late-dinner", occurredAt = moment, localDate = localDate)) }

        assertEquals(listOf("late-dinner"), feed(monthRange(LocalDate(2026, Month.NOVEMBER, 30))).map { it.id })
        assertEquals(emptyList(), feed(monthRange(LocalDate(2026, Month.DECEMBER, 1))))
    }
}
