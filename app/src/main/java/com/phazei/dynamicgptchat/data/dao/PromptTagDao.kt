package com.phazei.dynamicgptchat.data.dao

import androidx.room.*
import com.phazei.dynamicgptchat.data.entity.PromptTag

@Dao
interface PromptTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(promptTags: PromptTag)

    @Delete
    suspend fun delete(promptTags: PromptTag)

    @Query("DELETE FROM prompts_tags WHERE prompt_id = :promptId")
    suspend fun deleteAllTagsForPrompt(promptId: Long)

}