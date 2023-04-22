package com.phazei.dynamicgptchat.data.datastore

import javax.inject.Inject

data class AppSettings constructor(
    var openAIkey: String? = null
)