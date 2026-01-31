package com.app.summa.di

import com.app.summa.util.SystemTimeProvider
import com.app.summa.util.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider {
        return SystemTimeProvider()
    }
}
