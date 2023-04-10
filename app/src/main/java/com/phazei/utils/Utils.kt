package com.phazei.utils

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.slider.Slider


object Utils {
    fun showToast(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}

fun View.setChangeListener(listener: () -> Unit) {
    when (this) {
        is EditText -> {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { listener() }
                override fun afterTextChanged(s: Editable) { listener() }
            })
        }
        is Spinner -> {
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, v: View?, position: Int, id: Long) { listener() }
                override fun onNothingSelected(parent: AdapterView<*>) { listener() }
            }
        }
        is Slider -> {
            addOnChangeListener { _, _, _ -> listener() }
        }
        is ViewGroup -> {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                child.setChangeListener(listener)
            }
        }
    }
}