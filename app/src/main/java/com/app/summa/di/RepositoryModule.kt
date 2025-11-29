package com.app.summa.di

import com.app.summa.data.local.*
import com.app.summa.data.repository.*
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
        identityRepository: IdentityRepository
    ): HabitRepository {
        return HabitRepositoryImpl(habitDao, identityRepository)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        // TaskRepo sekarang butuh IdentityRepo untuk memberi poin
        identityRepository: IdentityRepository
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao, identityRepository)
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
        // KnowledgeRepo sekarang butuh NoteLinkDao untuk Zettelkasten
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
}