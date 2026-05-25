package ru.faserkraft.client.presentation.inventory.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemProductDetailBinding
import ru.faserkraft.client.utils.converter.formatIsoToUi

class ProductsOverviewByProcessAdapter(
    private val onItemClick: (String) -> Unit
) :
    androidx.recyclerview.widget.ListAdapter<ProductsOverviewByProcessUiItem, ProductsOverviewByProcessAdapter.ContentVH>(
        ContentDiff()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentVH {
        val binding = ItemProductDetailBinding.inflate(
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
        private val binding: ItemProductDetailBinding,
        private val onStageClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductsOverviewByProcessUiItem) = with(binding) {
            tvProductSerial.text = item.serialNumber
            tvCreated.text = binding.root.context.getString(
                R.string.closed_at,
                formatIsoToUi(item.createdAt)
            )
            binding.root.setOnClickListener {
                onStageClick(item.serialNumber)
            }

        }
    }

    class ContentDiff : DiffUtil.ItemCallback<ProductsOverviewByProcessUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductsOverviewByProcessUiItem,
            newItem: ProductsOverviewByProcessUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProductsOverviewByProcessUiItem,
            newItem: ProductsOverviewByProcessUiItem
        ): Boolean = oldItem == newItem
    }
}

data class ProductsOverviewByProcessUiItem(
    val id: Long,
    val serialNumber: String,
    val createdAt: String,
)
