package ru.pfc.shared.data

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

/** Построитель базы в файле [file]; на компьютере база нужна тестам общего модуля. */
fun databaseBuilder(file: File): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder(name = file.absolutePath, factory = { AppDatabaseConstructor.initialize() })
