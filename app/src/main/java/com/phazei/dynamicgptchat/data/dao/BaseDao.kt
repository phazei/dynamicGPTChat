package com.phazei.dynamicgptchat.data.dao

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Update
import com.phazei.dynamicgptchat.data.entity.BaseEntity
import java.util.*

@Dao
interface BaseDao<T : BaseEntity> {
    @Update
    suspend fun update(entity: T)

    @Transaction
    suspend fun updateWithTimestamp(entity: T) {
        entity.updatedAt = Date()
        update(entity)
    }
}