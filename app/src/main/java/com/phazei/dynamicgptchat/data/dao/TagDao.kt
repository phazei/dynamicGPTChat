package com.phazei.dynamicgptchat.data.dao

import androidx.room.*
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.Tag

@Dao
interface TagDao {
    @Upsert
    suspend fun upsert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Tag>): List<Long>

    @Delete
    suspend fun delete(tag: Tag)

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<Tag>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag

    @Query("SELECT * FROM tags WHERE name = :tagName")
    suspend fun getTagByName(tagName: String): Tag?

    // @Query("SELECT * FROM tags")
    // suspend fun getAllTags(): List<Tag>

    // @Query("SELECT tags.* FROM tags INNER JOIN prompts_tags ON tags.id = prompts_tags.tag_id WHERE prompts_tags.prompt_id = :promptId")
    // suspend fun getTagsForPrompt(promptId: Long): List<Tag>

    // @Query("SELECT * FROM tags WHERE name LIKE :query")
    // suspend fun searchTags(query: String): List<Tag>
}
