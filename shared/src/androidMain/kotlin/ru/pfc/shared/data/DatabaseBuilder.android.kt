package ru.pfc.shared.data

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/** Построитель базы в стандартной папке баз приложения: эту папку возьмёт системный бэкап (KTD17). */
fun databaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
        factory = { AppDatabaseConstructor.initialize() },
    )
}
