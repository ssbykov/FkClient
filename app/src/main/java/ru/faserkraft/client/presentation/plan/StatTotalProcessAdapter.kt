package ru.faserkraft.client.presentation.plan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemStatTotalProcessRowBinding



class StatTotalProcessAdapter :
    ListAdapter<ProcessTotalUiItem, StatTotalProcessAdapter.VH>(DiffCallback()) {

    class VH(private val binding: ItemStatTotalProcessRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProcessTotalUiItem) {
            binding.tvProcessName.text = item.processName
            binding.tvProcessCount.text = "${item.completedProducts} шт."
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStatTotalProcessRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProcessTotalUiItem>() {
        override fun areItemsTheSame(old: ProcessTotalUiItem, new: ProcessTotalUiItem) =
            old.processId == new.processId

        override fun areContentsTheSame(old: ProcessTotalUiItem, new: ProcessTotalUiItem) =
            old == new
    }
}