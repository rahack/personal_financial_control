package ru.pfc.shared.data.repository

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import ru.pfc.shared.data.AppDatabase
import ru.pfc.shared.data.entities.toColumn
import ru.pfc.shared.data.entities.toModel
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.model.Operation
import ru.pfc.shared.time.DayRange

/** Операции. Удаление — сохранение копии с заполненным `deletedAt` (KTD7). */
class OperationRepository(private val database: AppDatabase, private val logger: SkippedRowLogger = SkippedRowLogger.NONE) {
    private val operationDao = database.operationDao()
    private val accountDao = database.accountDao()

    suspend fun save(operation: Operation) = saveAll(listOf(operation))

    /**
     * Записывает операции в одной транзакции: если хоть одна не проходит проверку, не записывается ни одна (U3, шаг 3).
     * Сумма и остаток корректировки должны быть в валюте каждого счёта операции (U3, шаг 6).
     */
    suspend fun saveAll(operations: List<Operation>) = database.withWriteTransaction {
        for (operation in operations) {
            requireAccountCurrency(operation)
            operationDao.upsert(operation.toRow())
        }
    }

    /** Лента за отрезок дней по локальной дате операции (KTD8): операция не переезжает в другой день после смены пояса. */
    fun observeOperations(range: DayRange): Flow<List<Operation>> =
        operationDao.observeByLocalDate(range.start.toColumn(), range.endInclusive.toColumn())
            .readModels("operation", logger) { it.toModel() }

    /** Доходы и расходы для аналитики: переводы и корректировки в неё не входят (R40). */
    fun observeIncomeAndExpenses(range: DayRange): Flow<List<Operation>> =
        operationDao.observeIncomeAndExpenses(range.start.toColumn(), range.endInclusive.toColumn())
            .readModels("operation", logger) { it.toModel() }

    private suspend fun requireAccountCurrency(operation: Operation) {
        for (accountId in listOfNotNull(operation.accountId, operation.transferAccountId)) {
            val account = requireNotNull(accountDao.getAccount(accountId)) { "Счёт операции не найден" }.toModel()
            for (money in listOfNotNull(operation.amount, operation.balanceAfter)) {
                require(money.currency == account.currency) { "Сумма в валюте, отличной от валюты счёта" }
            }
        }
    }
}
