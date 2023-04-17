package com.phazei.dynamicgptchat.data

import androidx.room.ColumnInfo
import java.util.*

open class BaseEntity {
    @ColumnInfo(name = "updated_at") var updatedAt: Date = Date()
    @ColumnInfo(name = "created_at") var createdAt: Date = Date()
}