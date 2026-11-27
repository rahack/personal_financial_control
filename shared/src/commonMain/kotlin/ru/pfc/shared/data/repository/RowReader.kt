package ru.pfc.shared.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pfc.shared.data.entities.Row

/**
 * Получает таблицу, id и нарушенное правило строки, которую чтение пропустило (U3, шаг 7).
 * Сумм, магазинов и текстов уведомлений в сообщении нет (T6).
 */
fun interface SkippedRowLogger {
    fun log(table: String, id: String, rule: String?)

    companion object {
        val NONE = SkippedRowLogger { _, _, _ -> }
    }
}

internal class ReadRows<R : Row, T>(val models: List<T>, val skipped: List<R>)

/** Переводит строки в сущности по одной: строка, нарушающая правила модели, пропускается и не ломает список. */
internal fun <R : Row, T> List<R>.readEach(table: String, logger: SkippedRowLogger, toModel: (R) -> T): ReadRows<R, T> {
    val models = ArrayList<T>(size)
    val skipped = ArrayList<R>()
    for (row in this) {
        try {
            models += toModel(row)
        } catch (e: IllegalArgumentException) {
            skipped += row
            logger.log(table, row.id, e.message)
        }
    }
    return ReadRows(models, skipped)
}

internal fun <R : Row, T> Flow<List<R>>.readModels(table: String, logger: SkippedRowLogger, toModel: (R) -> T): Flow<List<T>> =
    map { rows -> rows.readEach(table, logger, toModel).models }
