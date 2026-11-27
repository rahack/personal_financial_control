package ru.pfc.shared.data.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import ru.pfc.shared.model.Category
import ru.pfc.shared.model.CategoryKind
import ru.pfc.shared.model.MerchantRule

@Entity(tableName = "category")
internal data class CategoryRow(
    @PrimaryKey override val id: String,
    val name: String,
    val kind: String,
    val parentId: String?,
    val icon: String?,
    val isMandatory: Boolean,
    val isHidden: Boolean,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun Category.toRow() = CategoryRow(
    id = id,
    name = name,
    kind = kind.name,
    parentId = parentId,
    icon = icon,
    isMandatory = isMandatory,
    isHidden = isHidden,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun CategoryRow.toModel() = Category(
    id = id,
    name = name,
    kind = CategoryKind.valueOf(kind),
    parentId = parentId,
    icon = icon,
    isMandatory = isMandatory,
    isHidden = isHidden,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)

@Entity(tableName = "merchant_rule", indices = [Index("merchantKey")])
internal data class MerchantRuleRow(
    @PrimaryKey override val id: String,
    val merchantKey: String,
    val categoryId: String,
    val updatedAt: Long,
    val deletedAt: Long?,
) : Row

internal fun MerchantRule.toRow() = MerchantRuleRow(
    id = id,
    merchantKey = merchantKey,
    categoryId = categoryId,
    updatedAt = updatedAt.toColumn(),
    deletedAt = deletedAt?.toColumn(),
)

internal fun MerchantRuleRow.toModel() = MerchantRule(
    id = id,
    merchantKey = merchantKey,
    categoryId = categoryId,
    updatedAt = updatedAt.toInstant(),
    deletedAt = deletedAt?.toInstant(),
)
