package ru.faserkraft.client.presentation.packaging

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemPackagingContentProductBinding
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.presentation.product.toUiProductStatus

class PackagingContentAdapter(
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

            val uiStatus = item.status.toUiProductStatus()
            val ctx = root.context
            val bgColor = getColor(ctx, uiStatus.bgColorRes)
            root.setCardBackgroundColor(bgColor)

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
    val status: ProductStatus,
)