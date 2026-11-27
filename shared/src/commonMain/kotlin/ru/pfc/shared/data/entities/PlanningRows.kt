package ru.pfc.shared.data.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import ru.pfc.shared.model.Budget
import ru.pfc.shared.model.Goal
import ru.pfc.shared.model.GoalContribution
import ru.pfc.shared.model.Recurring
import ru.pfc.shared.model.RecurringFulfillment
import ru.pfc.shared.model.RecurringKind
import ru.pfc.shared.model.RecurringPeriod
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money

@Entity(tableName = "recurring")
internal data class RecurringRow(
    @PrimaryKey override val id: String,
    val name: String,
    val kind: String,
    val amountMinor: Long,
    val currency: String,
    val period: String,
    val anchorDate: String,
    val accountId: String?,
    val categoryId: String?,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun Recurring.toRow() = RecurringRow(
    id = id,
    name = name,
    kind = kind.name,
    amountMinor = amount.minorUnits,
    currency = amount.currency.code,
    period = period.name,
    anchorDate = anchorDate.toColumn(),
    accountId = accountId,
    categoryId = categoryId,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun RecurringRow.toModel() = Recurring(
    id = id,
    name = name,
    kind = RecurringKind.valueOf(kind),
    amount = Money(amountMinor, Currency(currency)),
    period = RecurringPeriod.valueOf(period),
    anchorDate = anchorDate.toLocalDate(),
    accountId = accountId,
    categoryId = categoryId,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "recurring_fulfillment", indices = [Index("recurringId")])
internal data class RecurringFulfillmentRow(
    @PrimaryKey override val id: String,
    val recurringId: String,
    val dueDate: String,
    val operationId: String?,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun RecurringFulfillment.toRow() = RecurringFulfillmentRow(
    id = id,
    recurringId = recurringId,
    dueDate = dueDate.toColumn(),
    operationId = operationId,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun RecurringFulfillmentRow.toModel() = RecurringFulfillment(
    id = id,
    recurringId = recurringId,
    dueDate = dueDate.toLocalDate(),
    operationId = operationId,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "budget")
internal data class BudgetRow(
    @PrimaryKey override val id: String,
    val categoryId: String,
    val monthlyLimitMinor: Long,
    val currency: String,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun Budget.toRow() = BudgetRow(
    id = id,
    categoryId = categoryId,
    monthlyLimitMinor = monthlyLimit.minorUnits,
    currency = monthlyLimit.currency.code,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun BudgetRow.toModel() = Budget(
    id = id,
    categoryId = categoryId,
    monthlyLimit = Money(monthlyLimitMinor, Currency(currency)),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "goal")
internal data class GoalRow(
    @PrimaryKey override val id: String,
    val name: String,
    val targetMinor: Long,
    val currency: String,
    val targetDate: String?,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun Goal.toRow() = GoalRow(
    id = id,
    name = name,
    targetMinor = target.minorUnits,
    currency = target.currency.code,
    targetDate = targetDate?.toColumn(),
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun GoalRow.toModel() = Goal(
    id = id,
    name = name,
    target = Money(targetMinor, Currency(currency)),
    targetDate = targetDate?.toLocalDate(),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "goal_contribution", indices = [Index("goalId")])
internal data class GoalContributionRow(
    @PrimaryKey override val id: String,
    val goalId: String,
    val amountMinor: Long,
    val currency: String,
    val localDate: String,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun GoalContribution.toRow() = GoalContributionRow(
    id = id,
    goalId = goalId,
    amountMinor = amount.minorUnits,
    currency = amount.currency.code,
    localDate = localDate.toColumn(),
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun GoalContributionRow.toModel() = GoalContribution(
    id = id,
    goalId = goalId,
    amount = Money(amountMinor, Currency(currency)),
    localDate = localDate.toLocalDate(),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
