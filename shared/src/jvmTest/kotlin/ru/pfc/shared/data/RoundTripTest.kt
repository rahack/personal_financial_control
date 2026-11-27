package ru.pfc.shared.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import ru.pfc.shared.data.repository.AccountRepository
import ru.pfc.shared.data.repository.CategoryRepository
import ru.pfc.shared.data.repository.OperationRepository
import ru.pfc.shared.data.repository.PlanningRepository
import ru.pfc.shared.model.AccountType
import ru.pfc.shared.model.BalanceDiscrepancy
import ru.pfc.shared.model.Bank
import ru.pfc.shared.model.Budget
import ru.pfc.shared.model.CardMask
import ru.pfc.shared.model.Goal
import ru.pfc.shared.model.GoalContribution
import ru.pfc.shared.model.MerchantRule
import ru.pfc.shared.model.Operation
import ru.pfc.shared.model.OperationSource
import ru.pfc.shared.model.OperationStatus
import ru.pfc.shared.model.OperationType
import ru.pfc.shared.model.PendingReason
import ru.pfc.shared.model.Recurring
import ru.pfc.shared.model.RecurringFulfillment
import ru.pfc.shared.model.RecurringKind
import ru.pfc.shared.model.RecurringPeriod
import ru.pfc.shared.model.TransferDirection
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money
import ru.pfc.shared.time.monthRange
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Каждая сущность после записи и чтения равна исходной: перевод в строки таблиц не теряет полей. */
class RoundTripTest {
    private val database = inMemoryDatabase()
    private val accounts = AccountRepository(database)
    private val operations = OperationRepository(database)
    private val categories = CategoryRepository(database)
    private val planning = PlanningRepository(database)

    @AfterTest
    fun close() = database.close()

    private fun <T> Flow<List<T>>.now(): List<T> = runBlocking { first() }

    @Test
    fun accountsMasksAndDiscrepancies() {
        val card = account("card", type = AccountType.CREDIT_CARD).copy(bank = Bank.TBANK, creditLimit = rub(50_000))
        val mask = CardMask("mask", accountId = "card", lastDigits = "0042", updatedAt = t0)
        val discrepancy = BalanceDiscrepancy("gap", accountId = "card", bankBalance = rub(-1_200), notifiedAt = before, updatedAt = t0)
        runBlocking {
            accounts.save(card)
            accounts.save(mask)
            accounts.save(discrepancy)
        }
        assertEquals(listOf(card), accounts.observeAccounts().now())
        assertEquals(listOf(mask), accounts.observeCardMasks().now())
        assertEquals(listOf(discrepancy), accounts.observeDiscrepancies().now())
    }

    @Test
    fun operationWithEveryField() {
        val full = Operation(
            id = "full",
            type = OperationType.TRANSFER,
            amount = rub(5_000),
            originalAmount = Money(5_500, Currency("EUR")),
            accountId = "card",
            occurredAt = before,
            localDate = today,
            status = OperationStatus.PENDING,
            pendingReason = PendingReason.CHOOSE_ACCOUNT,
            source = OperationSource.NOTIFICATION,
            categoryId = "default.travel",
            merchant = "Booking",
            comment = "отель",
            bank = Bank.VTB,
            transferDirection = TransferDirection.OUT,
            transferAccountId = "cash",
            refundOfId = "purchase",
            updatedAt = t0,
        )
        runBlocking {
            accounts.save(account("card"))
            accounts.save(account("cash"))
            operations.save(full)
        }
        assertEquals(listOf(full), operations.observeOperations(monthRange(today)).now())
    }

    @Test
    fun categoriesAndMerchantRules() {
        val rule = MerchantRule("rule", merchantKey = "пятерочка", categoryId = "default.groceries", updatedAt = t0)
        runBlocking { categories.save(rule) }
        assertEquals(listOf(rule), categories.observeMerchantRules().now())
    }

    @Test
    fun planningEntities() {
        val salary = Recurring(
            id = "salary",
            name = "Зарплата",
            kind = RecurringKind.INCOME,
            amount = rub(90_000),
            period = RecurringPeriod.MONTH,
            anchorDate = LocalDate(2026, Month.OCTOBER, 10),
            accountId = "card",
            categoryId = "default.salary",
            updatedAt = t0,
        )
        val paid = RecurringFulfillment("paid", recurringId = "salary", dueDate = LocalDate(2026, Month.NOVEMBER, 10), operationId = "op", updatedAt = t0)
        val budget = Budget("budget", categoryId = "default.cafe", monthlyLimit = rub(15_000), updatedAt = t0)
        val goal = Goal("goal", name = "Отпуск", target = rub(90_000), targetDate = LocalDate(2027, Month.JUNE, 1), updatedAt = t0)
        val contribution = GoalContribution("in", goalId = "goal", amount = rub(5_000), localDate = today, updatedAt = t0)
        runBlocking {
            planning.save(salary)
            planning.save(paid)
            planning.save(budget)
            planning.save(goal)
            planning.save(contribution)
        }
        assertEquals(listOf(salary), planning.observeRecurring().now())
        assertEquals(listOf(paid), planning.observeFulfillments().now())
        assertEquals(listOf(budget), planning.observeBudgets().now())
        assertEquals(listOf(goal), planning.observeGoals().now())
        assertEquals(listOf(contribution), planning.observeContributions().now())
    }

    @Test
    fun deletedEntitiesAreNotRead() {
        val goal = Goal("goal", name = "Отпуск", target = rub(90_000), updatedAt = t0)
        runBlocking {
            planning.save(goal)
            planning.save(goal.copy(deletedAt = after, updatedAt = after))
        }
        assertEquals(emptyList(), planning.observeGoals().now())
    }
}
