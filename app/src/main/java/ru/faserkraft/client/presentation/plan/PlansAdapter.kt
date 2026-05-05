package ru.faserkraft.client.presentation.plan

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemEmployeeHeaderBinding
import ru.faserkraft.client.databinding.ItemPlanBinding
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep

class PlansAdapter(
    private var canEdit: Boolean = false,
    private val onEditPlanClick: (DailyPlan, DailyPlanStep) -> Unit = { _, _ -> },
    private val onEmployeeProductsClick: (DailyPlan, DailyPlanStep) -> Unit = { _, _ -> },
) : ListAdapter<EmployeePlanUiItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_STEP = 1
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is EmployeePlanUiItem.Header -> TYPE_HEADER
            is EmployeePlanUiItem.Step -> TYPE_STEP
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(
                ItemEmployeeHeaderBinding.inflate(inflater, parent, false)
            )

            TYPE_STEP -> StepVH(
                binding = ItemPlanBinding.inflate(inflater, parent, false),
                onEditPlanClick = onEditPlanClick,
                onEmployeeProductsClick = onEmployeeProductsClick,
            )

            else -> error("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is EmployeePlanUiItem.Header -> (holder as HeaderVH).bind(item)
            is EmployeePlanUiItem.Step -> (holder as StepVH).bind(item.plan, item.step, canEdit)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCanEdit(value: Boolean) {
        if (canEdit == value) return
        canEdit = value
        notifyDataSetChanged()
    }

    fun submitPlans(plans: List<DailyPlan>) {
        val items = plans
            .groupBy { it.employee.id }
            .values
            .flatMap { employeePlans ->
                val firstPlan = employeePlans.firstOrNull() ?: return@flatMap emptyList()

                val stepItems = employeePlans.flatMap { plan ->
                    plan.steps.map { step ->
                        EmployeePlanUiItem.Step(
                            plan = plan,
                            step = step
                        )
                    }
                }

                if (stepItems.isEmpty()) {
                    emptyList()
                } else {
                    buildList {
                        add(
                            EmployeePlanUiItem.Header(
                                employeeId = firstPlan.employee.id,
                                employeeName = firstPlan.employee.name
                            )
                        )
                        addAll(stepItems)
                    }
                }
            }

        submitList(items)
    }

    class HeaderVH(
        private val binding: ItemEmployeeHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EmployeePlanUiItem.Header) {
            binding.tvEmployeeName.text = item.employeeName
        }
    }

    class StepVH(
        private val binding: ItemPlanBinding,
        private val onEditPlanClick: (DailyPlan, DailyPlanStep) -> Unit,
        private val onEmployeeProductsClick: (DailyPlan, DailyPlanStep) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: DailyPlan, step: DailyPlanStep, canEdit: Boolean) = with(binding) {
            tvWorkProcess.text = step.workProcess
            tvStepName.text = step.stepDefinition.name
            tvPlanValue.text = step.plannedQuantity.toString()
            tvDoneValue.text = step.actualQuantity.toString()

            if (canEdit) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener { onEditPlanClick(plan, step) }
            } else {
                btnEdit.visibility = View.GONE
                btnEdit.setOnClickListener(null)
            }

            root.setOnClickListener { onEmployeeProductsClick(plan, step) }
        }
    }

    class Diff : DiffUtil.ItemCallback<EmployeePlanUiItem>() {

        override fun areItemsTheSame(
            oldItem: EmployeePlanUiItem,
            newItem: EmployeePlanUiItem,
        ): Boolean = when {
            oldItem is EmployeePlanUiItem.Header && newItem is EmployeePlanUiItem.Header ->
                oldItem.employeeId == newItem.employeeId

            oldItem is EmployeePlanUiItem.Step && newItem is EmployeePlanUiItem.Step ->
                oldItem.step.id == newItem.step.id

            else -> false
        }

        override fun areContentsTheSame(
            oldItem: EmployeePlanUiItem,
            newItem: EmployeePlanUiItem,
        ): Boolean = oldItem == newItem
    }
}

sealed class EmployeePlanUiItem {

    data class Header(
        val employeeId: Int,
        val employeeName: String,
    ) : EmployeePlanUiItem()

    data class Step(
        val plan: DailyPlan,
        val step: DailyPlanStep,
    ) : EmployeePlanUiItem()
}