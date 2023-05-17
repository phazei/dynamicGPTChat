package com.phazei.dynamicgptchat.data.repo

import androidx.room.withTransaction
import com.phazei.dynamicgptchat.data.AppDatabase
import com.phazei.dynamicgptchat.data.dao.PromptDao
import com.phazei.dynamicgptchat.data.dao.PromptTagDao
import com.phazei.dynamicgptchat.data.dao.TagDao
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.PromptWithTags
import com.phazei.dynamicgptchat.data.entity.PromptTag
import com.phazei.dynamicgptchat.data.entity.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromptsRepository @Inject constructor(
    private val promptDao: PromptDao,
    private val tagDao: TagDao,
    private val promptTagDao: PromptTagDao,
    private val database: AppDatabase
) {

    suspend fun loadPromptsWithTags(): List<PromptWithTags> {
        return promptDao.getAllPromptsWithTags()
    }

    suspend fun loadTags(): List<Tag> {
        return tagDao.getAllTags()
    }

    suspend fun searchPromptsWithTags(string: String, tags: List<Tag>): List<PromptWithTags> {
        val formattedString = if (string.trim().isNotEmpty()) "%${string.trim()}%" else null
        val idList = tags.map { it.id }

        return if (formattedString != null && idList.isEmpty()) {
            promptDao.searchPrompts(formattedString)
        } else if (formattedString == null && idList.isNotEmpty()) {
            promptDao.searchPromptsByTags(tags.map { it.id })
        } else if (formattedString != null && idList.isNotEmpty()) {
            promptDao.searchPromptsByQueryAndTags(formattedString, tags.map { it.id })
        } else {
            promptDao.getAllPromptsWithTags()
        }
    }

    suspend fun savePromptWithTags(promptWithTags: PromptWithTags) {
        database.withTransaction {
            val prompt = promptWithTags.prompt
            val tags = promptWithTags.tags
            // Upsert prompt
            val promptId = if (prompt.id == 0L) {
                prompt.id = promptDao.insert(prompt)
                prompt.id
            } else {
                promptDao.updateWithTimestamp(prompt)
                prompt.id
            }

            // Upsert tags and insert or update prompt_tag associations
            val newTagIds = promptWithTags.tags.map { tag ->
                val existingTag = tagDao.getTagByName(tag.name)
                if (existingTag != null) {
                    existingTag.id
                } else {
                    tag.id = tagDao.upsert(tag)
                    tag.id
                }
            }

            // Delete old prompt_tag associations
            promptTagDao.deleteAllTagsForPrompt(promptId)

            // Insert new prompt_tag associations
            newTagIds.forEach { tagId ->
                val promptTag = PromptTag(promptId = prompt.id, tagId = tagId)
                promptTagDao.insert(promptTag)
            }
        }
    }

    suspend fun deletePrompt(prompt: Prompt) {
        promptDao.delete(prompt)
    }

}
