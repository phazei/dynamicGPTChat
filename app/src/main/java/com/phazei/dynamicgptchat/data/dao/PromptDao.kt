package com.phazei.dynamicgptchat.data.dao

import androidx.room.*
import com.phazei.dynamicgptchat.data.entity.ChatNode
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.PromptWithTags

@Dao
interface PromptDao : BaseDao<Prompt> {
    @Insert
    suspend fun insert(prompt: Prompt): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Prompt>): List<Long>

    @Delete
    suspend fun delete(prompt: Prompt)

    @Query("SELECT * FROM prompts WHERE id = :promptId")
    suspend fun getPromptById(promptId: Long): Prompt

    @Query("SELECT * FROM prompts")
    suspend fun getAllPrompts(): List<Prompt>

    @Transaction
    @Query("SELECT * FROM prompts ORDER BY updated_at DESC")
    suspend fun getAllPromptsWithTags(): List<PromptWithTags>

    @Transaction
    @Query("SELECT * FROM prompts WHERE id = :promptId")
    suspend fun getPromptWithTag(promptId: Long): List<PromptWithTags>

    @Transaction
    @Query("SELECT * FROM prompts WHERE title LIKE :query OR body LIKE :query")
    suspend fun searchPrompts(query: String): List<PromptWithTags>

    @Transaction
    @Query("SELECT prompts.* FROM prompts INNER JOIN prompts_tags ON prompts.id = prompts_tags.prompt_id WHERE prompts_tags.tag_id IN (:tagIds)")
    suspend fun searchPromptsByTags(tagIds: List<Long>): List<PromptWithTags>

    @Transaction
    @Query("""
    SELECT * FROM prompts 
    WHERE (title LIKE :query OR body LIKE :query)
    AND id IN (
        SELECT prompt_id FROM prompts_tags
        WHERE tag_id IN (:tagIds)
    )
""")
    suspend fun searchPromptsByQueryAndTags(query: String, tagIds: List<Long>): List<PromptWithTags>

}
