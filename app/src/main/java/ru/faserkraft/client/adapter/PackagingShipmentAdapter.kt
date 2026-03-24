package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemPackagingShipmentBinding

class PackagingShipmentAdapter(
    private val onItemCheckedChange: (item: PackagingShipmentUiItem, isChecked: Boolean) -> Unit
) : ListAdapter<PackagingShipmentUiItem, PackagingShipmentAdapter.PackagingVH>(PackagingDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackagingVH {
        val binding = ItemPackagingShipmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackagingVH(binding, onItemCheckedChange)
    }

    override fun onBindViewHolder(holder: PackagingVH, position: Int) {
        holder.bind(getItem(position))
    }

    class PackagingVH(
        private val binding: ItemPackagingShipmentBinding,
        private val onItemCheckedChange: (item: PackagingShipmentUiItem, isChecked: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackagingShipmentUiItem) = with(binding) {

            tvPackagingSerial.text = item.serialNumber
            tvCount.text = item.itemsCount.toString()

            cbSelected.setOnCheckedChangeListener(null)

            cbSelected.isChecked = item.isSelected

            cbSelected.setOnCheckedChangeListener { _, isChecked ->
                onItemCheckedChange(item, isChecked)
            }

            root.setOnClickListener {
                cbSelected.isChecked = !cbSelected.isChecked
            }
        }

    }

    class PackagingDiff : DiffUtil.ItemCallback<PackagingShipmentUiItem>() {
        override fun areItemsTheSame(
            oldItem: PackagingShipmentUiItem,
            newItem: PackagingShipmentUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PackagingShipmentUiItem,
            newItem: PackagingShipmentUiItem
        ): Boolean = oldItem == newItem
    }
}

data class PackagingShipmentUiItem(
    val id: Int,
    val serialNumber: String,
    val itemsCount: Int,
    val isSelected: Boolean
)
