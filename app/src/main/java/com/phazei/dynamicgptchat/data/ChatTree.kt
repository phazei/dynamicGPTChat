package com.phazei.dynamicgptchat.data

import androidx.room.*

@Entity(
    tableName = "chat_trees",
    foreignKeys = [
        ForeignKey(
            entity = ChatNode::class,
            parentColumns = ["id"],
            childColumns = ["root_chat_node_id"],
            onDelete = ForeignKey.CASCADE
        ),        ForeignKey(
            entity = GPTSettings::class,
            parentColumns = ["id"],
            childColumns = ["gpt_settings_id"],
            onDelete = ForeignKey.CASCADE
        )

    ],
    indices = [
        Index(value = ["root_chat_node_id"]),
        Index(value = ["gpt_settings_id"])
    ]
)
data class ChatTree(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var title: String,
    //TODO: make settings its own table and load when retrieving tree
    @ColumnInfo(name = "gpt_settings_id") var gptSettingsId: Long? = null,
    @ColumnInfo(name = "root_chat_node_id") var rootChatNodeId: Long? = null,
) : BaseEntity() {
    var tempPrompt: String = ""

    @Ignore var gptSettings: GPTSettings = GPTSettings()
    @Ignore var rootNode: ChatNode = ChatNode()

    constructor(title: String) : this(
        id = 0,
        title = title
    )
}
