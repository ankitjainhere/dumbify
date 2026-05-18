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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        @Provides @Singleton @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
