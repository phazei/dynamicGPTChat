package com.phazei.taginputview

@Suppress("UNCHECKED_CAST")
open class TagInputData<T> {

    private val tagList = mutableListOf<T>()

    // Converter methods with default implementations
    open fun inputConverter(input: String): T? = input as? T
    open fun displayConverter(tag: T): String = tag.toString()
    open fun customFilter(tag: T?): T? = tag

    fun applyDisplayConverter(tag: Any): String {
        return displayConverter(tag as T)
    }
    fun applyCustomFilter(tag: Any?): T? {
        return if (tag == null) customFilter(tag) else customFilter(tag as T)
    }

    fun setType(type: Any): T {
        return type as T
    }

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
}

open class StringTagInputData : TagInputData<String>() {
    // No need to override anything as the base class already handles String objects
}