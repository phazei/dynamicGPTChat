package com.phazei.dynamicgptchat

import androidx.room.*

@Dao
interface ChatTreeDao : BaseDao<ChatTree> {
    @Query("SELECT * FROM chat_trees")
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
