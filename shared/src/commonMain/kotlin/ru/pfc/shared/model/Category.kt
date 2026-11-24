package ru.pfc.shared.model

import kotlin.time.Instant

enum class CategoryKind { EXPENSE, INCOME }

/** Категория операций; [parentId] задан у подкатегории. [icon] — имя значка Material Symbols (KTD22). */
data class Category(
    override val id: String,
    val name: String,
    val kind: CategoryKind,
    val parentId: String? = null,
    val icon: String? = null,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity
