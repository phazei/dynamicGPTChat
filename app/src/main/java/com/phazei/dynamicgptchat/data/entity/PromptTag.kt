package com.phazei.dynamicgptchat.data.entity

import androidx.room.*

@Entity(
    tableName = "prompts_tags",
    primaryKeys = ["prompt_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = Prompt::class,
            parentColumns = ["id"],
            childColumns = ["prompt_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["prompt_id"]),
        Index(value = ["tag_id"])
    ]
)
data class PromptTag(
    @ColumnInfo(name = "prompt_id") val promptId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long
)