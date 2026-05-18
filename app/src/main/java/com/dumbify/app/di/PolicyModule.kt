package com.dumbify.app.di

import com.dumbify.app.util.Clock
import com.dumbify.app.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PolicyModule {
    @Binds @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
