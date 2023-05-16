package com.phazei.taginputview

@Suppress("UNCHECKED_CAST")
class TagInputData<T> {

    private val tagList = mutableListOf<T>()

    private var customFilter: ((T?) -> T?)? = null
    var inputConverter: (String) -> T? = { it as? T }
    private var displayConverter: (T) -> String = { it.toString() }

    fun addTag(tag: Any) {
        tagList.add(tag as T)
    }

    fun removeTag(tag: Any) {
        tagList.remove(tag as T)
    }

    fun removeAt(index: Int) {
        tagList.removeAt(index)
    }

    fun getTags(): MutableList<T> {
        return tagList.toMutableList()
    }

    fun updateTagList(tagList: MutableList<Any>) {
        this.tagList.clear()
        tagList.forEach { addTag(it) }
    }

    fun containsTag(tag: Any): Boolean {
        return tagList.contains(tag as T)
    }

    fun size(): Int {
        return tagList.size
    }

    fun indexOf(tag: Any): Int {
        return tagList.indexOf(tag as T)
    }

    fun clearTags() {
        tagList.clear()
    }

    fun applyCustomFilter(input: Any?): Any? {
        return if (customFilter != null) {
            customFilter?.invoke(input as T)
        } else {
            input as T
        }
    }

    fun applyDisplayConverter(input: Any?): String {
        return displayConverter.invoke(input as T)
    }

    fun assignCustomFilter(filter: (Any?) -> Any?) {
        customFilter = filter as? (T?) -> T?
    }

    fun assignInputConverter(converter: (String) -> Any?) {
        inputConverter = converter as (String) -> T?
    }

    fun assignDisplayConverter(converter: (Any) -> String) {
        displayConverter = converter as (T) -> String
    }
}

