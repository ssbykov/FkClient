package ru.faserkraft.client.presentation.inventory.overview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryProcessHeaderBinding
import ru.faserkraft.client.databinding.ItemOverviewStageBinding
import ru.faserkraft.client.domain.model.ProductsOverview

sealed class ProductsOverviewUiItem {
    data class ProcessHeader(val processName: String) : ProductsOverviewUiItem()
    data class StageItem(val item: ProductsOverview) : ProductsOverviewUiItem()
}

class ProductsOverviewAdapter(
    private val onStageClick: (ProductsOverview) -> Unit,
) : androidx.recyclerview.widget.ListAdapter<ProductsOverviewUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_PROCESS_HEADER = 0
        private const val TYPE_STAGE_ITEM = 1
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ProductsOverviewUiItem.ProcessHeader -> TYPE_PROCESS_HEADER
        is ProductsOverviewUiItem.StageItem -> TYPE_STAGE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PROCESS_HEADER -> ProcessHeaderVH(
                ItemInventoryProcessHeaderBinding.inflate(inflater, parent, false)
            )

            else -> StageVH(
                ItemOverviewStageBinding.inflate(inflater, parent, false),
                onStageClick,
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ProductsOverviewUiItem.ProcessHeader -> (holder as ProcessHeaderVH).bind(item)
            is ProductsOverviewUiItem.StageItem -> (holder as StageVH).bind(item.item)
        }
    }

    class ProcessHeaderVH(
        private val binding: ItemInventoryProcessHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductsOverviewUiItem.ProcessHeader) {
            binding.tvProcessName.text = item.processName
        }
    }

    class StageVH(
        private val binding: ItemOverviewStageBinding,
        private val onClick: (ProductsOverview) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductsOverview) = with(binding) {
            tvStepName.text = item.stepNameGenitive
            tvCount.text = itemView.context.getString(R.string.state_count, item.count)
            root.setOnClickListener { onClick(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<ProductsOverviewUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductsOverviewUiItem,
            newItem: ProductsOverviewUiItem,
        ) = when (oldItem) {
            is ProductsOverviewUiItem.ProcessHeader if newItem is ProductsOverviewUiItem.ProcessHeader ->
                oldItem.processName == newItem.processName

            is ProductsOverviewUiItem.StageItem if newItem is ProductsOverviewUiItem.StageItem ->
                oldItem.item.processId == newItem.item.processId &&
                        oldItem.item.stepDefinitionId == newItem.item.stepDefinitionId

            else -> false
        }

        override fun areContentsTheSame(
            oldItem: ProductsOverviewUiItem,
            newItem: ProductsOverviewUiItem,
        ) = oldItem == newItem
    }
}