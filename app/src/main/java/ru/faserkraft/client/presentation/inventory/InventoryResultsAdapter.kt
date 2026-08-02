package ru.faserkraft.client.presentation.inventory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryResultBinding
import ru.faserkraft.client.databinding.ItemInventoryResultHeaderBinding
import ru.faserkraft.client.domain.model.InventoryCompareResult

sealed class InventoryResultListItem {
    data class Header(val processId: Int, val processName: String) : InventoryResultListItem()
    data class Result(val data: InventoryCompareResult) : InventoryResultListItem()
}

class InventoryResultsAdapter(
    private val onUnexpectedClick: (InventoryCompareResult) -> Unit,
    private val onMissingClick: (InventoryCompareResult) -> Unit,
) : ListAdapter<InventoryResultListItem, RecyclerView.ViewHolder>(DiffCallback) {

    // ---------- ViewHolders ----------

    class HeaderViewHolder(
        private val binding: ItemInventoryResultHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InventoryResultListItem.Header) {
            binding.tvProcessName.text = item.processName
        }
    }

    inner class ResultViewHolder(
        private val binding: ItemInventoryResultBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryResultListItem.Result) = with(binding) {
            val result = item.data

            tvStepName.text = result.stepDefinition.name

            tvDbCount.text = root.context.getString(R.string.inventory_db_count, result.dbCount)
            tvScannedCount.text =
                root.context.getString(R.string.inventory_scanned_count, result.scannedCount)

            tvMissingChip.text =
                root.context.getString(R.string.inventory_missing_count, result.missing.size)

            tvUnexpectedChip.text =
                root.context.getString(R.string.inventory_unexpected_count, result.unexpected.size)

            tvOkChip.text =
                root.context.getString(R.string.inventory_matched_count, result.matched.size)

            tvUnexpectedChip.setOnClickListener {
                onUnexpectedClick(result)
            }

            tvMissingChip.setOnClickListener {
                onMissingClick(result)
            }
        }
    }

    // ---------- Adapter ----------

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is InventoryResultListItem.Header -> VIEW_TYPE_HEADER
        is InventoryResultListItem.Result -> VIEW_TYPE_RESULT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                ItemInventoryResultHeaderBinding.inflate(inflater, parent, false)
            )

            else -> ResultViewHolder(
                ItemInventoryResultBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is InventoryResultListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is InventoryResultListItem.Result -> (holder as ResultViewHolder).bind(item)
        }
    }

    // ---------- DiffCallback ----------

    private object DiffCallback : DiffUtil.ItemCallback<InventoryResultListItem>() {
        override fun areItemsTheSame(
            a: InventoryResultListItem,
            b: InventoryResultListItem,
        ) = when (a) {
            is InventoryResultListItem.Header if b is InventoryResultListItem.Header ->
                a.processId == b.processId

            is InventoryResultListItem.Result if b is InventoryResultListItem.Result ->
                a.data.stepDefinition.id == b.data.stepDefinition.id

            else -> false
        }

        override fun areContentsTheSame(
            a: InventoryResultListItem,
            b: InventoryResultListItem,
        ) = a == b
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_RESULT = 1
    }
}