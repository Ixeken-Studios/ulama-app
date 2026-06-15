package com.ixeken.worldcupinfo.di

import android.content.Context
import androidx.room.Room
import com.ixeken.worldcupinfo.data.database.AppDatabase
import com.ixeken.worldcupinfo.data.database.MatchDao
import com.ixeken.worldcupinfo.data.repository.MatchRepositoryImpl
import com.ixeken.worldcupinfo.domain.repository.MatchRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias de Dagger Hilt para la base de datos y repositorios.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindMatchRepository(
        matchRepositoryImpl: MatchRepositoryImpl
    ): MatchRepository

    companion object {

        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "worldcup_database"
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideMatchDao(database: AppDatabase): MatchDao {
            return database.matchDao
        }
    }
}
