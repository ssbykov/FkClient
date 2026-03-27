package ru.faserkraft.client.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemStorageBinding

class ProductsStorageAdapter(
    private val onItemClick: (ProductsStorageUiItem) -> Unit
) : ListAdapter<ProductsStorageUiItem, ProductsStorageAdapter.ContentVH>(ContentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val binding = ItemStorageBinding.inflate(
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
        private val binding: ItemStorageBinding,
        private val onItemClick: (ProductsStorageUiItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: ProductsStorageUiItem) = with(binding) {
            binding.tvWorkProcess.text = item.process
            binding.tvDoneValue.text = item.productCount
            binding.tvPackagingValue.text = item.packagingCount


            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class ContentDiff : DiffUtil.ItemCallback<ProductsStorageUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductsStorageUiItem,
            newItem: ProductsStorageUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProductsStorageUiItem,
            newItem: ProductsStorageUiItem
        ): Boolean = oldItem == newItem
    }
}

data class ProductsStorageUiItem(
    val id: Int,
    val process: String,
    val productCount: String,
    val packagingCount: String,
)
