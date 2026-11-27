package ru.pfc.shared.data.repository

import kotlinx.coroutines.flow.Flow
import ru.pfc.shared.data.AppDatabase
import ru.pfc.shared.data.entities.toModel
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.model.Budget
import ru.pfc.shared.model.Goal
import ru.pfc.shared.model.GoalContribution
import ru.pfc.shared.model.Recurring
import ru.pfc.shared.model.RecurringFulfillment

/** Регулярные платежи и доходы с отметками выполнения, бюджеты, цели и взносы. */
class PlanningRepository(database: AppDatabase, private val logger: SkippedRowLogger = SkippedRowLogger.NONE) {
    private val planningDao = database.planningDao()

    fun observeRecurring(): Flow<List<Recurring>> = planningDao.observeRecurring().readModels("recurring", logger) { it.toModel() }

    suspend fun save(recurring: Recurring) = planningDao.upsertRecurring(recurring.toRow())

    fun observeFulfillments(): Flow<List<RecurringFulfillment>> =
        planningDao.observeFulfillments().readModels("recurring_fulfillment", logger) { it.toModel() }

    suspend fun save(fulfillment: RecurringFulfillment) = planningDao.upsertFulfillment(fulfillment.toRow())

    fun observeBudgets(): Flow<List<Budget>> = planningDao.observeBudgets().readModels("budget", logger) { it.toModel() }

    suspend fun save(budget: Budget) = planningDao.upsertBudget(budget.toRow())

    fun observeGoals(): Flow<List<Goal>> = planningDao.observeGoals().readModels("goal", logger) { it.toModel() }

    suspend fun save(goal: Goal) = planningDao.upsertGoal(goal.toRow())

    fun observeContributions(): Flow<List<GoalContribution>> =
        planningDao.observeContributions().readModels("goal_contribution", logger) { it.toModel() }

    suspend fun save(contribution: GoalContribution) = planningDao.upsertContribution(contribution.toRow())
}
