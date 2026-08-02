package ru.faserkraft.client.presentation.inventory

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemInventoryMissingProductBinding
import ru.faserkraft.client.domain.model.ProductInventoryItem
import ru.faserkraft.client.presentation.product.detail.toUiProductStatus
import ru.faserkraft.client.utils.converter.formatIsoToUi

class InventoryMissingDetailAdapter(
    private val onActionClick: (ProductInventoryItem) -> Unit
) : ListAdapter<ProductInventoryItem, InventoryMissingDetailAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemInventoryMissingProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductInventoryItem) = with(binding) {
            tvSerialNumber.text = item.serialNumber

            val uiStatus = item.status.toUiProductStatus()
            val ctx = root.context
            val bgColor = ContextCompat.getColor(ctx, uiStatus.bgColorRes)
            val textColor = ContextCompat.getColor(ctx, uiStatus.textColorRes)
            chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chipProductStatus.setTextColor(textColor)
            chipProductStatus.text = getString(ctx, uiStatus.titleRes)

            tvPerformedAt.text = formatIsoToUi(item.performedAt)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(ItemInventoryMissingProductBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductInventoryItem>() {
        override fun areItemsTheSame(a: ProductInventoryItem, b: ProductInventoryItem) =
            a.serialNumber == b.serialNumber

        override fun areContentsTheSame(a: ProductInventoryItem, b: ProductInventoryItem) =
            a == b
    }
}