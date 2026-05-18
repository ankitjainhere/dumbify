package com.dumbify.app.di

import android.content.Context
import com.dumbify.app.util.Clock
import com.dumbify.app.util.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PolicyModule {
    @Binds @Singleton
    abstract fun bindClock(impl: SystemClock): Clock

    companion object {
        @Provides @Singleton @Named("ownPackage")
        fun provideOwnPackage(@ApplicationContext context: Context): String = context.packageName
    }
}
