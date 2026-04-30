package ru.faserkraft.client.presentation.product

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStepBinding
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.utils.formatIsoToUi

class StepsAdapter(
    private val onItemClick: (StepUiItem) -> Unit,
) : androidx.recyclerview.widget.ListAdapter<StepUiItem, StepsAdapter.StepVH>(StepDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepVH {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepVH(binding, onItemClick)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: StepVH, position: Int) {
        holder.bind(getItem(position))
    }

    class StepVH(
        private val binding: ItemStepBinding,
        private val onItemClick: (StepUiItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: StepUiItem) = with(binding) {
            val step = item.step
            tvStepIndex.text = step.definition.order.toString()
            tvStepName.text = step.definition.name
            tvExecutor.text = root.context.getString(
                R.string.step_executor,
                step.performedBy?.name ?: "-"
            )

            val uiStatus = step.toUiStatus()
            val bg = root.background.mutate()
            if (bg is GradientDrawable) {
                bg.setColor(ContextCompat.getColor(root.context, uiStatus.bgColorRes))
            }

            tvCompletedAt.text = root.context.getString(
                R.string.step_completed_at,
                formatIsoToUi(step.performedAt)
            )

            if (item.isEditable) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener { onItemClick(item) }
            } else {
                btnEdit.visibility = View.GONE
            }
        }
    }

    class StepDiff : DiffUtil.ItemCallback<StepUiItem>() {
        override fun areItemsTheSame(oldItem: StepUiItem, newItem: StepUiItem) =
            oldItem.step.id == newItem.step.id

        override fun areContentsTheSame(oldItem: StepUiItem, newItem: StepUiItem) =
            oldItem == newItem
    }
}

data class StepUiItem(
    val isEditable: Boolean,
    val step: Step,
)