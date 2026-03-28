package ru.faserkraft.client.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemShippedPartitionBinding

class ShippedPartitionsAdapter(
    private val onItemClick: ((ShippedPartitionsUiItem) -> Unit)
) : ListAdapter<ShippedPartitionsUiItem, ShippedPartitionsAdapter.ContentVH>(ContentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val binding = ItemShippedPartitionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentVH(binding, onItemClick)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ContentVH, position: Int) {
        holder.bind(getItem(position))
    }

    class ContentVH(
        private val binding: ItemShippedPartitionBinding,
        private val onItemClick: ((ShippedPartitionsUiItem) -> Unit)
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: ShippedPartitionsUiItem) = with(binding) {
            tvShipmentDate.text = item.shipmentDate
            tvPackagingCount.text = itemView.context.getString(
                R.string.total_packaging_count,
                item.packagingCount
            )
            tvModuleCount.text = itemView.context.getString(
                R.string.total_items_count,
                item.moduleCount
            )

            // Очищаем старые чипы и добавляем новые
            chipGroupTypes.removeAllViews()
            item.moduleTypes.forEach { moduleType ->
                val chip = Chip(chipGroupTypes.context).apply {
                    val chipText = itemView.context.getString(
                        R.string.items_in_box,
                        moduleType.type,
                        moduleType.count
                    )
                    text = chipText
                    isCloseIconVisible = false
                    isClickable = false
                }
                chipGroupTypes.addView(chip)
            }

            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class ContentDiff : DiffUtil.ItemCallback<ShippedPartitionsUiItem>() {
        override fun areItemsTheSame(
            oldItem: ShippedPartitionsUiItem,
            newItem: ShippedPartitionsUiItem
        ): Boolean = oldItem.shipmentDate == newItem.shipmentDate

        override fun areContentsTheSame(
            oldItem: ShippedPartitionsUiItem,
            newItem: ShippedPartitionsUiItem
        ): Boolean = oldItem == newItem
    }
}

data class ShippedPartitionsUiItem(
    val shipmentDate: String,
    val packagingCount: Int,
    val moduleCount: Int,
    val moduleTypes: List<ModuleTypeDto>
)

data class ModuleTypeDto(
    val type: String,
    val count: Int
)
