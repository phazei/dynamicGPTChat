package com.phazei.dynamicgptchat.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.phazei.dynamicgptchat.data.dao.ChatNodeDao
import com.phazei.dynamicgptchat.data.dao.ChatTreeDao
import com.phazei.dynamicgptchat.data.dao.GPTSettingsDao
import com.phazei.dynamicgptchat.data.dao.PromptDao
import com.phazei.dynamicgptchat.data.dao.PromptTagDao
import com.phazei.dynamicgptchat.data.dao.TagDao
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.ChatTree
import com.phazei.dynamicgptchat.data.entity.GPTSettings
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.PromptTag
import com.phazei.dynamicgptchat.data.entity.Tag
import com.phazei.dynamicgptchat.data.migrations.SeedData
import java.util.concurrent.Executors
import javax.inject.Singleton

@Database(
    version = AppDatabase.LATEST_VERSION,
    entities = [ChatTree::class, ChatNode::class, GPTSettings::class, Prompt::class, PromptTag::class, Tag::class],
    exportSchema = true,
    // autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(DateConverter::class, UsageTypeConverter::class, ListTypeConverter::class, MapConverter::class, ChatOptionsConverter::class)
@Singleton
abstract class AppDatabase : RoomDatabase() {

    abstract fun chatTreeDao(): ChatTreeDao

    abstract fun chatNodeDao(): ChatNodeDao

    abstract fun gptSettingsDao(): GPTSettingsDao

    abstract fun promptDao(): PromptDao

    abstract fun promptTagDao(): PromptTagDao

    abstract fun tagDao(): TagDao

    companion object {
        const val LATEST_VERSION = 1

        fun seedDatabaseCallback(context: Context): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Executors.newSingleThreadExecutor().execute {
                        val database = getDatabase(context)
                        val seedData = SeedData(context, database)
                        seedData.seed()
                    }
                }
            }
        }

        /**
         * For future use
         * Manual migration will override auto migration
         *
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE `Fruit` (`id` INTEGER, `name` TEXT, " +
                        "PRIMARY KEY(`id`))")
            }
        }
        // */


        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addCallback(seedDatabaseCallback(context))
                    // .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
