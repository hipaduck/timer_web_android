package com.hipaduck.timerweb.di

import com.hipaduck.timerweb.data.TimerWebRepository
import com.hipaduck.timerweb.data.TimerWebRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Singleton
    @Binds
    abstract fun bindTimerWebRepository(
        timerWebRepositoryImpl: TimerWebRepositoryImpl
    ): TimerWebRepository
}