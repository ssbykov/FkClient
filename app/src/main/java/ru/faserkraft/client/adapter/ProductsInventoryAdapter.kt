package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryProcessHeaderBinding
import ru.faserkraft.client.databinding.ItemInventoryStageBinding
import ru.faserkraft.client.dto.ProductsInventoryDto


class ProductsInventoryAdapter(
    private val onStageClick: (ProductsInventoryDto) -> Unit
) :
    ListAdapter<ProductsInventoryUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_PROCESS_HEADER = 0
        private const val TYPE_STAGE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ProductsInventoryUiItem.ProcessHeader -> TYPE_PROCESS_HEADER
            is ProductsInventoryUiItem.StageItem -> TYPE_STAGE_ITEM
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_PROCESS_HEADER -> {
                val binding = ItemInventoryProcessHeaderBinding.inflate(inflater, parent, false)
                ProcessHeaderVH(binding)
            }

            else -> {
                val binding = ItemInventoryStageBinding.inflate(inflater, parent, false)
                StageVH(binding, onStageClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ProductsInventoryUiItem.ProcessHeader ->
                (holder as ProcessHeaderVH).bind(item)

            is ProductsInventoryUiItem.StageItem ->
                (holder as StageVH).bind(item.dto)
        }
    }

    class ProcessHeaderVH(
        private val binding: ItemInventoryProcessHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductsInventoryUiItem.ProcessHeader) {
            binding.tvProcessName.text = item.processName
        }
    }

    class StageVH(
        private val binding: ItemInventoryStageBinding,
        private val onClick: (ProductsInventoryDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dto: ProductsInventoryDto) = with(binding) {
            tvStepName.text = dto.stepName
            tvCount.text = itemView.context.getString(
                R.string.state_count,
                dto.count
            )
            root.setOnClickListener {
                onClick(dto)
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<ProductsInventoryUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductsInventoryUiItem,
            newItem: ProductsInventoryUiItem
        ): Boolean =
            when {
                oldItem is ProductsInventoryUiItem.ProcessHeader &&
                        newItem is ProductsInventoryUiItem.ProcessHeader ->
                    oldItem.processName == newItem.processName

                oldItem is ProductsInventoryUiItem.StageItem &&
                        newItem is ProductsInventoryUiItem.StageItem ->
                    oldItem.dto.processId == newItem.dto.processId &&
                            oldItem.dto.stepDefinitionId == newItem.dto.stepDefinitionId

                else -> false
            }

        override fun areContentsTheSame(
            oldItem: ProductsInventoryUiItem,
            newItem: ProductsInventoryUiItem
        ): Boolean = oldItem == newItem
    }
}

sealed class ProductsInventoryUiItem {
    data class ProcessHeader(val processName: String) : ProductsInventoryUiItem()
    data class StageItem(val dto: ProductsInventoryDto) : ProductsInventoryUiItem()
}
