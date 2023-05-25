package com.phazei.dynamicgptchat.data.pojo

import androidx.room.*
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.PromptTag
import com.phazei.dynamicgptchat.data.entity.Tag

data class PromptWithTags(
    @Embedded val prompt: Prompt,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(PromptTag::class, parentColumn = "prompt_id", entityColumn = "tag_id")
    )
    val tags: MutableList<Tag>
)