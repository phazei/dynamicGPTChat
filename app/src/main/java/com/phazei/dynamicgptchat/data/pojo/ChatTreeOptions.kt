package com.phazei.dynamicgptchat.data.pojo

data class ChatTreeOptions(
    var streaming: Boolean = true,
    var moderation: Boolean = false,
    var imeSubmit: Boolean = false,
    var responseWrap: Wrap = Wrap.WRAP,
    var responseWrapSize: Int = 500
) {
    enum class Wrap {
        WRAP, NOWRAP, CUSTOM
    }
}