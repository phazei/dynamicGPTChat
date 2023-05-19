package com.phazei.dynamicgptchat.data.migrations

import android.content.Context
import com.phazei.dynamicgptchat.data.AppDatabase
import com.phazei.dynamicgptchat.data.entity.Prompt
import com.phazei.dynamicgptchat.data.entity.Tag
import com.phazei.dynamicgptchat.data.repo.PromptsRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class SeedData(private var context: Context, private var database: AppDatabase) {
    @OptIn(DelicateCoroutinesApi::class)
    fun seed() {
        val tagDao = database.tagDao()
        val promptDao = database.promptDao()
        val promptTagDao = database.promptTagDao()
        val promptsRepo = PromptsRepository(promptDao, tagDao, promptTagDao, database)

        val defaultTags = listOf(Tag(name="Default"))
        val defaultPrompts = listOf(
            Prompt(title="Prompt Creator", body="""
                I want you to become my Prompt Creator. Your goal is to help me craft the best possible prompt for my needs. The prompt will be used by you, ChatGPT. You will follow the following process:
                1) Your first response will be to ask me what the prompt should be about. I will provide my answer, but we will need to improve it through continual iterations by going through the next steps.
                2) Based on my input, you will generate 2 sections. a) Revised prompt (provide your rewritten prompt. It should be clear, concise, and easily understood by you), b) Questions (ask any relevant questions pertaining to what additional information is needed from me to improve the prompt).
                3) We will continue this iterative process with me providing additional information to you and you updating the prompt in the Revised prompt section until I say we are done.
                """.trimIndent()),
            Prompt(title="Prompt Rating", body="""
                I want you to rate every prompt i give you. Give a rating 1 to 10. Add comments on what you think i could have improved about it. Do this for every prompt.
                If you rating of the prompt is an 7 or higher, execute the prompt. If its lower than that don't execute it, but generate me a better prompt.
                """.trimIndent()),
            Prompt(title="Simplified Guide", body="""
                Can you provide me with a long and well-thought-out comprehensive yet simplified guide of [SUBJECT],
                that only includes offline information that you are certain is true and excludes any speculation or uncertainty?
                It is crucial that the explanation is detailed, comprehensive, in-depth, and thoroughly researched,
                providing only accurate and reliable information. Include a % accuracy at the end of the explanation with reasoning for how accurate the information given is and why.
                Give 2 web sources with general urls (accurate as of 2021, ends in .com, .gov, or .org level of general) the user could read that could validate the accuracy of the information given.
                """.trimIndent()),
            Prompt(title="Rubric", body="""
                Create an exceptional rubric about [SUBJECT] that is clear, well-organized, and easy to follow.
                Include specific criteria and descriptors that leave no room for ambiguity.
                Define distinct levels of performance with comprehensive descriptions.
                Provide clear guidance on the assessment process, including scoring instructions and examples.
                """.trimIndent()),
            Prompt(title="Wrong Explanations", body="""
                Please provide a concise, profoundly wrong explanation of [SUBJECT]. Your explanation should demonstrate a
                complete misunderstanding of the topic and include entirely inaccurate information that contradicts the
                well-established understanding of the topic. Be creative and imaginative in constructing your explanation,
                incorporating logical inconsistencies, flawed reasoning, and baseless assertions.
                Feel free to let your imagination run wild and have fun crafting your response!
                """.trimIndent()),
            // Prompt(title="title", body="""
            //
            //     """.trimIndent()),
        )

        runBlocking(Dispatchers.IO) {
            val tagIds = tagDao.insertAll(defaultTags)
            val promptIds = promptDao.insertAll(defaultPrompts)
            //add the default tag
            promptTagDao.addTagToPrompts(tagIds[0], promptIds)

        }

    }
}
