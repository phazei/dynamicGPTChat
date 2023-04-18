package com.phazei.dynamicgptchat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phazei.dynamicgptchat.data.dao.ChatNodeDao
import com.phazei.dynamicgptchat.data.dao.ChatTreeDao
import com.phazei.dynamicgptchat.data.dao.GPTSettingsDao
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.entity.GPTSettings

@Database(entities = [ChatTree::class, ChatNode::class, GPTSettings::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class, UsageTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatTreeDao(): ChatTreeDao

    abstract fun chatNodeDao(): ChatNodeDao

    abstract fun gptSettingsDao(): GPTSettingsDao

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
