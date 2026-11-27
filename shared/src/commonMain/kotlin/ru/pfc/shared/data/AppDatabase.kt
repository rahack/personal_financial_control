package ru.pfc.shared.data

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import ru.pfc.shared.data.dao.AccountDao
import ru.pfc.shared.data.dao.CategoryDao
import ru.pfc.shared.data.dao.OperationDao
import ru.pfc.shared.data.dao.PlanningDao
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
import ru.pfc.shared.data.entities.toRow
import ru.pfc.shared.data.seed.defaultCategories
import ru.pfc.shared.time.AppClock

/** Имя файла базы в личном хранилище приложения; системное шифрование хранилища закрывает R28 (T4). */
const val DATABASE_NAME = "pfc.db"

/**
 * База приложения (T2). Каждое изменение схемы после версии 1 идёт с миграцией и тестом;
 * разрушающая миграция не включается (U3, шаг 5).
 */
@Database(
    entities = [
        AccountRow::class,
        CardMaskRow::class,
        BalanceDiscrepancyRow::class,
        OperationRow::class,
        CategoryRow::class,
        MerchantRuleRow::class,
        RecurringRow::class,
        RecurringFulfillmentRow::class,
        BudgetRow::class,
        GoalRow::class,
        GoalContributionRow::class,
    ],
    version = 1,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun accountDao(): AccountDao

    internal abstract fun operationDao(): OperationDao

    internal abstract fun categoryDao(): CategoryDao

    internal abstract fun planningDao(): PlanningDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/** Собирает базу на встроенном SQLite (KTD4); при создании файла заполняет готовый набор категорий (R13, R14). */
fun RoomDatabase.Builder<AppDatabase>.buildAppDatabase(clock: AppClock): AppDatabase =
    setDriver(BundledSQLiteDriver())
        .addCallback(SeedCallback(clock))
        .build()

private class SeedCallback(private val clock: AppClock) : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        val sql = "INSERT INTO category (id, name, kind, parentId, icon, isMandatory, isHidden, updatedAt, deletedAt) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)"
        connection.prepare(sql).use { statement ->
            for (category in defaultCategories(clock.now())) {
                val row = category.toRow()
                statement.bindText(1, row.id)
                statement.bindText(2, row.name)
                statement.bindText(3, row.kind)
                row.parentId?.let { statement.bindText(4, it) } ?: statement.bindNull(4)
                row.icon?.let { statement.bindText(5, it) } ?: statement.bindNull(5)
                statement.bindBoolean(6, row.isMandatory)
                statement.bindBoolean(7, row.isHidden)
                statement.bindLong(8, row.updatedAt)
                statement.step()
                statement.reset()
            }
        }
    }
}
