package ru.faserkraft.client.presentation.inventory

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemInventoryDiffProductBinding
import ru.faserkraft.client.domain.model.ProductInventoryItem
import ru.faserkraft.client.presentation.product.detail.toUiProductStatus

class InventoryDiffDetailAdapter(
    private val onSyncClick: (ProductInventoryItem) -> Unit
) : ListAdapter<ProductInventoryItem, InventoryDiffDetailAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(
        private val binding: ItemInventoryDiffProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductInventoryItem) = with(binding) {
            tvSerialNumber.text = item.serialNumber

            val uiStatus = item.status.toUiProductStatus()
            val ctx = root.context
            val bgColor = getColor(ctx, uiStatus.bgColorRes)
            val textColor = getColor(ctx, uiStatus.textColorRes)

            // Статус продукта
            chipProductStatus.text = getString(ctx, uiStatus.titleRes)

            chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chipProductStatus.setTextColor(textColor)

            tvLastClosedStep.text = item.stepDefinition.name

            btnSync.setOnClickListener {
                onSyncClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemInventoryDiffProductBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductInventoryItem>() {
        override fun areItemsTheSame(a: ProductInventoryItem, b: ProductInventoryItem) =
            a.id == b.id

        override fun areContentsTheSame(a: ProductInventoryItem, b: ProductInventoryItem) =
            a == b
    }
}