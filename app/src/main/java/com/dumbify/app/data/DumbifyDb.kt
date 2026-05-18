package com.dumbify.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.dumbify.app.data.dao.AppRuleDao
import com.dumbify.app.data.dao.ConfigDao
import com.dumbify.app.data.dao.EventDao
import com.dumbify.app.data.entities.AppRule
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.Config
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.dumbify.app.data.entities.UserRole

class EnumConverters {
    @TypeConverter fun blockModeToString(v: BlockMode): String = v.name
    @TypeConverter fun stringToBlockMode(v: String): BlockMode = BlockMode.valueOf(v)

    @TypeConverter fun userRoleToString(v: UserRole): String = v.name
    @TypeConverter fun stringToUserRole(v: String): UserRole = UserRole.valueOf(v)

    @TypeConverter fun bypassModeToString(v: BypassMode): String = v.name
    @TypeConverter fun stringToBypassMode(v: String): BypassMode = BypassMode.valueOf(v)

    @TypeConverter fun eventTypeToString(v: EventType): String = v.name
    @TypeConverter fun stringToEventType(v: String): EventType = EventType.valueOf(v)
}

@Database(
    entities = [Config::class, AppRule::class, Event::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class DumbifyDb : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun eventDao(): EventDao
}
