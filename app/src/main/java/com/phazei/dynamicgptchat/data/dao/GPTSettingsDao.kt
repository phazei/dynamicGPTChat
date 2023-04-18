package com.phazei.dynamicgptchat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.phazei.dynamicgptchat.data.entity.GPTSettings

@Dao
interface GPTSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gptSettings: GPTSettings): Long

    @Update
    suspend fun update(gptSettings: GPTSettings)

    @Upsert
    suspend fun upsert(gptSettings: GPTSettings)

    @Query("DELETE FROM gpt_settings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM gpt_settings WHERE id = :id")
    suspend fun getById(id: Long): GPTSettings

    @Query("SELECT * FROM gpt_settings")
    suspend fun getAll(): List<GPTSettings>

    // Additional queries that might be used

    @Query("SELECT * FROM gpt_settings WHERE model = :model")
    suspend fun getByModel(model: String): List<GPTSettings>

    @Query("DELETE FROM gpt_settings")
    suspend fun deleteAll()
}
