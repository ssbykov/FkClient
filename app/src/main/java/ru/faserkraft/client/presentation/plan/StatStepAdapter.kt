package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStatStepRowBinding
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class StatStepAdapter(
    private val showProgress: Boolean = true,
    private val showDivider: Boolean = true
) : ListAdapter<StepCountUiItem, StatStepAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(
        private val binding: ItemStatStepRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StepCountUiItem, isLast: Boolean) = with(binding) {
            tvStepName.text = item.stepName

            val avgText = String.format(Locale.getDefault(), "%.1f", item.dailyAverage)

            tvStepCount.text = root.context.getString(
                R.string.stat_step_fact_only_format,
                item.count
            )
            tvStepAvg.text = root.context.getString(
                R.string.stat_step_avg_inline_format,
                avgText
            )

            tvPercentage.isVisible = true

            if (item.planCount != null && item.completionPercentage != null) {
                val percentageInt = item.completionPercentage.toInt()

                tvPlanCount.text = root.context.getString(
                    R.string.stat_step_plan_only_format,
                    item.planCount
                )
                tvPercentage.text = root.context.getString(
                    R.string.stat_percentage_format,
                    percentageInt
                )

                if (showProgress) {
                    progressStep.isVisible = true
                    progressStep.setProgressCompat(percentageInt.coerceIn(0, 100), true)
                } else {
                    progressStep.isVisible = false
                }
            } else {
                // Плана нет - явно показываем "Без плана", считаем
                // выполнение 100% (план забыли указать).
                tvPlanCount.text = root.context.getString(R.string.stat_step_no_plan_label)
                tvPercentage.text = root.context.getString(
                    R.string.stat_percentage_format,
                    100
                )

                if (showProgress) {
                    progressStep.isVisible = true
                    progressStep.setProgressCompat(100, true)
                } else {
                    progressStep.isVisible = false
                }
            }

            if (item.amount > BigDecimal.ZERO) {
                tvStepAmount.isVisible = true
                tvStepAmount.text = formatCurrency(item.amount)
            } else {
                tvStepAmount.isVisible = false
            }

            stepDivider.isVisible = showDivider && !isLast
        }

        private fun formatCurrency(amount: BigDecimal): String {
            val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru")).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return binding.root.context.getString(
                R.string.stat_step_amount_format,
                format.format(amount)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatStepRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position), position == itemCount - 1)

    private object DiffCallback : DiffUtil.ItemCallback<StepCountUiItem>() {
        override fun areItemsTheSame(old: StepCountUiItem, new: StepCountUiItem) =
            old.stepDefinitionId == new.stepDefinitionId

        override fun areContentsTheSame(old: StepCountUiItem, new: StepCountUiItem) =
            old == new
    }
}
