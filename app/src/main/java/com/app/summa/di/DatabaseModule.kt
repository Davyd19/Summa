package com.app.summa.di

import android.content.Context
import androidx.room.Room
import com.app.summa.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSummaDatabase(
        @ApplicationContext context: Context
    ): SummaDatabase {
        return Room.databaseBuilder(
            context,
            SummaDatabase::class.java,
            "summa_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideHabitDao(database: SummaDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun provideTaskDao(database: SummaDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideAccountDao(database: SummaDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideNoteDao(database: SummaDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun provideIdentityDao(database: SummaDatabase): IdentityDao {
        return database.identityDao()
    }
}