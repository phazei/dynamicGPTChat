package com.phazei.dynamicgptchat.data.datastore

import javax.inject.Inject

data class AppSettings constructor(
    var openAIkey: String? = null,
    var theme: Theme = Theme.AUTO
)

enum class Theme {
    LIGHT,
    DARK,
    AUTO
}