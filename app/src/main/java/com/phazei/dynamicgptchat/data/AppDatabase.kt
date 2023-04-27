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
import javax.inject.Singleton

@Database(entities = [ChatTree::class, ChatNode::class, GPTSettings::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class, UsageTypeConverter::class, ListTypeConverter::class, MapConverter::class)
@Singleton
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatTreeDao(): ChatTreeDao

    abstract fun chatNodeDao(): ChatNodeDao

    abstract fun gptSettingsDao(): GPTSettingsDao

}
