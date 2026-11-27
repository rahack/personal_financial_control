package ru.pfc.shared.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.pfc.shared.data.repository.AccountRepository
import ru.pfc.shared.data.repository.CategoryRepository
import ru.pfc.shared.data.repository.OperationRepository
import ru.pfc.shared.data.seed.DefaultMerchants
import ru.pfc.shared.data.seed.defaultCategories
import ru.pfc.shared.model.Category
import ru.pfc.shared.model.CategoryKind
import ru.pfc.shared.time.monthRange
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedTest {
    private val database = inMemoryDatabase()
    private val categories = CategoryRepository(database)

    @AfterTest
    fun close() = database.close()

    private fun visible(): List<Category> = runBlocking { categories.observeCategories().first() }

    private fun all(): List<Category> = runBlocking { categories.observeCategories(includeHidden = true).first() }

    @Test
    fun newDatabaseHasDefaultCategoriesWithOneLevelOfSubcategories() {
        val stored = all()
        assertEquals(defaultCategories(testClock.now()).toSet(), stored.toSet())

        val byId = stored.associateBy { it.id }
        val children = stored.filter { it.parentId != null }
        assertTrue(children.isNotEmpty())
        for (child in children) {
            val parent = byId.getValue(child.parentId!!)
            assertEquals(null, parent.parentId, "У подкатегории ${child.name} родитель тоже подкатегория")
            assertEquals(parent.kind, child.kind)
        }
        assertTrue(stored.any { it.isMandatory } && stored.any { !it.isMandatory })
        assertTrue(stored.any { it.kind == CategoryKind.INCOME && it.parentId == null })
        assertTrue(stored.none { it.kind == CategoryKind.INCOME && it.isMandatory })
    }

    @Test
    fun categoryIdsAreUnique() {
        val ids = defaultCategories(testClock.now()).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyDefaultMerchantPointsToExistingCategory() {
        val ids = defaultCategories(testClock.now()).map { it.id }.toSet()
        for (merchant in DefaultMerchants) {
            assertTrue(merchant.categoryId in ids, "Нет категории ${merchant.categoryId} для ${merchant.names}")
        }
        val names = DefaultMerchants.flatMap { it.names }.map { it.lowercase() }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun hiddenCategoryAndItsSubcategoriesAreNotOfferedButStayInHistory() {
        val cafe = all().single { it.id == "default.cafe" }
        runBlocking { categories.save(cafe.copy(isHidden = true, updatedAt = t0)) }

        assertTrue(visible().none { it.id == "default.cafe" || it.parentId == "default.cafe" })
        assertTrue(all().any { it.id == "default.cafe" })

        val accounts = AccountRepository(database)
        val operations = OperationRepository(database)
        runBlocking {
            accounts.save(account("card"))
            operations.save(operation("coffee", categoryId = "default.cafe.coffee"))
        }
        val feed = runBlocking { operations.observeOperations(monthRange(today)).first() }
        assertEquals("default.cafe.coffee", feed.single().categoryId)
    }

    @Test
    fun categoryFlagsCanBeChanged() {
        val groceries = all().single { it.id == "default.groceries" }
        assertTrue(groceries.isMandatory)
        runBlocking { categories.save(groceries.copy(isMandatory = false, updatedAt = t0)) }
        assertEquals(false, all().single { it.id == "default.groceries" }.isMandatory)
    }
}
