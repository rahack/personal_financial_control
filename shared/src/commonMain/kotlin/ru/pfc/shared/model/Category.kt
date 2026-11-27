package ru.pfc.shared.model

import kotlin.time.Instant

enum class CategoryKind { EXPENSE, INCOME }

/**
 * Категория операций; [parentId] задан у подкатегории. [icon] — имя значка Material Symbols (KTD22).
 * [isMandatory] — обязательная трата (R14). Скрытая категория ([isHidden]) не предлагается при вводе,
 * но остаётся в истории операций (R13).
 */
data class Category(
    override val id: String,
    val name: String,
    val kind: CategoryKind,
    val parentId: String? = null,
    val icon: String? = null,
    val isMandatory: Boolean = false,
    val isHidden: Boolean = false,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity

/** Правило пользователя «магазин → категория» (KTD10, источник USER); оно важнее готового словаря магазинов. */
data class MerchantRule(
    override val id: String,
    val merchantKey: String,
    val categoryId: String,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require(merchantKey.isNotBlank()) { "Пустой ключ магазина" }
    }
}
