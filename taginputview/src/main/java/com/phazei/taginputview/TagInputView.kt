package com.phazei.taginputview

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.LayerDrawable
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.Xml
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Suppress("UNCHECKED_CAST")
class TagInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FlexboxLayout(context, attrs, defStyleAttr) {

    private val defaultTagInputData = TagInputData<String>()
    private var tagInputData: TagInputData<*> = defaultTagInputData

    private var delimiterChars = ","
    private val delimiterKeys = mutableListOf(KeyEvent.KEYCODE_ENTER)

    private val defaultTagStyle = R.style.TagInputTagStyle
    private var customTagStyle: Int? = null
    private var inputTheme: Int? = null
    private var tagLimit: Int? = null
    private var maxTextLength: Int? = null


    var onTagChangeListener: OnTagChangeListener<Any>? = null

    lateinit var tagInputEditText: AutoCompleteTextView

    private var selectedChip: Chip? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val viewHelper: ViewHelper

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TagInputView)
        //needs to be initiated before edit text
        inputTheme = typedArray.getResourceId(R.styleable.TagInputView_inputTheme, -1).takeIf { it != -1 }

        setupSelfView()
        setupEditText()
        viewHelper = ViewHelper()


        val hint = typedArray.getString(R.styleable.TagInputView_android_hint).takeIf { it != "" }
        tagInputEditText.hint = hint ?: context.getString(R.string.enter_tags)

        maxTextLength = typedArray.getInt(R.styleable.TagInputView_android_maxLength, -1).takeIf { it != -1 }
        if (maxTextLength != null) tagInputEditText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxTextLength!!))

        val isEnabled = typedArray.getBoolean(R.styleable.TagInputView_android_enabled, true)
        setEnabled(isEnabled)


        tagLimit = typedArray.getInt(R.styleable.TagInputView_tagLimit, -1).takeIf { it != -1 }
        customTagStyle = typedArray.getResourceId(R.styleable.TagInputView_tagStyle, -1).takeIf { it != -1 }


        viewHelper.setupBackground(typedArray)

        typedArray.recycle()


        // addTag(inputConverter("a")!!)

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        //hack since minHeight is broken: https://github.com/google/flexbox-layout/issues/562
        val minHeight = 56.dpToPx()
        //once the heights been adjusted, it will cycle back and forth, need 1 px leeway to stop
        if (measuredHeight < minHeight + 1) {
            val extraPadding = minHeight - measuredHeight
            if (extraPadding > 0) {
                setPadding(viewHelper.paddingMod[0], viewHelper.paddingMod[1] + extraPadding, viewHelper.paddingMod[2], viewHelper.paddingMod[3])
            }
            //not needed most of the time, but discovered one case in a Dialog where the height didn't change with padding alone
            setMeasuredDimension(measuredWidth, minHeight)
        } else {
            setPadding(viewHelper.paddingMod[0], viewHelper.paddingMod[1], viewHelper.paddingMod[2], viewHelper.paddingMod[3])
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        // Control the state of your custom view's components

        setChipsEnabled(enabled)
        if (enabled) {
            tagInputEditText.visibility = View.VISIBLE
        } else {
            tagInputEditText.visibility = View.GONE
        }
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        // Always request focus for the EditText instead of the TagInputView itself
        return tagInputEditText.requestFocus(direction, previouslyFocusedRect)
    }

    private fun setupSelfView() {
        this.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {}
        this.flexWrap = FlexWrap.WRAP
        this.alignItems = AlignItems.FLEX_END
        // this.focusable = NOT_FOCUSABLE
        descendantFocusability = FOCUS_BEFORE_DESCENDANTS

    }

    private fun setupEditText() {
        var contextWrapper = context
        if (inputTheme != null) {
            contextWrapper = ContextThemeWrapper(context, inputTheme!!)
        }

        tagInputEditText = AutoCompleteTextView(contextWrapper, null, android.R.attr.autoCompleteTextViewStyle, com.google.android.material.R.style.ThemeOverlay_Material3_AutoCompleteTextView_FilledBox).apply {
            // tagInputEditText = TextInputEditText(context, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_FilledBox).apply {
            id = View.generateViewId()
            layoutParams = LayoutParams(
                100,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                alignSelf = AlignItems.CENTER
                flexGrow = 1f
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            }
            gravity = Gravity.CENTER_VERTICAL
            hint = context.getString(R.string.enter_tags)
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
            maxLines = 1
            textSize = 16F
            background = null
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            isFocusedByDefault = true
        }


        addView(tagInputEditText)

        tagInputEditText.setOnFocusChangeListener { _, hasFocus ->
            viewHelper.underlineDrawable.animateFocus(hasFocus)
            uncheckAllChips()
            selectedChip = null
        }

        tagInputEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode in delimiterKeys) {
                addTagFromEditText()
                true
            } else {
                false
            }
        }

        var showLimitMessage = true
        tagInputEditText.addTextChangedListener { text ->
            if (!text.isNullOrEmpty()) {
                if (tagLimit != null && tagInputData.size() + 1 > tagLimit!!) {
                    tagInputEditText.setText("")
                    if (showLimitMessage) {
                        showLimitMessage = false
                        coroutineScope.launch {
                            Snackbar.make(this@TagInputView, "Max tokens ($tagLimit) reached", Snackbar.LENGTH_LONG).show()
                            delay(3000)
                            showLimitMessage = true
                        }
                    }
                } else {
                    val lastChar = text.last()
                    if (delimiterChars.contains(lastChar)) {
                        addTagFromEditText()
                    }
                }
            }
        }

        tagInputEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val position = tagInputEditText.getListSelection()
                if (keyCode == KeyEvent.KEYCODE_ENTER && tagInputEditText.isPopupShowing && position != ListView.INVALID_POSITION) {
                    //if autoComplete popup is showing and an item is selected, then it shouldn't add
                } else if (delimiterKeys.contains(keyCode)) {
                    addTagFromEditText()
                    return@setOnKeyListener true
                }
                //allow backspace to delete tags
                if (tagInputEditText.selectionStart == 0 && keyCode == KeyEvent.KEYCODE_DEL) {
                    //select last chip
                    val lastChipChildIndex = childCount - 2
                    if (lastChipChildIndex >= 0) {
                        val chip = getChildAt(lastChipChildIndex)
                        chip.performClick()
                    }
                    return@setOnKeyListener true
                  }
            }
            return@setOnKeyListener false
        }

        /**
         * Array
         */
        tagInputEditText.setOnItemClickListener { _, _, position, _ ->
            val item = tagInputEditText.adapter.getItem(position)
            var convertedItem = item
            if (item is String) {
                convertedItem = tagInputData.inputConverter(item)
            }
            addTag(convertedItem)
            tagInputEditText.setText("")
        }

    }

    private fun addTagFromEditText() {
        var input = tagInputEditText.text.toString()
        if (input.isNotEmpty() && delimiterChars.contains(input.last())) {
            input = input.dropLast(1)
        }

        if (input.isNotEmpty()) {
            val convertedTag = tagInputData.inputConverter(input)
            if (convertedTag != null) {
                addTag(convertedTag)
            }
        }
        tagInputEditText.setText("")
    }

    fun addTag(tag: Any) {
        if (!tagInputData.containsTag(tag)) {
            if (tagLimit != null && tagInputData.size() + 1 > tagLimit!!) {
                return
            }
            val processedTag = tagInputData.applyCustomFilter(tag)
            if (processedTag != null) {
                tagInputData.addTag(tag)
                addTagView(tag)
                onTagChangeListener?.onTagChange(tag, null)
            }
        }
    }

    /**
     * Lots of extra work to ensure styles are retrieved from R.styles
     * Note: chip won't show preview unless it's a child of a Material theme hence chipContext
     *
     */
    private fun addTagView(tag: Any) {

        // // in case issues with creating from scratch
        // val chipInflater = LayoutInflater.from(chipContext)
        // val chip = chipInflater.inflate(R.layout.tag_input_tag, this, false) as Chip

        val chipDrawableStyle = ChipDrawable.createFromAttributes(viewHelper.chipContextCustom, viewHelper.attrsCustom, 0, viewHelper.chipTagStyle)
        chipDrawableStyle.apply {
            isCloseIconVisible = false
            isContextClickable = false
            isCheckedIconVisible = false
            isCheckable = true
            isClickable = true
        }
        val chip = Chip(viewHelper.chipContextCustom, viewHelper.attrsCustom, viewHelper.chipTagStyle)

        // com.google.android.flexbox.R.styleable.FlexboxLayout_Layout_layout_flexGrow
        chip.apply {
            // could be programmatically added while disabled so need to check here
            isEnabled = this@TagInputView.isEnabled

            text = tagInputData.applyDisplayConverter(tag)
            setChipDrawable(chipDrawableStyle)
            layoutParams = LayoutParams(viewHelper.chipContextCustom, viewHelper.attrsCustom)
            // setEnsureMinTouchTargetSize(false)
            ensureAccessibleTouchTarget(viewHelper.getChipMinTouchTargetSizeFromStyle(viewHelper.chipTagStyle))

            chip.setOnClickListener {
                if (!this@TagInputView.isEnabled) {
                    return@setOnClickListener
                }
                // If the clicked chip is the same as the selected chip, remove it from the view and reset the selected chip
                if (selectedChip == chip) {
                    removeTag(tag)
                    selectedChip = null
                } else {
                    // Uncheck all chips to ensure only one is selected at a time
                    uncheckAllChips()

                    // Check the clicked chip and set it as the selected chip
                    chip.isChecked = true
                    selectedChip = chip
                }
            }
            // focusable = NOT_FOCUSABLE
            chip.onFocusChangeListener = viewHelper.chipFocusListener
        }

        addView(chip, childCount - 1)

    }

    fun uncheckAllChips() {
        for (i in 0 until this.childCount) {
            val child = this.getChildAt(i)
            if (child is Chip) {
                child.isChecked = false
            }
        }
    }
    fun setChipsEnabled(enabled: Boolean) {
        for (i in 0 until this.childCount) {
            val child = this.getChildAt(i)
            if (child is Chip) {
                child.isEnabled = enabled
            }
        }
    }

    fun removeTag(tag: Any) {
        val index = tagInputData.indexOf(tag)
        if (index != -1) {
            tagInputData.removeAt(index)
            removeViewAt(index)
            onTagChangeListener?.onTagChange(null, tag)
        }
    }

    fun setTagLimit(limit: Int? = null) {
        tagLimit = limit?:0
    }
    fun setDelimiterKeys(keys: List<Int>) {
        delimiterKeys.clear()
        delimiterKeys.addAll(keys)
    }
    fun setDelimiterChars(chars: String) {
        delimiterChars = chars
    }

    fun <T> setTagInputData(tagInputData: TagInputData<T>) {
        this.tagInputData = tagInputData
    }
    fun containsTag(tag: Any): Boolean {
        return tagInputData.containsTag(tag)
    }
    fun getTags(): MutableList<Any> {
        return tagInputData.getTags().toMutableList() as MutableList<Any>
    }
    inline fun <reified T> getTagsOfType(): MutableList<T> {
        return getTags().map { it as T }.toMutableList()
    }

    fun clearTags() {
        tagInputData.clearTags()
        for (i in this.childCount - 1 downTo 0) {
                val child = this.getChildAt(i)
            if (child is Chip) {
                removeView(child)
            }
        }
        onTagChangeListener?.onTagChange(null, null)
    }

    /**
     * ArrayAdapter for autocomplete.  Optional.
     * Should be same type as TagInputData.
     */
    fun <T> setAutoCompleteAdapter(adapter: ArrayAdapter<T>?) {
        tagInputEditText.setAdapter(adapter)
    }

    fun setCustomFilter(filter: (Any?) -> Any?) {
        tagInputData.assignCustomFilter(filter)
    }

    fun setInputConverter(converter: (String) -> Any?) {
        tagInputData.assignInputConverter(converter)
    }

    fun setDisplayConverter(converter: (Any) -> String) {
        tagInputData.assignDisplayConverter(converter)
    }

    fun updateTagList(tagList: MutableList<Any>) {
        tagInputData.updateTagList(tagList)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.cancel() // Cancel all coroutines when the view is detached
    }

    fun setOnTagChangeListener(listener: (addedTag: Any?, removedTag: Any?) -> Unit) {
        this.onTagChangeListener = object : OnTagChangeListener<Any> {
            override fun onTagChange(addedTag: Any?, removedTag: Any?) {
                listener(addedTag, removedTag)
            }
        }
    }
    interface OnTagChangeListener<T> {
        fun onTagChange(addedTag: T?, removedTag: T?)
    }

    inner class ViewHelper() {
        val paddingMod = listOf(paddingLeft, paddingTop, paddingRight, paddingBottom)
        val chipContextCustom: ContextThemeWrapper
        val attrsCustom: AttributeSet
        val chipTagStyle = customTagStyle ?: defaultTagStyle
        val chipFocusListener: OnFocusChangeListener
        lateinit var underlineDrawable: TagInputUnderlineDrawable

        init {
            val chipContext = ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents_Light)
            chipContextCustom = ContextThemeWrapper(chipContext, chipTagStyle)

            val parser = chipContextCustom.resources.getXml(R.xml.tag_input_tag_style)
            attrsCustom = Xml.asAttributeSet(parser)

            chipFocusListener =  View.OnFocusChangeListener { view, hasFocus ->
                viewHelper.underlineDrawable.animateFocus(hasFocus)
            }

        }

        fun getChipMinTouchTargetSizeFromStyle(@StyleRes styleResId: Int): Int {
            val attributes = intArrayOf(com.google.android.material.R.attr.chipMinTouchTargetSize)
            val typedArray = context.obtainStyledAttributes(styleResId, attributes)
            val minTouchTargetSize = typedArray.getDimensionPixelSize(0, 0)
            typedArray.recycle()

            return minTouchTargetSize
        }

        fun setupBackground(typedArray: TypedArray) {
            val backgroundColor = typedArray.getColor(R.styleable.TagInputView_android_background, ContextCompat.getColor(context, R.color.tag_input_background))
            val strokeColor = typedArray.getColor(R.styleable.TagInputView_strokeColor, ContextCompat.getColor(context, R.color.default_stroke_color))

            underlineDrawable = TagInputUnderlineDrawable(context, backgroundColor, strokeColor)
            val shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, 4.dpToPx().toFloat())
                .setTopRightCorner(CornerFamily.ROUNDED, 4.dpToPx().toFloat())
                .build()

            val appearanceDrawable = MaterialShapeDrawable(shapeAppearanceModel).apply {
                fillColor = ColorStateList.valueOf(Color.RED)
                setTint(backgroundColor)
                strokeWidth = 0f
            }
            this@TagInputView.background = LayerDrawable(arrayOf(appearanceDrawable, underlineDrawable))

        }
    }

    private fun Int.dpToPx(): Int {
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        return (this * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
    }
}
