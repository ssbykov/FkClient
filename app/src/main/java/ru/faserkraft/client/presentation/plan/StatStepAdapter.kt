package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStatStepRowBinding


class StatStepAdapter : ListAdapter<StepCountUiItem, StatStepAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(
        private val binding: ItemStatStepRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StepCountUiItem) = with(binding) {
            tvStepName.text = item.stepName
            tvStepCount.text = root.context.getString(R.string.step_count_format, item.count)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatStepRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<StepCountUiItem>() {
        override fun areItemsTheSame(oldItem: StepCountUiItem, newItem: StepCountUiItem) =
            oldItem.stepDefinitionId == newItem.stepDefinitionId

        override fun areContentsTheSame(oldItem: StepCountUiItem, newItem: StepCountUiItem) =
            oldItem == newItem
    }
}