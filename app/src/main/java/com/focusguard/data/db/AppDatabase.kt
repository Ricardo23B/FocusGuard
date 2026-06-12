package com.focusguard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focusguard.data.dao.*
import com.focusguard.data.models.*

@Database(
    entities = [Session::class, AppRule::class, VipContact::class, DailyStats::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun vipContactDao(): VipContactDao
    abstract fun dailyStatsDao(): DailyStatsDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "focusguard.db")
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
