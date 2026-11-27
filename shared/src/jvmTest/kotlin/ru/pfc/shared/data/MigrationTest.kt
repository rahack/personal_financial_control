package ru.pfc.shared.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ru.pfc.shared.data.repository.AccountRepository
import ru.pfc.shared.data.repository.CategoryRepository
import ru.pfc.shared.data.repository.OperationRepository
import ru.pfc.shared.data.seed.defaultCategories
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Версия схемы пока одна, поэтому тест открывает существующий файл версии 1 и проверяет данные.
 * Миграции к следующим версиям добавляются сюда же вместе с изменением схемы (U3, шаг 5).
 */
class MigrationTest {
    private val directory = createTempDirectory("pfc-db").toFile()
    private val file = File(directory, DATABASE_NAME)

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun reopenedVersionOneDatabaseKeepsDataAndDoesNotReseed() {
        val database = databaseBuilder(file).buildAppDatabase(testClock)
        runBlocking {
            AccountRepository(database).save(account("card"))
            OperationRepository(database).save(adjustment("start", rub(30_000)))
        }
        database.close()

        val reopened = databaseBuilder(file).buildAppDatabase(testClock)
        try {
            val balances = runBlocking { AccountRepository(reopened).observeBalances().first() }
            assertEquals(rub(30_000), balances.single().balance)
            val categories = runBlocking { CategoryRepository(reopened).observeCategories(includeHidden = true).first() }
            assertEquals(defaultCategories(testClock.now()).size, categories.size)
        } finally {
            reopened.close()
        }
    }

    @Test
    fun versionOneSchemaIsExported() {
        val schema = File("schemas/ru.pfc.shared.data.AppDatabase/1.json")
        assertTrue(schema.isFile, "Нет экспортированной схемы ${schema.absolutePath}")
        assertTrue(schema.readText().contains("\"version\": 1"))
    }
}
