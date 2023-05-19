package com.phazei.dynamicgptchat.data.entity

import androidx.room.*

data class PromptWithTags(
    @Embedded val prompt: Prompt,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(PromptTag::class, parentColumn = "prompt_id", entityColumn = "tag_id")
    )
    val tags: MutableList<Tag>
)