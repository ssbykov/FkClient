package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemPackagingContentProductBinding

class PackagingContentAdapter :
    ListAdapter<PackagingContentUiItem, PackagingContentAdapter.ContentVH>(ContentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val binding = ItemPackagingContentProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentVH(binding)
    }

    override fun onBindViewHolder(holder: ContentVH, position: Int) {
        holder.bind(getItem(position))
    }

    class ContentVH(
        private val binding: ItemPackagingContentProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackagingContentUiItem) = with(binding) {
            // номер продукта
            tvProductSerial.text = item.serialNumber
            // название процесса
            tvProcessName.text = item.processName
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
