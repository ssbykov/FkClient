package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemStatStepsProcessRowBinding

class StatProcessStepsAdapter : ListAdapter<ProcessStepsUiItem, StatProcessStepsAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(
        private val binding: ItemStatStepsProcessRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        // Создаём уникальный адаптер для каждого элемента списка (nested RecyclerView)
        private val stepsAdapter = StatStepAdapter()

        init {
            binding.rvSteps.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = stepsAdapter
                itemAnimator = null
            }
        }

        fun bind(item: ProcessStepsUiItem) {
            binding.tvProcessName.text = item.processName
            // Передаём внутренний список этапов во вложенный адаптер
            stepsAdapter.submitList(item.steps)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStatStepsProcessRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<ProcessStepsUiItem>() {
        override fun areItemsTheSame(oldItem: ProcessStepsUiItem, newItem: ProcessStepsUiItem) =
            oldItem.processId == newItem.processId

        override fun areContentsTheSame(oldItem: ProcessStepsUiItem, newItem: ProcessStepsUiItem) =
            oldItem == newItem
    }
}