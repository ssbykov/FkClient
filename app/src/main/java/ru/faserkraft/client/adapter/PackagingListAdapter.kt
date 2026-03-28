package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemPackagingBinding

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
        private val onChipCheckedChange: (item: PackagingListUiItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackagingListUiItem) = with(binding) {

            tvPackagingSerial.text = item.serialNumber
            tvTotalCount.text = itemView.context.getString(
                R.string.total_items_count,
                item.totalCount
            )

            chipGroupTypes.removeAllViews()
            item.types.forEach { typeInfo ->
                val chipText = itemView.context.getString(
                    R.string.items_in_box,
                    typeInfo.name,
                    typeInfo.count
                )
                val chip = Chip(root.context).apply {
                    text = chipText
                    setTextAppearance(R.style.Widget_FK_ModuleTypeChip)
                }
                chipGroupTypes.addView(chip)
            }
            root.setOnClickListener {
                onChipCheckedChange(item)
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

data class PackagingListUiItem(
    val id: Int,
    val serialNumber: String,
    val totalCount: Int,
    val types: List<ModuleTypeUi>
)
