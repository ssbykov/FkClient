package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemStepBinding
import ru.faserkraft.client.dto.StepDto
import ru.faserkraft.client.utils.formatIsoToUi

class StepsAdapter() : ListAdapter<StepDto, StepsAdapter.StepVH>(StepDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepVH {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepVH(binding)
    }

    override fun onBindViewHolder(holder: StepVH, position: Int) {
        holder.bind(getItem(position))
    }

    class StepVH(
        private val binding: ItemStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: StepDto) {
            with(binding) {
                tvStepIndex.text = step.stepDefinition.order.toString()
                tvStepName.text = step.stepDefinition.template.name
                tvExecutor.text = step.performedBy.let { "Выполнил: ${it?.name ?: "-"}" }
                tvCompletedAt.text = "Выполнено: " + (step.performedAt?.let(::formatIsoToUi) ?: "-")

            }
        }
    }

    class StepDiff : DiffUtil.ItemCallback<StepDto>() {
        override fun areItemsTheSame(oldItem: StepDto, newItem: StepDto): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: StepDto, newItem: StepDto): Boolean =
            oldItem == newItem
    }
}
