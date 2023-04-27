package com.phazei.dynamicgptchat.chatsettings

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.phazei.dynamicgptchat.R
import com.tokenautocomplete.TokenCompleteTextView


class LogitBiasTokenCompleteTextView(context: Context, attrs: AttributeSet?) : TokenCompleteTextView<LogitBiasWrapper>(context, attrs) {

    override fun defaultObject(completionText: String): LogitBiasWrapper {
        val keyValue = completionText.trim().split("=")
        return try {
            LogitBiasWrapper(mapOf(keyValue[0].toInt() to keyValue[1].toInt()))
        } catch (e: Exception) {
            LogitBiasWrapper(emptyMap())
        }
    }
    override fun getViewForObject(token: LogitBiasWrapper): View {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.token_layout, parent as ViewGroup, false)
        val textView = view.findViewById<View>(R.id.token_text) as TextView
        textView.text = token.logitBias.entries.firstOrNull()?.toString() ?: ""
        return view
    }

    override fun shouldIgnoreToken(token: LogitBiasWrapper): Boolean {
        return objects.contains(token) || token.logitBias.isEmpty()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_TAB -> {
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun getMap(): MutableMap<Int, Int> {
        val resultMap = mutableMapOf<Int, Int>()
        for (wrapper in this.objects) {
            resultMap.putAll(wrapper.logitBias)
        }
        return resultMap
    }


}

data class LogitBiasWrapper(val logitBias: Map<Int, Int>)
