package ru.pfc.shared.data.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import ru.pfc.shared.model.Bank
import ru.pfc.shared.model.Operation
import ru.pfc.shared.model.OperationSource
import ru.pfc.shared.model.OperationStatus
import ru.pfc.shared.model.OperationType
import ru.pfc.shared.model.PendingReason
import ru.pfc.shared.model.TransferDirection

/** [currency] — валюта счёта: в ней хранятся и сумма, и остаток корректировки (KTD7). */
@Entity(tableName = "operation", indices = [Index("accountId"), Index("transferAccountId"), Index("localDate")])
internal data class OperationRow(
    @PrimaryKey override val id: String,
    val type: String?,
    val amountMinor: Long?,
    val currency: String?,
    val originalAmountMinor: Long?,
    val originalCurrency: String?,
    val accountId: String?,
    val occurredAt: Long,
    val localDate: String,
    val status: String,
    val pendingReason: String?,
    val source: String,
    val categoryId: String?,
    val merchant: String?,
    val comment: String?,
    val bank: String?,
    val transferDirection: String?,
    val transferAccountId: String?,
    val refundOfId: String?,
    val balanceAfterMinor: Long?,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun Operation.toRow() = OperationRow(
    id = id,
    type = type?.name,
    amountMinor = amount?.minorUnits,
    currency = (amount ?: balanceAfter)?.currency?.code,
    originalAmountMinor = originalAmount?.minorUnits,
    originalCurrency = originalAmount?.currency?.code,
    accountId = accountId,
    occurredAt = occurredAt.toColumn(),
    localDate = localDate.toColumn(),
    status = status.name,
    pendingReason = pendingReason?.name,
    source = source.name,
    categoryId = categoryId,
    merchant = merchant,
    comment = comment,
    bank = bank?.name,
    transferDirection = transferDirection?.name,
    transferAccountId = transferAccountId,
    refundOfId = refundOfId,
    balanceAfterMinor = balanceAfter?.minorUnits,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun OperationRow.toModel() = Operation(
    id = id,
    type = type?.let(OperationType::valueOf),
    amount = money(amountMinor, currency),
    originalAmount = money(originalAmountMinor, originalCurrency),
    accountId = accountId,
    occurredAt = occurredAt.toInstant(),
    localDate = localDate.toLocalDate(),
    status = OperationStatus.valueOf(status),
    pendingReason = pendingReason?.let(PendingReason::valueOf),
    source = OperationSource.valueOf(source),
    categoryId = categoryId,
    merchant = merchant,
    comment = comment,
    bank = bank?.let(Bank::valueOf),
    transferDirection = transferDirection?.let(TransferDirection::valueOf),
    transferAccountId = transferAccountId,
    refundOfId = refundOfId,
    balanceAfter = money(balanceAfterMinor, currency),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
