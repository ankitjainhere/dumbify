package com.dumbify.app.di

import android.content.Context
import androidx.room.Room
import com.dumbify.app.data.DumbifyDb
import com.dumbify.app.data.EncryptedSecurePrefs
import com.dumbify.app.data.SecurePrefs
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds @Singleton
    abstract fun bindSecurePrefs(impl: EncryptedSecurePrefs): SecurePrefs

    companion object {
        @Provides @Singleton
        fun provideDb(@ApplicationContext ctx: Context): DumbifyDb =
            Room.databaseBuilder(ctx, DumbifyDb::class.java, "dumbify.db")
                .fallbackToDestructiveMigration()
                .build()

        @Provides fun provideConfigDao(db: DumbifyDb): ConfigDao = db.configDao()
        @Provides fun provideAppRuleDao(db: DumbifyDb): AppRuleDao = db.appRuleDao()
        @Provides fun provideEventDao(db: DumbifyDb): EventDao = db.eventDao()
    }
}
