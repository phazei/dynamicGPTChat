package com.phazei.dynamicgptchat.data.dao

import androidx.room.*
import com.phazei.dynamicgptchat.data.entity.ChatTree

@Dao
interface ChatTreeDao : BaseDao<ChatTree> {
    @Query("SELECT * FROM chat_trees ORDER BY updated_at DESC")
    suspend fun getAll(): MutableList<ChatTree>

    @Query("SELECT * FROM chat_trees WHERE id = :id")
    suspend fun getById(id: Long): ChatTree

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(chatTree: ChatTree): Long

    // @Update //this is in BaseDao
    // override suspend fun update(chatTree: ChatTree)

    @Delete
    suspend fun delete(chatTree: ChatTree)
}
