package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStatProcessStepBinding
import java.util.Locale

class ProcessStepStatAdapter :
    ListAdapter<StepCountUiItem, ProcessStepStatAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemStatProcessStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StepCountUiItem, isLast: Boolean) = with(binding) {
            tvStepName.text = item.stepName

            val avgText = String.format(Locale.getDefault(), "%.1f", item.dailyAverage)

            if (item.planCount != null && item.completionPercentage != null) {
                val percentageInt = item.completionPercentage.toInt()

                tvStepDetails.text = root.context.getString(
                    R.string.stat_step_compact_with_plan_format,
                    item.count,
                    item.planCount,
                    avgText
                )
                tvPercentage.text = root.context.getString(
                    R.string.stat_percentage_format,
                    percentageInt
                )
            } else {
                tvStepDetails.text = root.context.getString(
                    R.string.stat_step_compact_no_plan_format,
                    item.count,
                    avgText
                )
                tvPercentage.text = root.context.getString(
                    R.string.stat_percentage_format,
                    100
                )
            }

            stepDivider.isVisible = !isLast
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatProcessStepBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
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