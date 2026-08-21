package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStatStepRowBinding
import java.util.Locale


class StatStepAdapter : ListAdapter<StepCountUiItem, StatStepAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemStatStepRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StepCountUiItem) = with(binding) {
            tvStepName.text = item.stepName

            val avgText = String.format(Locale.getDefault(), "%.1f", item.dailyAverage)

            if (item.planCount != null && item.completionPercentage != null) {
                val percentageInt = item.completionPercentage.toInt()
                tvPercentage.text = root.context.getString(R.string.stat_percentage_format, percentageInt)
                tvStepDetails.text = root.context.getString(
                    R.string.stat_step_details_with_plan_format,
                    item.count,
                    item.planCount,
                    avgText
                )
                progressStep.isVisible = true
                progressStep.setProgressCompat(percentageInt.coerceIn(0, 100), true)
            } else {
                tvPercentage.text = root.context.getString(R.string.count_units_format, item.count)
                tvStepDetails.text = root.context.getString(
                    R.string.stat_step_details_avg_only_format,
                    avgText
                )
                progressStep.isVisible = false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatStepRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<StepCountUiItem>() {
        override fun areItemsTheSame(old: StepCountUiItem, new: StepCountUiItem) =
            old.stepDefinitionId == new.stepDefinitionId

        override fun areContentsTheSame(old: StepCountUiItem, new: StepCountUiItem) =
            old == new
    }
}