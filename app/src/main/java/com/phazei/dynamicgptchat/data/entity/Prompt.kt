package com.phazei.dynamicgptchat.data.entity

import androidx.room.*

@Entity(tableName = "prompts")
data class Prompt(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    var body: String,
) : BaseEntity() {
    constructor() : this(0, "", "")
}
