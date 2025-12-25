package com.app.summa.di

import com.app.summa.data.local.*
import com.app.summa.data.repository.*
import com.app.summa.util.NotificationScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHabitRepository(
        habitDao: HabitDao,
        identityRepository: IdentityRepository,
        database: SummaDatabase // PERBAIKAN: Tambahkan parameter database
    ): HabitRepository {
        // PERBAIKAN: Pass database ke constructor
        return HabitRepositoryImpl(habitDao, identityRepository, database)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        identityRepository: IdentityRepository,
        notificationScheduler: NotificationScheduler
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao, identityRepository, notificationScheduler)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        accountDao: AccountDao
    ): AccountRepository {
        return AccountRepositoryImpl(accountDao)
    }

    @Provides
    @Singleton
    fun provideKnowledgeRepository(
        dao: KnowledgeDao,
        linkDao: NoteLinkDao
    ): KnowledgeRepository {
        return KnowledgeRepositoryImpl(dao, linkDao)
    }

    @Provides
    @Singleton
    fun provideIdentityRepository(
        identityDao: IdentityDao,
        knowledgeRepository: KnowledgeRepository
    ): IdentityRepository {
        return IdentityRepositoryImpl(identityDao, knowledgeRepository)
    }

    @Provides
    @Singleton
    fun provideFocusRepository(
        dao: FocusSessionDao
    ): FocusRepository {
        return FocusRepositoryImpl(dao)
    }

    // --- TAMBAHAN UNTUK FITUR BACKUP ---
    @Provides
    @Singleton
    fun provideBackupRepository(
        database: SummaDatabase
    ): BackupRepository {
        return BackupRepositoryImpl(database)
    }
}