package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.OrderActionsListener
import ru.faserkraft.client.adapter.OrderHeader
import ru.faserkraft.client.adapter.OrderListItem
import ru.faserkraft.client.adapter.OrderUiItem
import ru.faserkraft.client.adapter.OrdersAdapter
import ru.faserkraft.client.databinding.FragmentOrdersBinding
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.model.ModuleTypeDto
import ru.faserkraft.client.ui.base.BaseFragment
import ru.faserkraft.client.ui.common.SharedUiViewModel
import ru.faserkraft.client.ui.order.OrderViewModel
import ru.faserkraft.client.utils.collectEventsIn
import ru.faserkraft.client.utils.collectIn
import ru.faserkraft.client.utils.convertDate

/**
 * МИГРИРОВАННЫЙ OrdersFragment с новой архитектурой
 *
 * НОВОЕ: Использует OrderViewModel вместо ScannerViewModel
 * НОВОЕ: Использует StateFlow вместо LiveData
 * НОВОЕ: Работает с Domain Models вместо DTOs
 * НОВОЕ: Наследуется от BaseFragment для общей логики
 */
@AndroidEntryPoint
class OrdersFragment : BaseFragment<OrderViewModel>() {

    override val viewModel: OrderViewModel by activityViewModels()
    private val sharedUiViewModel: SharedUiViewModel by activityViewModels()

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

        setupRecyclerView()
        observeViewModel()
        observeSharedViewModel()
        setupClickListeners()

        // Загружаем заказы при старте
        viewModel.getOrders()
    }

    /**
     * Настраиваем RecyclerView и адаптер
     */
    private fun setupRecyclerView() {
        adapter = OrdersAdapter(object : OrderActionsListener {
            override fun onOrderClick(item: OrderUiItem) {
                if (_binding == null) return

                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.storageContainerFragment) {
                    val bundle = Bundle().apply {
                        putInt("orderId", item.orderId)
                    }
                    navController.navigate(
                        R.id.action_storageContainerFragment_to_orderPackagingFragment,
                        bundle
                    )
                }
            }

            override fun onEditOrderClick(item: OrderUiItem) {
                if (_binding == null) return

                val bundle = Bundle().apply {
                    putInt("orderId", item.orderId)
                }
                findNavController().navigate(R.id.action_global_editOrderFragment, bundle)
            }

            override fun onAddPackagingClick(item: OrderUiItem) {
                if (_binding == null) return

                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.storageContainerFragment) {
                    val bundle = Bundle().apply {
                        putInt("orderId", item.orderId)
                    }
                    navController.navigate(
                        R.id.action_storageContainerFragment_to_orderAddPackagingFragment,
                        bundle
                    )
                }
            }

            override fun onCloseOrderClick(item: OrderUiItem) {
                showConfirmDialog(
                    "Закрытие заказа",
                    "Вы уверены, что хотите закрыть заказ по договору №${item.contractNumber}?",

                    onConfirm = {
                        viewModel.closeOrder(item.orderId)
                    }
                )
            }

            override fun onDeleteOrderClick(item: OrderUiItem) {
                showConfirmDialog(
                    "Удаление заказа",
                    "Вы уверены, что хотите удалить заказ по договору №${item.contractNumber}?",
                    onConfirm = {
                        // TODO: Implement delete order in new architecture
                        showDialog("Удаление заказа пока не реализовано в новой архитектуре")
                    }
                )
            }
        })

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter

        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()
    }

    /**
     * Наблюдать за состоянием заказов
     */
    private fun observeViewModel() {
        // Состояние списка заказов
        viewModel.ordersState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> {
                    hideLoadingIndicator()
                }
                is UiState.Loading -> {
                    showLoadingIndicator()
                }
                is UiState.Success -> {
                    hideLoadingIndicator()
                    updateOrdersList(state.data)
                }
                is UiState.Error -> {
                    hideLoadingIndicator()
                    showErrorDialog(state.exception)
                }
            }
        }

        // Состояние действий (создание, закрытие заказа)
        viewModel.actionState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is OrderViewModel.ActionState.Idle -> {
                    // Ничего не делаем
                }
                is OrderViewModel.ActionState.InProgress -> {
                    showLoadingIndicator()
                }
                is OrderViewModel.ActionState.Success -> {
                    hideLoadingIndicator()
                    showDialog(state.message)
                    // Обновляем список после успешного действия
                    viewModel.getOrders()
                }
                is OrderViewModel.ActionState.Error -> {
                    hideLoadingIndicator()
                    showErrorDialog(state.exception)
                }
            }
        }
    }

    /**
     * Наблюдать за общими событиями
     */
    private fun observeSharedViewModel() {
        sharedUiViewModel.errorMessages.collectEventsIn(viewLifecycleOwner) { message: String ->
            showDialog(message)
        }
    }

    /**
     * Обновить список заказов
     */
    private fun updateOrdersList(orders: List<Order>) {
        if (orders.isEmpty()) {
            adapter.submitList(emptyList())
            checkEmpty()
            return
        }

        // Маппим данные с бэкенда в UI модели
        val allOrders = orders.map { order ->
            val totalRequired = order.items.sumOf { it.quantity }
            val totalPacked = order.packaging.sumOf { pkg -> pkg.products.size }

            val packedProductsByType = order.packaging
                .flatMap { it.products }
                .groupingBy { product -> "Process ${product.processId}" } // TODO: Get actual process name
                .eachCount()

            val moduleTypes = order.items.map { item ->
                val typeName = item.workProcess.name
                val required = item.quantity
                val packed = packedProductsByType[typeName] ?: 0

                ModuleTypeDto(
                    type = typeName,
                    requiredCount = required,
                    packedCount = packed
                )
            }

            OrderUiItem(
                orderId = order.id,
                contractNumber = order.contractNumber,
                contractDateStr = convertDate(order.contractDate),
                plannedShipmentDateStr = convertDate(order.plannedShipmentDate),
                shipmentDateStr = order.shipmentDate?.let { convertDate(it) },
                isShipped = order.shipmentDate != null,
                requiredModulesCount = totalRequired,
                packedModulesCount = totalPacked,
                packagingCount = order.packaging.size,
                moduleTypes = moduleTypes
            )
        }

        // Разделяем списки
        val activeOrders = allOrders.filter { !it.isShipped }.sortedByDescending { it.orderId }
        val completedOrders = allOrders.filter { it.isShipped }.sortedByDescending { it.orderId }

        // Формируем итоговый список с заголовками
        val finalList = mutableListOf<OrderListItem>()

        if (activeOrders.isNotEmpty()) {
            finalList.add(OrderHeader(R.string.order_header_active))
            finalList.addAll(activeOrders)
        }

        if (completedOrders.isNotEmpty()) {
            finalList.add(OrderHeader(R.string.order_header_completed))
            finalList.addAll(completedOrders)
        }

        // Передаем callback в submitList для надежного обновления пустого состояния
        adapter.submitList(finalList) {
            checkEmpty()
        }
    }

    /**
     * Настроить обработчики клика
     */
    private fun setupClickListeners() {
        binding.fabAddOrder.setOnClickListener {
            findNavController().navigate(R.id.action_storageContainerFragment_to_newOrderFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.getOrders()
        }
    }

    private fun showLoadingIndicator() {
        binding.swipeRefresh.isRefreshing = true
    }

    private fun hideLoadingIndicator() {
        binding.swipeRefresh.isRefreshing = false
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