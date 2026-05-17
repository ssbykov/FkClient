package ru.faserkraft.client.presentation.product.inventory


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemProductDetailBinding
import ru.faserkraft.client.utils.converter.formatIsoToUi


data class ProductModuleUiItem(
    val id: Long,
    val serialNumber: String,
    val createdAt: String,
)

class ProductsReworkScrapListAdapter(
    private val onItemClick: (String) -> Unit
) : androidx.recyclerview.widget.ListAdapter<ProductModuleUiItem, ProductsReworkScrapListAdapter.ContentVH>(
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
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductModuleUiItem) = with(binding) {
            tvProductSerial.text = item.serialNumber

            tvCreated.text = binding.root.context.getString(
                R.string.product_start_at,
                formatIsoToUi(item.createdAt)
            )

            binding.root.setOnClickListener {
                onClick(item.serialNumber)
            }
        }
    }

    class ContentDiff : DiffUtil.ItemCallback<ProductModuleUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductModuleUiItem,
            newItem: ProductModuleUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProductModuleUiItem,
            newItem: ProductModuleUiItem
        ): Boolean = oldItem == newItem
    }
}