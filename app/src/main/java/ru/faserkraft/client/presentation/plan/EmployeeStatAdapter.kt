package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
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
            tvEmployeeName.text = item.employeeName
            tvWorkingDays.text = root.context.getString(R.string.stat_working_days_format, item.workingDays)
            tvEmployeeTotal.text = root.context.getString(
                R.string.grand_total_format, item.totalCompleted
            )
            tvEmployeeEarned.text = root.context.getString(
                R.string.stat_earned_format,
                formatCurrency(item.totalEarned)
            )
            sizeTypeAdapter.submitList(item.sizeTypes)

            root.setOnClickListener {
                onEmployeeClick?.invoke(item)
            }
        }

        private fun formatCurrency(amount: BigDecimal): String {
            val format = NumberFormat.getNumberInstance(Locale.forLanguageTag("ru")).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return format.format(amount)
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
