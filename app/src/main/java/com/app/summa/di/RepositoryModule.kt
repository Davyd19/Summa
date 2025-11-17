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
        habitDao: HabitDao
    ): HabitRepository {
        return HabitRepositoryImpl(habitDao)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao)
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
        dao: KnowledgeDao
    ): KnowledgeRepository {
        return KnowledgeRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideIdentityRepository(
        identityDao: IdentityDao
    ): IdentityRepository {
        return IdentityRepositoryImpl(identityDao)
    }
}