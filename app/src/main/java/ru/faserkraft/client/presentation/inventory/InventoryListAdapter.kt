package ru.faserkraft.client.presentation.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryBinding
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.utils.converter.formatIsoToUi

data class InventoryListItem(
    val inventory: Inventory,
    val itemCount: Int,
)

class InventoryListAdapter(
    private val onItemClick: (Inventory) -> Unit,
    private val onDeleteClick: (Inventory) -> Unit,
) : ListAdapter<InventoryListItem, InventoryListAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryListItem) = with(binding) {
            val context = itemView.context
            val inventory = item.inventory

            tvInventoryName.text = context.getString(
                R.string.inventory_name_format,
                inventory.id
            )
            tvCreatedAt.text = formatIsoToUi(inventory.createdAt)
            tvScannedCount.text = context.getString(
                R.string.inventory_scanned_count,
                item.itemCount
            )

            root.setOnClickListener { onItemClick(inventory) }
//            btnDelete.setOnClickListener { onDeleteClick(inventory) }
        }
    }

    class Diff : DiffUtil.ItemCallback<InventoryListItem>() {
        override fun areItemsTheSame(
            oldItem: InventoryListItem,
            newItem: InventoryListItem,
        ) = oldItem.inventory.id == newItem.inventory.id

        override fun areContentsTheSame(
            oldItem: InventoryListItem,
            newItem: InventoryListItem,
        ) = oldItem == newItem
    }
}