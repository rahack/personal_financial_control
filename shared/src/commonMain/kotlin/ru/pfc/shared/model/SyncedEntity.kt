package ru.pfc.shared.model

import kotlin.time.Instant

/**
 * Поля, которые есть у каждой записи ради будущей синхронизации (T3):
 * UUID, время последнего изменения и пометка удаления вместо физического удаления.
 */
interface SyncedEntity {
    val id: String
    val updatedAt: Instant
    val deletedAt: Instant?
}
