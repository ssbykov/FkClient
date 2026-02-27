package ru.faserkraft.client.adapter

//noinspection SuspiciousImport
import android.R
import android.content.Context
import android.widget.ArrayAdapter

data class EmployeeUi(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name
}

class EmployeesAdapter(
    context: Context,
    items: MutableList<EmployeeUi> = mutableListOf()
) : ArrayAdapter<EmployeeUi>(context, R.layout.simple_list_item_1, items) {

    fun setItems(newItems: List<EmployeeUi>) {
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }
}
