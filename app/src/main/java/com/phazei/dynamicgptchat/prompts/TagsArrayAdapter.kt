package com.phazei.dynamicgptchat.prompts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.phazei.dynamicgptchat.data.entity.Tag

class TagsArrayAdapter(context: Context, resource: Int, private val tags: List<Tag>) :
    ArrayAdapter<Tag>(context, resource, tags) {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private val originalTags: List<Tag> = ArrayList(tags)
    private var filteredTags: List<Tag> = ArrayList(tags)


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: layoutInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
        val tag = getItem(position)
        (view as TextView).text = tag.name
        return view
    }

    override fun getCount(): Int = filteredTags.size

    override fun getItem(position: Int): Tag = filteredTags[position]

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                val suggestions = if (constraint != null) {
                    originalTags.filter { it.name.contains(constraint, true) }
                } else {
                    originalTags
                }
                filterResults.values = suggestions
                filterResults.count = suggestions.size
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                filteredTags = results.values as List<Tag>
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any): String {
                return (resultValue as Tag).name
            }
        }
    }
}
