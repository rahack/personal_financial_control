package ru.pfc.shared.data.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import ru.pfc.shared.model.Account
import ru.pfc.shared.model.AccountType
import ru.pfc.shared.model.BalanceDiscrepancy
import ru.pfc.shared.model.Bank
import ru.pfc.shared.model.CardMask
import ru.pfc.shared.money.Currency
import ru.pfc.shared.money.Money

@Entity(tableName = "account")
internal data class AccountRow(
    @PrimaryKey override val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val bank: String?,
    val creditLimitMinor: Long?,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun Account.toRow() = AccountRow(
    id = id,
    name = name,
    type = type.name,
    currency = currency.code,
    bank = bank?.name,
    creditLimitMinor = creditLimit?.minorUnits,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun AccountRow.toModel() = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    currency = Currency(currency),
    bank = bank?.let(Bank::valueOf),
    creditLimit = money(creditLimitMinor, currency),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "card_mask", indices = [Index("accountId")])
internal data class CardMaskRow(
    @PrimaryKey override val id: String,
    val accountId: String,
    val lastDigits: String,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun CardMask.toRow() = CardMaskRow(
    id = id,
    accountId = accountId,
    lastDigits = lastDigits,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun CardMaskRow.toModel() = CardMask(
    id = id,
    accountId = accountId,
    lastDigits = lastDigits,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "balance_discrepancy", indices = [Index("accountId")])
internal data class BalanceDiscrepancyRow(
    @PrimaryKey override val id: String,
    val accountId: String,
    val bankBalanceMinor: Long,
    val currency: String,
    val notifiedAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun BalanceDiscrepancy.toRow() = BalanceDiscrepancyRow(
    id = id,
    accountId = accountId,
    bankBalanceMinor = bankBalance.minorUnits,
    currency = bankBalance.currency.code,
    notifiedAt = notifiedAt.toColumn(),
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun BalanceDiscrepancyRow.toModel() = BalanceDiscrepancy(
    id = id,
    accountId = accountId,
    bankBalance = Money(bankBalanceMinor, Currency(currency)),
    notifiedAt = notifiedAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
