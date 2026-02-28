package ru.faserkraft.client.adapter

import android.content.Context
import android.widget.ArrayAdapter

data class StepUi(
    val id: Int,
    val name: String
)

class AddStepsAdapter(
    context: Context
) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1) {

    private val items: MutableList<StepUi> = mutableListOf()

    fun setItems(newItems: List<StepUi>) {
        items.clear()
        items.addAll(newItems)

        clear()
        addAll(items.map { it.name })
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): StepUi? =
        items.getOrNull(position)
}