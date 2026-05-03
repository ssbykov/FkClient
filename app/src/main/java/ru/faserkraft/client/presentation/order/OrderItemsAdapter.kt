package ru.faserkraft.client.presentation.order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemOrderModuleBinding

data class LocalOrderItem(
    val processId: Int,
    val type: String,
    val quantity: Int
)

class OrderItemsAdapter(
    private val items: List<LocalOrderItem>,
    private val onDeleteClick: (position: Int) -> Unit
) : RecyclerView.Adapter<OrderItemsAdapter.ItemVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemVH {
        val binding = ItemOrderModuleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ItemVH(binding)
    }

    override fun onBindViewHolder(holder: ItemVH, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ItemVH(private val binding: ItemOrderModuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocalOrderItem, position: Int) {
            binding.tvModuleName.text = item.type
            binding.tvModuleQuantity.text = itemView.context.getString(
                R.string.module_quantity_format,
                item.quantity
            )

            // Обработка удаления (корзина)
            binding.btnDelete.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }
}