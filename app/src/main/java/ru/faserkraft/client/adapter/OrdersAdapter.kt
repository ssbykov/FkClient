package ru.faserkraft.client.adapter

import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemOrderBinding
import ru.faserkraft.client.databinding.ItemOrderHeaderBinding
import ru.faserkraft.client.dto.ModuleTypeDto
import ru.faserkraft.client.utils.formatIsoToUi

sealed class OrderListItem {
    abstract val itemId: String
}

data class OrderHeader(@StringRes val titleResId: Int) : OrderListItem() {
    override val itemId: String = "header_$titleResId"
}

data class OrderUiItem(
    val orderId: Int,
    val contractNumber: String,
    val contractDateStr: String,
    val plannedShipmentDateStr: String,
    val shipmentDateStr: String?,
    val isShipped: Boolean,
    val requiredModulesCount: Int,
    val packedModulesCount: Int,
    val packagingCount: Int,
    val moduleTypes: List<ModuleTypeDto>
) : OrderListItem() {
    override val itemId: String = "order_$orderId"
}

interface OrderActionsListener {
    fun onOrderClick(item: OrderUiItem)
    fun onEditOrderClick(item: OrderUiItem)
    fun onAddPackagingClick(item: OrderUiItem)
    fun onCloseOrderClick(item: OrderUiItem)
    fun onDeleteOrderClick(item: OrderUiItem)
}

class OrdersAdapter(
    private val listener: OrderActionsListener
) : ListAdapter<OrderListItem, RecyclerView.ViewHolder>(OrderDiff()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ORDER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is OrderHeader -> TYPE_HEADER
            is OrderUiItem -> TYPE_ORDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemOrderHeaderBinding.inflate(inflater, parent, false)
                HeaderVH(binding)
            }
            TYPE_ORDER -> {
                val binding = ItemOrderBinding.inflate(inflater, parent, false)
                OrderVH(binding, listener)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is OrderHeader -> (holder as HeaderVH).bind(item)
            is OrderUiItem -> (holder as OrderVH).bind(item)
        }
    }

    class HeaderVH(private val binding: ItemOrderHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: OrderHeader) {
            binding.tvHeaderTitle.setText(item.titleResId)
        }
    }

    class OrderVH(
        private val binding: ItemOrderBinding,
        private val listener: OrderActionsListener
    ) : RecyclerView.ViewHolder(binding.root) {

        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(item: OrderUiItem) = with(binding) {
            val context = itemView.context

            tvContractInfo.text = context.getString(
                R.string.contract_info_format,
                item.contractNumber,
                item.contractDateStr
            )

            tvPlannedShipmentDate.text = context.getString(
                R.string.planned_shipment_format,
                item.plannedShipmentDateStr
            )

            if (item.isShipped && item.shipmentDateStr != null) {
                tvShipmentStatus.visibility = View.VISIBLE
                val shipmentDateStr = formatIsoToUi(item.shipmentDateStr)
                tvShipmentStatus.text = context.getString(
                    R.string.shipment_status_format,
                    shipmentDateStr
                )
                tvShipmentStatus.setTextColor(
                    ContextCompat.getColor(context, R.color.brand_blue)
                )
                btnOrderMenu.visibility = View.GONE
            } else {
                tvShipmentStatus.visibility = View.GONE
                btnOrderMenu.visibility = View.VISIBLE
            }

            val required = item.requiredModulesCount
            val packed = item.packedModulesCount

            pbAssemblyProgress.max = if (required > 0) required else 1
            pbAssemblyProgress.progress = packed

            tvProgressValue.text = context.getString(
                R.string.order_progress_format,
                packed,
                required
            )

            tvPackagingCount.text = context.getString(
                R.string.packaging_count_format,
                item.packagingCount
            )

            val isOrderFullyPacked = required in 1..packed
            if (isOrderFullyPacked && !item.isShipped) {
                tvProgressValue.setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
            } else {
                tvProgressValue.setTextColor(ContextCompat.getColor(context, R.color.black))
            }

            chipGroupTypes.removeAllViews()
            item.moduleTypes.forEach { moduleType ->
                val chip = Chip(chipGroupTypes.context).apply {
                    text = context.getString(
                        R.string.chip_progress_format,
                        moduleType.type,
                        moduleType.packedCount,
                        moduleType.requiredCount
                    )
                    isCloseIconVisible = false
                    isClickable = false

                    val isModuleFullyPacked =
                        moduleType.packedCount >= moduleType.requiredCount &&
                                moduleType.requiredCount > 0

                    if (isModuleFullyPacked) {
                        setTextColor(ContextCompat.getColor(context, R.color.brand_blue))
                        chipStrokeColor = ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.brand_blue)
                        )
                        chipStrokeWidth = 3f
                    } else {
                        setTextColor(ContextCompat.getColor(context, R.color.black))
                        chipStrokeWidth = 1f
                    }
                }
                chipGroupTypes.addView(chip)
            }

            root.setOnClickListener {
                listener.onOrderClick(item)
            }

            btnOrderMenu.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.inflate(R.menu.menu_order_actions)

                if (item.isShipped) {
                    popup.menu.findItem(R.id.action_edit)?.isVisible = false
                    popup.menu.findItem(R.id.action_add_packaging)?.isVisible = false
                    popup.menu.findItem(R.id.action_close_order)?.isVisible = false
                    popup.menu.findItem(R.id.action_delete_order)?.isVisible = false
                }

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit -> {
                            listener.onEditOrderClick(item)
                            true
                        }
                        R.id.action_add_packaging -> {
                            listener.onAddPackagingClick(item)
                            true
                        }
                        R.id.action_close_order -> {
                            listener.onCloseOrderClick(item)
                            true
                        }
                        R.id.action_delete_order -> {
                            listener.onDeleteOrderClick(item)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    class OrderDiff : DiffUtil.ItemCallback<OrderListItem>() {
        override fun areItemsTheSame(oldItem: OrderListItem, newItem: OrderListItem): Boolean =
            oldItem.itemId == newItem.itemId

        override fun areContentsTheSame(oldItem: OrderListItem, newItem: OrderListItem): Boolean =
            oldItem == newItem
    }
}