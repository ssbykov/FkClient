package ru.faserkraft.client.presentation.product.detail

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStepBinding
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.utils.converter.formatIsoToUi

enum class StepAction {
    CHANGE_PERFORMER,
    RESET_STEP,
}

class StepsAdapter(
    private val onActionClick: (StepUiItem, StepAction) -> Unit,
) : ListAdapter<StepUiItem, StepsAdapter.StepVH>(StepDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepVH {
        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StepVH(binding, onActionClick)
    }

    override fun onBindViewHolder(holder: StepVH, position: Int) {
        holder.bind(getItem(position))
    }

    class StepVH(
        private val binding: ItemStepBinding,
        private val onActionClick: (StepUiItem, StepAction) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

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
                btnMenu.visibility = View.VISIBLE
                btnMenu.setOnClickListener { view ->
                    showPopupMenu(view, item)
                }
            } else {
                btnMenu.visibility = View.GONE
            }
        }

        private fun showPopupMenu(view: View, item: StepUiItem) {
            val popup = PopupMenu(view.context, view)
            popup.inflate(R.menu.menu_step_actions)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_change_performer -> {
                        onActionClick(item, StepAction.CHANGE_PERFORMER)
                        true
                    }
                    R.id.action_reset_step -> {
                        onActionClick(item, StepAction.RESET_STEP)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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