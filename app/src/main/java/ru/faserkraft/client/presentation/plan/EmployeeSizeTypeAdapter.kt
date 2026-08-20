package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemStatEmployeeProcessRowBinding

class EmployeeSizeTypeAdapter :
    ListAdapter<EmployeeSizeTypeUiItem, EmployeeSizeTypeAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(
        private val binding: ItemStatEmployeeProcessRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val stepsAdapter = StatStepAdapter()

        init {
            binding.rvSteps.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = stepsAdapter
                itemAnimator = null
            }
        }

        fun bind(item: EmployeeSizeTypeUiItem) = with(binding) {
            tvProcessName.text = root.context.getString(
                R.string.size_type_format,
                item.sizeTypeName
            )
            tvProcessTotal.text = root.context.getString(
                R.string.grand_total_format, item.totalCompleted
            )
            stepsAdapter.submitList(item.steps)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatEmployeeProcessRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<EmployeeSizeTypeUiItem>() {
        override fun areItemsTheSame(
            old: EmployeeSizeTypeUiItem,
            new: EmployeeSizeTypeUiItem
        ) = if (old.sizeTypeId != null && new.sizeTypeId != null) {
            old.sizeTypeId == new.sizeTypeId
        } else {
            old.sizeTypeName == new.sizeTypeName
        }

        override fun areContentsTheSame(
            old: EmployeeSizeTypeUiItem,
            new: EmployeeSizeTypeUiItem
        ) = old == new
    }
}