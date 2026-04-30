//package ru.faserkraft.client.adapter
//
//import android.graphics.drawable.GradientDrawable
//import android.os.Build
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.annotation.RequiresApi
//import androidx.core.content.ContextCompat
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.RecyclerView
//import ru.faserkraft.client.R
//import ru.faserkraft.client.databinding.ItemStepBinding
//import ru.faserkraft.client.dto.StepDto
//import ru.faserkraft.client.dto.toUiStatus
//import ru.faserkraft.client.utils.formatIsoToUi
//
//class StepsAdapter_OLD(
//    private val onItemClick: (StepUiItem) -> Unit
//) : ListAdapter<StepUiItem, StepsAdapter_OLD.StepVH>(StepDiff()) {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepVH {
//        val binding = ItemStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return StepVH(binding, onItemClick)
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onBindViewHolder(holder: StepVH, position: Int) {
//        holder.bind(getItem(position))
//    }
//
//    class StepVH(
//        private val binding: ItemStepBinding,
//        private val onItemClick: (StepUiItem) -> Unit
//    ) : RecyclerView.ViewHolder(binding.root) {
//
//        @RequiresApi(Build.VERSION_CODES.O)
//        fun bind(step: StepUiItem) {
//            with(binding) {
//                tvStepIndex.text = step.stepDto.stepDefinition.order.toString()
//                tvStepName.text = step.stepDto.stepDefinition.template.name
//                tvExecutor.text = root.context.getString(
//                    R.string.step_executor,
//                    step.stepDto.performedBy?.name ?: "-"
//                )
//
//                val uiStatus = step.stepDto.toUiStatus()
//                val bg = root.background.mutate()
//                if (bg is GradientDrawable) {
//                    bg.setColor(ContextCompat.getColor(root.context, uiStatus.bgColorRes))
//                }
//
//                val completedAtText = formatIsoToUi(step.stepDto.performedAt)
//                tvCompletedAt.text = root.context.getString(
//                    R.string.step_completed_at,
//                    completedAtText
//                )
//
//                if (step.isEditable) {
//                    btnEdit.visibility = View.VISIBLE
//                    btnEdit.setOnClickListener {
//                        onItemClick(step)
//                    }
//                }
//            }
//        }
//
//    }
//
//    class StepDiff : DiffUtil.ItemCallback<StepUiItem>() {
//        override fun areItemsTheSame(oldItem: StepUiItem, newItem: StepUiItem): Boolean =
//            oldItem.stepDto.id == newItem.stepDto.id
//
//        override fun areContentsTheSame(oldItem: StepUiItem, newItem: StepUiItem): Boolean =
//            oldItem == newItem
//    }
//}
//
//data class StepUiItem(
//    val isEditable: Boolean,
//    val stepDto: StepDto,
//)