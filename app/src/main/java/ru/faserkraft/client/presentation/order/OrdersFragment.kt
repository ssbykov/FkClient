package ru.faserkraft.client.presentation.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentOrdersBinding
import ru.faserkraft.client.domain.model.ModuleType
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.convertDate
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

/**
 * Главный экран управления заказами.
 * Поддерживает сквозной поиск по номеру договора / серийному номеру упаковки,
 * Pull-to-refresh и мгновенное открытие деталей без лишних сетевых запросов.
 */
class OrdersFragment : Fragment() {

    private val viewModel: OrderViewModel by activityViewModels()

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OrdersAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    private var allOrders: List<Order> = emptyList()

    // ---------- Lifecycle ----------

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
        setupSearch()
        observeState()
        observeEvents()

        // Загружаем из сети только при первом открытии (когда список в ViewModel пуст).
        // При возврате назад с вложенных фрагментов список уже есть в памяти, экран не мигает лоадером.
        if (viewModel.uiState.value.orders.isEmpty()) {
            viewModel.loadOrders()
        }
    }

    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvOrders.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup & Observe ----------

    private fun setupAdapter() {
        adapter = OrdersAdapter(object : OrderActionsListener {

            override fun onOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                // Мгновенный выбор заказа из памяти UI State (без лишнего сетевого запроса)
                viewModel.selectOrder(item.orderId)
                findNavController().navigateSafely(
                    R.id.action_storageContainerFragment_to_orderPackagingFragment
                )
            }

            override fun onEditOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                viewModel.selectOrder(item.orderId)
                findNavController().navigateSafely(R.id.action_global_editOrderFragment)
            }

            override fun onAddPackagingClick(item: OrderUiItem) {
                if (_binding == null) return
                viewModel.selectOrder(item.orderId)
                findNavController().navigateSafely(
                    R.id.action_storageContainerFragment_to_orderAddPackagingFragment
                )
            }

            override fun onCloseOrderClick(item: OrderUiItem) {
                if (_binding == null) return
                viewModel.requestCloseOrder(item.orderId)
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
            findNavController().navigateSafely(R.id.action_storageContainerFragment_to_newOrderFragment)
        }
        // Ручное обновление только по свайпу вниз
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadOrders()
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            filterOrders(text?.toString().orEmpty())
        }
    }

    private fun filterOrders(query: String) {
        val trimmed = query.trim()

        val filteredOrders = if (trimmed.isEmpty()) {
            allOrders
        } else {
            allOrders.filter { order ->
                val matchContract = order.contractNumber.contains(trimmed, ignoreCase = true)
                val matchPackaging = order.packaging.any { pkg ->
                    pkg.serialNumber.contains(trimmed, ignoreCase = true)
                }
                matchContract || matchPackaging
            }
        }

        val items = mapOrdersToUiItems(filteredOrders)
        adapter.submitList(items) { checkEmpty() }
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            val isSwipeRefreshing = b.swipeRefresh.isRefreshing
            if (isSwipeRefreshing && !state.isLoading) {
                b.swipeRefresh.isRefreshing = false
            }

            b.progressBar.isVisible = state.isLoading && !isSwipeRefreshing

            allOrders = state.orders
            val currentQuery = b.etSearch.text?.toString().orEmpty()
            filterOrders(currentQuery)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is OrderEvent.ShowError -> {
                    showErrorSnackbar(event.message)
                }

                is OrderEvent.CloseOrderDenied -> {
                    if (_binding == null) return@collectFlow
                    val serials = event.invalidPackagingSerials.joinToString(", ")
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Невозможно закрыть заказ")
                        .setMessage(
                            "Заказ нельзя закрыть, так как в следующих упаковках есть изделия со статусом, отличным от NORMAL:\n\n$serials"
                        )
                        .setPositiveButton("ОК", null)
                        .show()
                }

                is OrderEvent.ConfirmCloseOrder -> {
                    if (_binding == null) return@collectFlow
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Закрытие заказа")
                        .setMessage("Вы уверены, что хотите закрыть заказ по договору №${event.contractNumber}?")
                        .setPositiveButton("Закрыть") { _, _ ->
                            viewModel.closeOrder(event.orderId)
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                }

                else -> Unit
            }
        }
    }

    // ---------- Helpers ----------

    private fun mapOrdersToUiItems(
        orders: List<Order>
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
                    ModuleType(
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
        val isEmpty = adapter.itemCount == 0 && !b.progressBar.isVisible
        b.tvEmpty.isVisible = isEmpty
        b.rvOrders.isVisible = !isEmpty
    }
}
