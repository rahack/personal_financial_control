package ru.pfc.shared.data.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pfc.shared.data.entities.AccountRow
import ru.pfc.shared.data.entities.BalanceDiscrepancyRow
import ru.pfc.shared.data.entities.BudgetRow
import ru.pfc.shared.data.entities.CardMaskRow
import ru.pfc.shared.data.entities.CategoryRow
import ru.pfc.shared.data.entities.GoalContributionRow
import ru.pfc.shared.data.entities.GoalRow
import ru.pfc.shared.data.entities.MerchantRuleRow
import ru.pfc.shared.data.entities.OperationRow
import ru.pfc.shared.data.entities.RecurringFulfillmentRow
import ru.pfc.shared.data.entities.RecurringRow

// DAO доступны только репозиториям: запись идёт целыми строками, собранными из сущностей модели,
// а запросов, меняющих отдельные поля в обход проверок модели, нет (U3, шаг 6).
// Удаление мягкое: запись с заполненным deletedAt чтение пропускает (KTD7).

@Dao
internal interface AccountDao {
    @Upsert
    suspend fun upsertAccount(row: AccountRow)

    @Query("SELECT * FROM account WHERE id = :id")
    suspend fun getAccount(id: String): AccountRow?

    @Query("SELECT * FROM account WHERE deletedAt IS NULL ORDER BY name")
    fun observeAccounts(): Flow<List<AccountRow>>

    @Upsert
    suspend fun upsertCardMask(row: CardMaskRow)

    @Query("SELECT * FROM card_mask WHERE deletedAt IS NULL")
    fun observeCardMasks(): Flow<List<CardMaskRow>>

    @Upsert
    suspend fun upsertDiscrepancy(row: BalanceDiscrepancyRow)

    @Query("SELECT * FROM balance_discrepancy WHERE deletedAt IS NULL")
    fun observeDiscrepancies(): Flow<List<BalanceDiscrepancyRow>>
}

@Dao
internal interface OperationDao {
    @Upsert
    suspend fun upsert(row: OperationRow)

    /** Даты хранятся как ISO-строки, поэтому сравнение строк совпадает со сравнением дат. */
    @Query("SELECT * FROM operation WHERE deletedAt IS NULL AND localDate BETWEEN :start AND :end ORDER BY occurredAt DESC")
    fun observeByLocalDate(start: String, end: String): Flow<List<OperationRow>>

    @Query(
        "SELECT * FROM operation WHERE deletedAt IS NULL AND type IN ('EXPENSE', 'INCOME') " +
            "AND localDate BETWEEN :start AND :end ORDER BY occurredAt DESC",
    )
    fun observeIncomeAndExpenses(start: String, end: String): Flow<List<OperationRow>>

    @Query("SELECT * FROM operation WHERE deletedAt IS NULL AND (accountId IS NOT NULL OR transferAccountId IS NOT NULL)")
    fun observeAccountOperations(): Flow<List<OperationRow>>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM operation WHERE deletedAt IS NULL " +
            "AND (accountId = :accountId OR transferAccountId = :accountId))",
    )
    suspend fun hasAccountOperations(accountId: String): Boolean
}

@Dao
internal interface CategoryDao {
    @Upsert
    suspend fun upsertCategory(row: CategoryRow)

    @Query("SELECT * FROM category WHERE deletedAt IS NULL ORDER BY name")
    fun observeCategories(): Flow<List<CategoryRow>>

    @Upsert
    suspend fun upsertMerchantRule(row: MerchantRuleRow)

    @Query("SELECT * FROM merchant_rule WHERE deletedAt IS NULL")
    fun observeMerchantRules(): Flow<List<MerchantRuleRow>>
}

@Dao
internal interface PlanningDao {
    @Upsert
    suspend fun upsertRecurring(row: RecurringRow)

    @Query("SELECT * FROM recurring WHERE deletedAt IS NULL")
    fun observeRecurring(): Flow<List<RecurringRow>>

    @Upsert
    suspend fun upsertFulfillment(row: RecurringFulfillmentRow)

    @Query("SELECT * FROM recurring_fulfillment WHERE deletedAt IS NULL")
    fun observeFulfillments(): Flow<List<RecurringFulfillmentRow>>

    @Upsert
    suspend fun upsertBudget(row: BudgetRow)

    @Query("SELECT * FROM budget WHERE deletedAt IS NULL")
    fun observeBudgets(): Flow<List<BudgetRow>>

    @Upsert
    suspend fun upsertGoal(row: GoalRow)

    @Query("SELECT * FROM goal WHERE deletedAt IS NULL")
    fun observeGoals(): Flow<List<GoalRow>>

    @Upsert
    suspend fun upsertContribution(row: GoalContributionRow)

    @Query("SELECT * FROM goal_contribution WHERE deletedAt IS NULL")
    fun observeContributions(): Flow<List<GoalContributionRow>>
}
