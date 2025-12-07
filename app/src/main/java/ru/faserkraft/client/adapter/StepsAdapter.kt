package ru.faserkraft.client.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStepBinding
import ru.faserkraft.client.dto.StepDto
import ru.faserkraft.client.dto.toUiStatus
import ru.faserkraft.client.utils.formatIsoToUi

class StepsAdapter() : ListAdapter<StepDto, StepsAdapter.StepVH>(StepDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepVH {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepVH(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: StepVH, position: Int) {
        holder.bind(getItem(position))
    }

    class StepVH(
        private val binding: ItemStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(step: StepDto) {
            with(binding) {
                tvStepIndex.text = step.stepDefinition.order.toString()
                tvStepName.text = step.stepDefinition.template.name
                tvExecutor.text = root.context.getString(
                    R.string.step_executor,
                    step.performedBy?.name ?: "-"
                )

                val uiStatus = step.toUiStatus()
                imgStatus.setImageResource(uiStatus.iconRes)
                root.setBackgroundColor(
                    ContextCompat.getColor(root.context, uiStatus.bgColorRes)
                )

                val completedAtText = formatIsoToUi(step.performedAt)
                tvCompletedAt.text = root.context.getString(
                    R.string.step_completed_at,
                    completedAtText
                )
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
