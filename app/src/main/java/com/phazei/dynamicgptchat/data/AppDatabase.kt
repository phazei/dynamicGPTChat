package com.phazei.dynamicgptchat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ChatTree::class, ChatNode::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class, UsageTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatTreeDao(): ChatTreeDao

    abstract fun chatNodeDao(): ChatNodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
