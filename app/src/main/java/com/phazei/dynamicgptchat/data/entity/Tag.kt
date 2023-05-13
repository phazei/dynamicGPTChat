package com.phazei.dynamicgptchat.data.entity

import androidx.room.*

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var name: String
) {
    constructor(name: String) : this(0, name)
}