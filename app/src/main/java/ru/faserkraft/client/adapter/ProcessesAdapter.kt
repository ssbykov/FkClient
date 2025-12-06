package ru.faserkraft.client.adapter

import android.content.Context
import android.widget.ArrayAdapter

data class ProcessUi(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name
}

class ProcessAdapter(
    context: Context,
    items: MutableList<ProcessUi> = mutableListOf()
) : ArrayAdapter<ProcessUi>(context, android.R.layout.simple_list_item_1, items) {

    fun setItems(newItems: List<ProcessUi>) {
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }
}
