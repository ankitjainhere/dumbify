package com.dumbify.app.di

import com.dumbify.app.admin.PolicyEnforcer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PolicyModule {
    // PolicyEnforcer is @Inject constructor — no provides needed.
    // Module exists for future bindings.
}
