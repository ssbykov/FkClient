package ru.faserkraft.client.presentation.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemPackagingBinding
import ru.faserkraft.client.presentation.order.ModuleTypeUi

data class PackagingListUiItem(
    val id: Int,
    val serialNumber: String,
    val totalCount: Int,
    val types: List<ModuleTypeUi>,
    val performedBy: String? = null,
    val performedAt: String? = null
)

class PackagingListAdapter(
    private val onItemClick: (item: PackagingListUiItem) -> Unit
) : ListAdapter<PackagingListUiItem, PackagingListAdapter.PackagingVH>(PackagingDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackagingVH {
        val binding = ItemPackagingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackagingVH(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: PackagingVH, position: Int) {
        holder.bind(getItem(position))
    }

    class PackagingVH(
        private val binding: ItemPackagingBinding,
        private val onItemClick: (item: PackagingListUiItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackagingListUiItem) = with(binding) {
            tvPackagingSerial.text = item.serialNumber
            tvTotalCount.text = itemView.context.getString(
                R.string.total_items_count,
                item.totalCount
            )

            // Дата упаковки
            if (!item.performedAt.isNullOrBlank()) {
                tvPerformedAt.isVisible = true
                tvPerformedAt.text = item.performedAt
            } else {
                tvPerformedAt.isVisible = false
            }

            // Кто упаковал
            if (!item.performedBy.isNullOrBlank()) {
                tvPerformedBy.isVisible = true
                tvPerformedBy.text = itemView.context.getString(
                    R.string.packaging_performed_by,
                    item.performedBy
                )
            } else {
                tvPerformedBy.isVisible = false
            }

            // Чипы
            chipGroupTypes.removeAllViews()
            item.types.forEach { typeInfo ->
                val chipText = itemView.context.getString(
                    R.string.items_in_box,
                    typeInfo.name,
                    typeInfo.count
                )
                val chip = Chip(root.context).apply {
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

    class PackagingDiff : DiffUtil.ItemCallback<PackagingListUiItem>() {
        override fun areItemsTheSame(
            oldItem: PackagingListUiItem,
            newItem: PackagingListUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PackagingListUiItem,
            newItem: PackagingListUiItem
        ): Boolean = oldItem == newItem
    }
}
