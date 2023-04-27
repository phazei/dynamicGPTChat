package com.phazei.dynamicgptchat.chatsettings

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import com.phazei.dynamicgptchat.R
import com.tokenautocomplete.TokenCompleteTextView


class StopTokenCompleteTextView(context: Context, attrs: AttributeSet?) : TokenCompleteTextView<String>(context, attrs) {

    init {
        // isLongClickable = true
    }

    override fun defaultObject(completionText: String): String {
        return completionText.trim()
    }

    override fun getViewForObject(token: String): View {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.token_layout, parent as ViewGroup, false)
        (view.findViewById<View>(R.id.token_text) as TextView).text = token
        return view
    }

    override fun shouldIgnoreToken(token: String): Boolean {
        return objects.contains(token)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_TAB -> {
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}