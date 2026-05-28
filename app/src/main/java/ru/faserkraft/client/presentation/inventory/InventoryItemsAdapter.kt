package ru.faserkraft.client.presentation.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryEntryBinding
import ru.faserkraft.client.databinding.ItemInventoryProcessHeaderBinding
import ru.faserkraft.client.domain.model.InventoryItem
import ru.faserkraft.client.utils.converter.formatIsoToUi

sealed class InventoryItemUiItem {
    data class ProcessHeader(val processName: String) : InventoryItemUiItem()
    data class Entry(val item: InventoryItem) : InventoryItemUiItem()
}

class InventoryItemsAdapter :
    ListAdapter<InventoryItemUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is InventoryItemUiItem.ProcessHeader -> TYPE_HEADER
        is InventoryItemUiItem.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(
                ItemInventoryProcessHeaderBinding.inflate(inflater, parent, false)
            )

            else -> EntryVH(
                ItemInventoryEntryBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val uiItem = getItem(position)) {
            is InventoryItemUiItem.ProcessHeader -> (holder as HeaderVH).bind(uiItem)
            is InventoryItemUiItem.Entry -> (holder as EntryVH).bind(uiItem.item)
        }
    }

    class HeaderVH(
        private val binding: ItemInventoryProcessHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uiItem: InventoryItemUiItem.ProcessHeader) {
            binding.tvProcessName.text = uiItem.processName
        }
    }

    class EntryVH(
        private val binding: ItemInventoryEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryItem) = with(binding) {
            val context = itemView.context
            tvSerialNumber.text = item.serialNumber

            tvStepName.text = context.getString(
                R.string.step_last_title,
                item.stepDefinition.name
            )

            tvScannedAt.text = context.getString(
                R.string.inventory_item_scanned_at,
                formatIsoToUi(item.scannedAt)
            )
        }
    }

    class Diff : DiffUtil.ItemCallback<InventoryItemUiItem>() {
        override fun areItemsTheSame(
            oldItem: InventoryItemUiItem,
            newItem: InventoryItemUiItem
        ): Boolean = when {
            oldItem is InventoryItemUiItem.ProcessHeader && newItem is InventoryItemUiItem.ProcessHeader ->
                oldItem.processName == newItem.processName

            oldItem is InventoryItemUiItem.Entry && newItem is InventoryItemUiItem.Entry ->
                oldItem.item.id == newItem.item.id

            else -> false
        }

        override fun areContentsTheSame(
            oldItem: InventoryItemUiItem,
            newItem: InventoryItemUiItem
        ): Boolean = oldItem == newItem
    }
}