package ru.faserkraft.client.presentation.inventory

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as MaterialR
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryProductResultBinding
import ru.faserkraft.client.domain.model.StepDefinitionWithProcess
import ru.faserkraft.client.presentation.product.detail.toUiProductStatus

class InventoryProductResultsAdapter(
    private val onItemClick: (ProductInventoryCompareUiItem) -> Unit,
    private val onAcceptAccountingStep: (ProductInventoryCompareUiItem, StepDefinitionWithProcess) -> Unit,
    private val onAcceptInventoryStep: (ProductInventoryCompareUiItem, StepDefinitionWithProcess) -> Unit,
    private val onSelectCustomStep: (ProductInventoryCompareUiItem) -> Unit
) : ListAdapter<ProductInventoryCompareUiItem, InventoryProductResultsAdapter.ProductViewHolder>(
    DiffCallback
) {

    inner class ProductViewHolder(
        private val binding: ItemInventoryProductResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Кэшируем цвета темы один раз на ViewHolder для высокой производительности скролла
        private val transparentColor = ColorStateList.valueOf(Color.TRANSPARENT)
        private val activeRippleColor = MaterialColors.getColorStateList(
            binding.root.context,
            android.R.attr.colorControlHighlight,
            ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.step_match))
        )
        private val activeStrokeColor = MaterialColors.getColor(binding.root, MaterialR.attr.colorOutline)
        private val passiveStrokeColor = MaterialColors.getColor(binding.root, MaterialR.attr.colorOutlineVariant)
        private val defaultTextColor = MaterialColors.getColor(binding.root, MaterialR.attr.colorOnSurface)

        init {
            binding.layoutHeader.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: ProductInventoryCompareUiItem) = with(binding) {
            val context = root.context
            val isMismatch = item.isMismatch

            // 1. Заголовок и статус продукта
            tvSerialNumber.text = item.serialNumber
            val dbStatusText = item.status.toUiProductStatus().getTitle(context)
            tvProductStatus.text = context.getString(R.string.product_status_format, dbStatusText)

            // 2. Названия этапов
            val accStep = item.accountingStep
            val invStep = item.inventoryStep
            tvAccountingStep.text = accStep?.name ?: context.getString(R.string.step_missing_packed)
            tvInventoryStep.text = invStep?.name ?: context.getString(R.string.step_not_scanned)

            // 3. Оформление бейджа и иконки
            val status = item.compareStatus
            val statusColor = ContextCompat.getColor(context, status.colorRes)
            tvStatusLabel.setText(status.titleRes)
            tvStatusLabel.setTextColor(statusColor)
            ivStatusIcon.setImageResource(status.iconRes)
            ivStatusIcon.imageTintList = ColorStateList.valueOf(statusColor)

            // 4. Текст этапа инвентаризации
            tvInventoryStep.setTextColor(if (isMismatch) statusColor else defaultTextColor)

            // 5. Видимость вспомогательных элементов
            tvResolvePrompt.isVisible = isMismatch
            btnCustomStep.isVisible = isMismatch

            // 6. Настройка карточки «По учету»
            cardAccountingOption.setupOption(
                isAvailable = isMismatch && accStep != null,
                isSelected = item.resolvedStep?.id == accStep?.id,
                onClick = { accStep?.let { onAcceptAccountingStep(item, it) } }
            )

            // 7. Настройка карточки «Фактически»
            cardInventoryOption.setupOption(
                isAvailable = isMismatch && invStep != null,
                isSelected = item.resolvedStep?.id == invStep?.id,
                onClick = { invStep?.let { onAcceptInventoryStep(item, it) } }
            )

            // 8. Кнопка ручного выбора
            btnCustomStep.setOnClickListener(if (isMismatch) { { onSelectCustomStep(item) } } else null)
        }

        /**
         * Компактная настройка визуального и интерактивного состояния опции выбора
         */
        private fun MaterialCardView.setupOption(
            isAvailable: Boolean,
            isSelected: Boolean,
            onClick: () -> Unit
        ) {
            rippleColor = if (isAvailable) activeRippleColor else transparentColor
            isClickable = isAvailable
            isFocusable = isAvailable
            isCheckable = isAvailable
            isChecked = isAvailable && isSelected
            strokeColor = if (isAvailable) activeStrokeColor else passiveStrokeColor
            setOnClickListener(if (isAvailable) { { onClick() } } else null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder =
        ProductViewHolder(
            ItemInventoryProductResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<ProductInventoryCompareUiItem>() {
        override fun areItemsTheSame(
            oldItem: ProductInventoryCompareUiItem,
            newItem: ProductInventoryCompareUiItem
        ): Boolean = oldItem.serialNumber == newItem.serialNumber

        override fun areContentsTheSame(
            oldItem: ProductInventoryCompareUiItem,
            newItem: ProductInventoryCompareUiItem
        ): Boolean = oldItem == newItem
    }
}