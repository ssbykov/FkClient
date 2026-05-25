package ru.faserkraft.client.presentation.inventory.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryProcessHeaderBinding
import ru.faserkraft.client.databinding.ItemReworkScrapStatBinding
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.presentation.product.detail.toUiProductStatus


sealed class ReworkScrapUiItem {
    data class ProcessHeader(val processName: String) : ReworkScrapUiItem()
    data class StatusStatItem(
        val processName: String,
        val status: ProductStatus,
        val count: Int
    ) : ReworkScrapUiItem()
}

class ProductsReworkScrapAdapter(
    private val onStatClick: (ReworkScrapUiItem.StatusStatItem) -> Unit
) : androidx.recyclerview.widget.ListAdapter<ReworkScrapUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_PROCESS_HEADER = 0
        private const val TYPE_STAT_ITEM = 1
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ReworkScrapUiItem.ProcessHeader -> TYPE_PROCESS_HEADER
        is ReworkScrapUiItem.StatusStatItem -> TYPE_STAT_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PROCESS_HEADER -> ProcessHeaderVH(
                ItemInventoryProcessHeaderBinding.inflate(inflater, parent, false)
            )

            else -> StatVH(
                ItemReworkScrapStatBinding.inflate(inflater, parent, false),
                onStatClick
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ReworkScrapUiItem.ProcessHeader -> (holder as ProcessHeaderVH).bind(item)
            is ReworkScrapUiItem.StatusStatItem -> (holder as StatVH).bind(item)
        }
    }

    class ProcessHeaderVH(
        private val binding: ItemInventoryProcessHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReworkScrapUiItem.ProcessHeader) {
            binding.tvProcessName.text = item.processName
        }
    }

    class StatVH(
        private val binding: ItemReworkScrapStatBinding,
        private val onClick: (ReworkScrapUiItem.StatusStatItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ReworkScrapUiItem.StatusStatItem) = with(binding) {
            val uiStatus = item.status.toUiProductStatus()
            val ctx = root.context
            root.setCardBackgroundColor(ContextCompat.getColor(ctx, uiStatus.bgColorRes))
            tvStatusName.setTextColor(ContextCompat.getColor(ctx, uiStatus.textColorRes))
            tvStatusName.text = uiStatus.getTitle(ctx)
            tvCount.text = itemView.context.getString(R.string.state_count, item.count)
            root.setOnClickListener { onClick(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<ReworkScrapUiItem>() {
        override fun areItemsTheSame(oldItem: ReworkScrapUiItem, newItem: ReworkScrapUiItem) =
            when {
                oldItem is ReworkScrapUiItem.ProcessHeader && newItem is ReworkScrapUiItem.ProcessHeader ->
                    oldItem.processName == newItem.processName

                oldItem is ReworkScrapUiItem.StatusStatItem && newItem is ReworkScrapUiItem.StatusStatItem ->
                    oldItem.processName == newItem.processName && oldItem.status == newItem.status

                else -> false
            }

        override fun areContentsTheSame(oldItem: ReworkScrapUiItem, newItem: ReworkScrapUiItem) =
            oldItem == newItem
    }
}