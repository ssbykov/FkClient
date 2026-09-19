package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStatEmployeeCardBinding
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class EmployeeStatAdapter(
    private val onEmployeeClick: ((EmployeeStatsUiItem) -> Unit)? = null
) : ListAdapter<EmployeeStatsUiItem, EmployeeStatAdapter.ViewHolder>(DiffCallback) {

    private val expandedEmployeeIds = mutableSetOf<Any>()

    inner class ViewHolder(
        private val binding: ItemStatEmployeeCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val sizeTypeAdapter = EmployeeSizeTypeAdapter()

        init {
            binding.rvEmployeeProcesses.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = sizeTypeAdapter
                itemAnimator = null
            }
        }

        fun bind(item: EmployeeStatsUiItem) = with(binding) {
            // Разделяем Фамилию и Имя на две отдельные строки
            val nameParts = item.employeeName.trim().split("\\s+".toRegex())
            if (nameParts.size >= 2) {
                tvEmployeeLastName.text = nameParts[0]
                tvEmployeeFirstName.text = nameParts.drop(1).joinToString(" ")
                tvEmployeeFirstName.isVisible = true
            } else {
                tvEmployeeLastName.text = item.employeeName
                tvEmployeeFirstName.isVisible = false
            }

            tvWorkingDays.text = root.context.getString(
                R.string.stat_working_days_format,
                item.workingDays
            )
            tvEmployeeEarned.text = formatCurrency(item.totalEarned)

            // Аванс за 1-15 число
            firstHalfContainer.isVisible = item.showFirstHalf
            if (item.showFirstHalf) {
                tvEmployeeFirstHalfEarned.text = formatCurrency(item.firstHalfEarned)
            }

            sizeTypeAdapter.submitList(item.sizeTypes)

            val isExpanded = expandedEmployeeIds.contains(item.employeeId)
            expandableContainer.isVisible = isExpanded
            ivExpand.rotation = if (isExpanded) 180f else 0f

            val toggleExpand: () -> Unit = {
                val currentlyExpanded = expandedEmployeeIds.contains(item.employeeId)
                if (currentlyExpanded) {
                    expandedEmployeeIds.remove(item.employeeId)
                } else {
                    expandedEmployeeIds.add(item.employeeId)
                }

                val willBeExpanded = !currentlyExpanded
                expandableContainer.isVisible = willBeExpanded
                ivExpand.animate().rotation(if (willBeExpanded) 180f else 0f).setDuration(200).start()

                onEmployeeClick?.invoke(item)
            }

            headerEmployee.setOnClickListener { toggleExpand() }
            root.setOnClickListener { toggleExpand() }
        }

        private fun formatCurrency(amount: BigDecimal): String {
            val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru")).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return binding.root.context.getString(
                R.string.stat_earned_format,
                format.format(amount)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatEmployeeCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<EmployeeStatsUiItem>() {
        override fun areItemsTheSame(
            old: EmployeeStatsUiItem,
            new: EmployeeStatsUiItem
        ) = old.employeeId == new.employeeId

        override fun areContentsTheSame(
            old: EmployeeStatsUiItem,
            new: EmployeeStatsUiItem
        ) = old == new
    }
}
