package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryProcessHeaderBinding
import ru.faserkraft.client.databinding.ItemInventoryStageBinding
import ru.faserkraft.client.domain.model.ProductsInventory

sealed class ProductsInventoryUiItem {
    data class ProcessHeader(val processName: String) : ProductsInventoryUiItem()
    data class StageItem(val item: ProductsInventory) : ProductsInventoryUiItem()
}

class ProductsInventoryAdapter(
    private val onStageClick: (ProductsInventory) -> Unit,
) : ListAdapter<ProductsInventoryUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_PROCESS_HEADER = 0
        private const val TYPE_STAGE_ITEM = 1
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is ProductsInventoryUiItem.ProcessHeader -> TYPE_PROCESS_HEADER
        is ProductsInventoryUiItem.StageItem -> TYPE_STAGE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PROCESS_HEADER -> ProcessHeaderVH(
                ItemInventoryProcessHeaderBinding.inflate(inflater, parent, false)
            )

            else -> StageVH(
                ItemInventoryStageBinding.inflate(inflater, parent, false),
                onStageClick,
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ProductsInventoryUiItem.ProcessHeader -> (holder as ProcessHeaderVH).bind(item)
            is ProductsInventoryUiItem.StageItem -> (holder as StageVH).bind(item.item)
        }
    }

    class ProcessHeaderVH(
        private val binding: ItemInventoryProcessHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductsInventoryUiItem.ProcessHeader) {
            binding.tvProcessName.text = item.processName
        }
    }

    class StageVH(
        private val binding: ItemInventoryStageBinding,
        private val onClick: (ProductsInventory) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductsInventory) = with(binding) {
            tvStepName.text = item.stepNameGenitive
            tvCount.text = itemView.context.getString(R.string.state_count, item.count)
            root.setOnClickListener { onClick(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<ProductsInventoryUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductsInventoryUiItem,
            newItem: ProductsInventoryUiItem,
        ) = when {
            oldItem is ProductsInventoryUiItem.ProcessHeader &&
                    newItem is ProductsInventoryUiItem.ProcessHeader ->
                oldItem.processName == newItem.processName

            oldItem is ProductsInventoryUiItem.StageItem &&
                    newItem is ProductsInventoryUiItem.StageItem ->
                oldItem.item.processId == newItem.item.processId &&
                        oldItem.item.stepDefinitionId == newItem.item.stepDefinitionId

            else -> false
        }

        override fun areContentsTheSame(
            oldItem: ProductsInventoryUiItem,
            newItem: ProductsInventoryUiItem,
        ) = oldItem == newItem
    }
}