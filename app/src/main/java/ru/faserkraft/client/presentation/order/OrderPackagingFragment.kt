package ru.faserkraft.client.presentation.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ModuleTypeUi
import ru.faserkraft.client.adapter.PackagingListAdapter
import ru.faserkraft.client.adapter.PackagingListUiItem
import ru.faserkraft.client.databinding.FragmentOrderPackagingBinding
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.presentation.packaging.PackagingEvent
import ru.faserkraft.client.presentation.packaging.PackagingViewModel
import ru.faserkraft.client.utils.showErrorSnackbar

class OrderPackagingFragment : Fragment() {

    private val orderViewModel: OrderViewModel by activityViewModels()
    private val packagingViewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentOrderPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingListAdapter
    private var itemTouchHelper: ItemTouchHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        observeState()
        observeOrderEvents()
        observePackagingEvents()
    }

    private fun setupAdapter() {
        adapter = PackagingListAdapter { item ->
            packagingViewModel.loadPackaging(item.serialNumber)
        }
    }

    private fun setupRecyclerView() {
        binding.rvPackagingStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingStats.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                orderViewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect
                    val order = state.currentOrder
                    if (order != null) {
                        renderOrder(order)
                    } else {
                        b.tvEmptyStorage.visibility = View.VISIBLE
                        b.rvPackagingStats.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun observeOrderEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                orderViewModel.events.collect { event ->
                    when (event) {
                        is OrderEvent.ShowError -> showErrorSnackbar(event.message)
                        OrderEvent.OrderClosed,
                        OrderEvent.OrderDeleted,
                        OrderEvent.OrderUpdated,
                        OrderEvent.OrderCreated,
                        OrderEvent.PackagingAdded -> Unit
                    }
                }
            }
        }
    }

    private fun observePackagingEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                packagingViewModel.events.collect { event ->
                    when (event) {
                        is PackagingEvent.NavigateToPackaging ->
                            findNavController().navigate(
                                R.id.action_orderPackagingFragment_to_packagingFragment
                            )
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun renderOrder(order: Order) {
        val b = _binding ?: return

        b.tvStatsTitle.text = getString(R.string.packaging_title_format, order.contractNumber)

        val uiItems = order.packaging.map { box ->
            val groups = box.products.groupBy { it.process.name }
            PackagingListUiItem(
                id = box.id,
                serialNumber = box.serialNumber,
                totalCount = box.products.size,
                types = groups.map { (name, list) -> ModuleTypeUi(name = name, count = list.size) }
            )
        }

        adapter.submitList(uiItems)

        val isEmpty = uiItems.isEmpty()
        b.tvEmptyStorage.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvPackagingStats.visibility = if (isEmpty) View.GONE else View.VISIBLE

        updateSwipeHelper(order)
    }

    private fun updateSwipeHelper(order: Order) {
        val isClosed = order.shipmentDate != null
        if (!isClosed && itemTouchHelper == null) {
            itemTouchHelper = ItemTouchHelper(buildSwipeCallback(order.id))
                .also { it.attachToRecyclerView(binding.rvPackagingStats) }
        } else if (isClosed && itemTouchHelper != null) {
            itemTouchHelper?.attachToRecyclerView(null)
            itemTouchHelper = null
        }
    }

    private fun buildSwipeCallback(orderId: Int) =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val item = adapter.currentList[position]

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Отвязка упаковки")
                    .setMessage("Вы уверены, что хотите отвязать упаковку ${item.serialNumber} от этого заказа?")
                    .setPositiveButton("Да") { _, _ ->
                        orderViewModel.detachPackagingFromOrder(orderId, listOf(item.id))
                    }
                    .setNegativeButton("Нет") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        orderViewModel.clearCurrentOrder()
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        binding.rvPackagingStats.adapter = null
        _binding = null
    }
}