package ru.pfc.shared.data.repository

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import ru.pfc.shared.data.AppDatabase
import ru.pfc.shared.data.entities.OperationRow
import ru.pfc.shared.data.entities.toInstant
import ru.pfc.shared.data.entities.toModel
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.model.Account
import ru.pfc.shared.model.BalanceDiscrepancy
import ru.pfc.shared.model.CardMask
import ru.pfc.shared.model.Operation
import ru.pfc.shared.money.Money

/** Счета, их балансы, маски карт и расхождения с остатком банка. */
class AccountRepository(private val database: AppDatabase, private val logger: SkippedRowLogger = SkippedRowLogger.NONE) {
    private val accountDao = database.accountDao()
    private val operationDao = database.operationDao()

    fun observeAccounts(): Flow<List<Account>> = accountDao.observeAccounts().readModels("account", logger) { it.toModel() }

    /** Валюту счёта с операциями сменить нельзя: суммы операций хранятся в валюте счёта (KTD7). */
    suspend fun save(account: Account) = database.withWriteTransaction {
        val stored = accountDao.getAccount(account.id)
        require(stored == null || stored.currency == account.currency.code || !operationDao.hasAccountOperations(account.id)) {
            "Нельзя сменить валюту счёта с операциями"
        }
        accountDao.upsertAccount(account.toRow())
    }

    /** Балансы счетов (KTD7) с пометкой «может быть неточным», если её требует U3, шаг 7. */
    fun observeBalances(): Flow<List<AccountBalance>> {
        val operations = operationDao.observeAccountOperations().map { rows -> rows.readEach("operation", logger) { it.toModel() } }
        return combine(observeAccounts(), operations) { accounts, read ->
            accounts.map { account -> accountBalance(account, read) }
        }.distinctUntilChanged()
    }

    /**
     * Баланс одного счёта. Операция с суммой не в валюте счёта считается непрочитанной строкой, а при переполнении суммы
     * показывается остаток последней корректировки: ошибка одного счёта не ломает балансы остальных (U3, шаг 7).
     * Непрочитанная строка помечает баланс, только если она не раньше последней корректировки:
     * более ранние строки на баланс уже не влияют, и «Указать фактический остаток» снимает пометку (R40).
     */
    private fun accountBalance(account: Account, read: ReadRows<OperationRow, Operation>): AccountBalance {
        val (readable, foreign) = read.models
            .filter { it.accountId == account.id || it.transferAccountId == account.id }
            .partition { operation -> listOfNotNull(operation.amount, operation.balanceAfter).all { it.currency == account.currency } }
        foreign.forEach { logger.log("operation", it.id, "Сумма в валюте, отличной от валюты счёта") }
        val anchor = latestAdjustment(account, readable)
        val unreadableAt = read.skipped
            .filter { it.accountId == account.id || it.transferAccountId == account.id }
            .map { it.occurredAt.toInstant() } + foreign.map { it.occurredAt }
        val exact = try {
            balanceOf(account, readable)
        } catch (e: ArithmeticException) {
            logger.log("account", account.id, "Переполнение суммы баланса")
            null
        }
        val warnings = buildSet {
            if (exact == null || unreadableAt.any { anchor == null || it >= anchor.occurredAt }) {
                add(BalanceWarning.UNREADABLE_ROWS)
            }
            if (read.models.any { it.accountId == account.id && it.amount == null && it.originalAmount != null }) {
                add(BalanceWarning.FOREIGN_AMOUNT_MISSING)
            }
        }
        return AccountBalance(account, exact ?: anchor?.balanceAfter ?: Money(0, account.currency), warnings)
    }

    fun observeCardMasks(): Flow<List<CardMask>> = accountDao.observeCardMasks().readModels("card_mask", logger) { it.toModel() }

    suspend fun save(cardMask: CardMask) = accountDao.upsertCardMask(cardMask.toRow())

    fun observeDiscrepancies(): Flow<List<BalanceDiscrepancy>> =
        accountDao.observeDiscrepancies().readModels("balance_discrepancy", logger) { it.toModel() }

    suspend fun save(discrepancy: BalanceDiscrepancy) = accountDao.upsertDiscrepancy(discrepancy.toRow())
}
