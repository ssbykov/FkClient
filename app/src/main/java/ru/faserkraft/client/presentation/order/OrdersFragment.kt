package ru.faserkraft.client.presentation.order

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentOrdersBinding
import ru.faserkraft.client.dto.ModuleTypeDto
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.utils.showErrorSnackbar

class OrdersFragment : Fragment() {

    private val viewModel: OrderViewModel by activityViewModels()

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OrdersAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        setupListeners()
        observeState()
        observeEvents()

        viewModel.loadOrders()
    }

    private fun setupAdapter() {
        adapter = OrdersAdapter(object : OrderActionsListener {

            override fun onOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                viewModel.loadOrder(item.orderId)
                findNavController().navigate(
                    R.id.action_storageContainerFragment_to_orderPackagingFragment
                )
            }

            override fun onEditOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                viewModel.loadOrder(item.orderId)
                findNavController().navigate(R.id.action_global_editOrderFragment)
            }

            override fun onAddPackagingClick(item: OrderUiItem) {
                if (_binding == null) return
                viewModel.loadOrder(item.orderId)
                findNavController().navigate(
                    R.id.action_storageContainerFragment_to_orderAddPackagingFragment
                )
            }

            override fun onCloseOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Закрытие заказа")
                    .setMessage("Вы уверены, что хотите закрыть заказ по договору №${item.contractNumber}?")
                    .setPositiveButton("Закрыть") { _, _ -> viewModel.closeOrder(item.orderId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            }

            override fun onDeleteOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удаление заказа")
                    .setMessage("Вы уверены, что хотите удалить заказ по договору №${item.contractNumber}?")
                    .setPositiveButton("Удалить") { _, _ -> viewModel.deleteOrder(item.orderId) }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        })
    }

    private fun setupRecyclerView() {
        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddOrder.setOnClickListener {
            findNavController().navigate(R.id.action_storageContainerFragment_to_newOrderFragment)
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadOrders()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    binding.swipeRefresh.isEnabled = !state.isLoading

                    val items = mapOrdersToUiItems(state.orders)
                    adapter.submitList(items) { checkEmpty() }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is OrderEvent.ShowError -> {
                            Log.e("OrdersFragment", "Order error: ${event.message}")
                            showErrorSnackbar(event.message)
                        }
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

    private fun mapOrdersToUiItems(
        orders: List<ru.faserkraft.client.domain.model.Order>
    ): List<OrderListItem> {
        val allOrders = orders.map { order ->
            val packedByType = order.packaging
                .flatMap { it.products }
                .groupingBy { it.process.name }
                .eachCount()

            OrderUiItem(
                orderId = order.id,
                contractNumber = order.contractNumber,
                contractDateStr = convertDate(order.contractDate),
                plannedShipmentDateStr = convertDate(order.plannedShipmentDate),
                shipmentDateStr = order.shipmentDate?.let { convertDate(it) },
                isShipped = order.shipmentDate != null,
                requiredModulesCount = order.items.sumOf { it.quantity },
                packedModulesCount = order.packaging.sumOf { it.products.size },
                packagingCount = order.packaging.size,
                moduleTypes = order.items.map { item ->
                    ModuleTypeDto(
                        type = item.workProcess.name,
                        requiredCount = item.quantity,
                        packedCount = packedByType[item.workProcess.name] ?: 0
                    )
                }
            )
        }

        val active = allOrders.filter { !it.isShipped }.sortedByDescending { it.orderId }
        val completed = allOrders.filter { it.isShipped }.sortedByDescending { it.orderId }

        return buildList {
            if (active.isNotEmpty()) {
                add(OrderHeader(R.string.order_header_active))
                addAll(active)
            }
            if (completed.isNotEmpty()) {
                add(OrderHeader(R.string.order_header_completed))
                addAll(completed)
            }
        }
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvOrders.adapter = null
        _binding = null
    }
}