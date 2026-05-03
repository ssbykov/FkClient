package ru.faserkraft.client.presentation.packaging

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemPackagingContentProductBinding

class PackagingContentAdapter (
    private val onItemClick: (String) -> Unit
) :
    ListAdapter<PackagingContentUiItem, PackagingContentAdapter.ContentVH>(ContentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val binding = ItemPackagingContentProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentVH(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ContentVH, position: Int) {
        holder.bind(getItem(position))
    }

    class ContentVH(
        private val binding: ItemPackagingContentProductBinding,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackagingContentUiItem) = with(binding) {
            tvProductSerial.text = item.serialNumber
            tvProcessName.text = item.processName

            root.setOnClickListener {
                onItemClick(item.serialNumber)
            }
        }
    }

    class ContentDiff : DiffUtil.ItemCallback<PackagingContentUiItem>() {
        override fun areItemsTheSame(
            oldItem: PackagingContentUiItem,
            newItem: PackagingContentUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PackagingContentUiItem,
            newItem: PackagingContentUiItem
        ): Boolean = oldItem == newItem
    }
}

data class PackagingContentUiItem(
    val id: Int,
    val serialNumber: String,
    val processName: String,
)