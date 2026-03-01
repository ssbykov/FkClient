package ru.faserkraft.client.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemEmployeeHeaderBinding
import ru.faserkraft.client.databinding.ItemPlanBinding
import ru.faserkraft.client.dto.EmployeePlanDto

class PlansAdapter(private val onStepClick: (EmployeePlanDto) -> Unit) :
    ListAdapter<EmployeePlanUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_STEP = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is EmployeePlanUiItem.Header -> TYPE_HEADER
            is EmployeePlanUiItem.Step -> TYPE_STEP
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemEmployeeHeaderBinding.inflate(inflater, parent, false)
                HeaderVH(binding)
            }

            else -> {
                val binding = ItemPlanBinding.inflate(inflater, parent, false)
                StepVH(onStepClick, binding)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is EmployeePlanUiItem.Header -> (holder as HeaderVH).bind(item)
            is EmployeePlanUiItem.Step -> (holder as StepVH).bind(item.plan)
        }
    }

    class HeaderVH(
        private val binding: ItemEmployeeHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EmployeePlanUiItem.Header) {
            binding.tvEmployeeName.text = item.employeeName
        }
    }

    class StepVH(
        private val onStepClick: (EmployeePlanDto) -> Unit,
        private val binding: ItemPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(plan: EmployeePlanDto) = with(binding) {
            tvWorkProcess.text = plan.workProcess
            tvStepName.text = plan.stepDefinition.template.name
            tvPlanValue.text = plan.plannedQuantity.toString()
            tvDoneValue.text = plan.actualQuantity.toString()

            binding.root.setOnClickListener {
                onStepClick(plan)
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<EmployeePlanUiItem>() {
        override fun areItemsTheSame(
            oldItem: EmployeePlanUiItem,
            newItem: EmployeePlanUiItem
        ): Boolean =
            when {
                oldItem is EmployeePlanUiItem.Header && newItem is EmployeePlanUiItem.Header ->
                    oldItem.employeeName == newItem.employeeName

                oldItem is EmployeePlanUiItem.Step && newItem is EmployeePlanUiItem.Step ->
                    oldItem.plan.id == newItem.plan.id

                else -> false
            }

        override fun areContentsTheSame(
            oldItem: EmployeePlanUiItem,
            newItem: EmployeePlanUiItem
        ): Boolean =
            oldItem == newItem
    }
}

sealed class EmployeePlanUiItem {
    data class Header(val employeeName: String) : EmployeePlanUiItem()
    data class Step(val plan: EmployeePlanDto) : EmployeePlanUiItem()
}
