package ru.pfc.shared.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pfc.shared.data.AppDatabase
import ru.pfc.shared.data.entities.toModel
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.model.Category
import ru.pfc.shared.model.MerchantRule

/** Категории и правила пользователя «магазин → категория». */
class CategoryRepository(database: AppDatabase, private val logger: SkippedRowLogger = SkippedRowLogger.NONE) {
    private val categoryDao = database.categoryDao()

    /**
     * Категории для выбора. Скрытая категория и её подкатегории сюда не попадают, если не нужны все,
     * но операции с ними остаются в истории (R13).
     */
    fun observeCategories(includeHidden: Boolean = false): Flow<List<Category>> =
        categoryDao.observeCategories().readModels("category", logger) { it.toModel() }.map { categories ->
            if (includeHidden) {
                categories
            } else {
                val hiddenIds = categories.filter { it.isHidden }.map { it.id }.toSet()
                categories.filterNot { it.isHidden || it.parentId in hiddenIds }
            }
        }

    suspend fun save(category: Category) = categoryDao.upsertCategory(category.toRow())

    fun observeMerchantRules(): Flow<List<MerchantRule>> =
        categoryDao.observeMerchantRules().readModels("merchant_rule", logger) { it.toModel() }

    suspend fun save(rule: MerchantRule) = categoryDao.upsertMerchantRule(rule.toRow())
}
