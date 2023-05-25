package com.phazei.dynamicgptchat.data.pojo

data class ChatTreeOptions(
    var streaming: Boolean = false,
    var moderation: Boolean = false,
    var imeSubmit: Boolean = false
)