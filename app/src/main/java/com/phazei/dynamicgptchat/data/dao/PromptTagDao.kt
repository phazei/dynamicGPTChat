package com.phazei.dynamicgptchat.data.dao

import androidx.room.*
import com.phazei.dynamicgptchat.data.entity.PromptTag

@Dao
interface PromptTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(promptTags: PromptTag)

    @Transaction
    suspend fun addTagToPrompts(tagId: Long, promptIds: List<Long>) {
        for (promptId in promptIds) {
            insert(PromptTag(promptId, tagId))
        }
    }

    @Delete
    suspend fun delete(promptTags: PromptTag)

    @Query("DELETE FROM prompts_tags WHERE prompt_id = :promptId")
    suspend fun deleteAllTagsForPrompt(promptId: Long)

}