package com.ixeken.worldcupinfo.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ixeken.worldcupinfo.data.database.entities.MatchEntity
import com.ixeken.worldcupinfo.data.database.entities.PredictionEntity

/**
 * Base de datos principal de la aplicación con las tablas matches y predictions.
 */
@Database(
    entities = [MatchEntity::class, PredictionEntity::class],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val matchDao: MatchDao
}
