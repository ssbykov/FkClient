package ru.faserkraft.client.presentation.inventory

import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryProductResultBinding
import ru.faserkraft.client.domain.model.ProductInventoryCompareItem
import ru.faserkraft.client.presentation.product.detail.toUiProductStatus

class InventoryProductResultsAdapter(
    private val onItemClick: (ProductInventoryCompareItem) -> Unit
) : ListAdapter<ProductInventoryCompareItem, InventoryProductResultsAdapter.ProductViewHolder>(
    DiffCallback
) {

    inner class ProductViewHolder(
        private val binding: ItemInventoryProductResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: ProductInventoryCompareItem) = with(binding) {
            val context = root.context
            tvSerialNumber.text = item.serialNumber

            // 1. Статус продукта
            val dbStatusText = item.status.toUiProductStatus().getTitle(context)
            tvProductStatus.text =
                context.getString(R.string.product_status_format, dbStatusText)

            // 2. Названия этапов
            val accStep = item.accountingStepDefinition
            val invStep = item.inventoryStepDefinition

            tvAccountingStep.text = accStep?.name ?: "Отсутствует/Упакован"
            tvInventoryStep.text = invStep?.name ?: "Не отсканирован"

            // 3. Определение статуса расхождения через CompareStatus
            val (statusText, colorResId, iconRes) = when (item.compareStatus) {
                CompareStatus.MISSING -> Triple(
                    "ОТСУТСТВУЕТ",
                    R.color.step_mismatch,
                    R.drawable.ic_error_outline
                )

                CompareStatus.UNEXPECTED -> Triple(
                    "ЛИШНИЙ",
                    R.color.step_mismatch,
                    R.drawable.ic_warning
                )

                CompareStatus.STEP_MISMATCH -> Triple(
                    "ОШИБКА ЭТАПА",
                    R.color.step_mismatch,
                    R.drawable.ic_warning
                )

                CompareStatus.MATCHED -> Triple(
                    "СОВПАЛ",
                    R.color.step_match,
                    R.drawable.ic_check_circle
                )
            }

            // 4. Применение текстов и иконок
            tvStatusLabel.text = statusText
            ivStatusIcon.setImageResource(iconRes)

            // 5. Разрешение цветов (прямо из R.color)
            val resolvedColor = ContextCompat.getColor(context, colorResId)

            tvStatusLabel.setTextColor(resolvedColor)
            ivStatusIcon.imageTintList = ColorStateList.valueOf(resolvedColor)

            // 6. Подсветка текста этапа инвентаризации при несовпадении
            if (item.compareStatus != CompareStatus.MATCHED) {
                tvInventoryStep.setTextColor(resolvedColor)
            } else {
                // Извлечение цвета colorOnSurface из темы
                val typedValue = TypedValue()
                context.theme.resolveAttribute(
                    com.google.android.material.R.attr.colorOnSurface,
                    typedValue,
                    true
                )
                tvInventoryStep.setTextColor(ContextCompat.getColor(context, typedValue.resourceId))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemInventoryProductResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProductInventoryCompareItem>() {
        override fun areItemsTheSame(
            oldItem: ProductInventoryCompareItem,
            newItem: ProductInventoryCompareItem
        ): Boolean {
            return oldItem.serialNumber == newItem.serialNumber
        }

        override fun areContentsTheSame(
            oldItem: ProductInventoryCompareItem,
            newItem: ProductInventoryCompareItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}