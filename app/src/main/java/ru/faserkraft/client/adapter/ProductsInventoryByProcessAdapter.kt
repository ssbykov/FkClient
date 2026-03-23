package ru.faserkraft.client.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemProductDetailBinding
import ru.faserkraft.client.utils.formatIsoToUi

@RequiresApi(Build.VERSION_CODES.O)
class ProductsInventoryByProcessAdapter :
    ListAdapter<ProductsInventoryByProcessUiItem, ProductsInventoryByProcessAdapter.ContentVH>(ContentDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val binding = ItemProductDetailBinding.inflate(
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
        private val binding: ItemProductDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductsInventoryByProcessUiItem) = with(binding) {
            tvProductSerial.text = item.serialNumber
            tvCreated.text = binding.root.context.getString(
                R.string.created_at,
                formatIsoToUi(item.createdAt)
            )

        }
    }

    class ContentDiff : DiffUtil.ItemCallback<ProductsInventoryByProcessUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductsInventoryByProcessUiItem,
            newItem: ProductsInventoryByProcessUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProductsInventoryByProcessUiItem,
            newItem: ProductsInventoryByProcessUiItem
        ): Boolean = oldItem == newItem
    }
}

data class ProductsInventoryByProcessUiItem(
    val id: Long,
    val serialNumber: String,
    val createdAt: String,
)
