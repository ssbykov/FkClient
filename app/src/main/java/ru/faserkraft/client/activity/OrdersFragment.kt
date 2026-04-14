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
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.OrderHeader
import ru.faserkraft.client.adapter.OrderListItem
import ru.faserkraft.client.adapter.OrderUiItem
import ru.faserkraft.client.adapter.OrdersAdapter
import ru.faserkraft.client.databinding.FragmentOrdersBinding
import ru.faserkraft.client.dto.ModuleTypeDto
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.viewmodel.ScannerViewModel

class OrdersFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OrdersAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    private var activeDialog: AlertDialog? = null

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

        adapter = OrdersAdapter(
            onItemClick = { item ->
                if (_binding == null) return@OrdersAdapter

                val navController = findNavController()
                // Проверяем, что NavController находится на родительском экране
                if (navController.currentDestination?.id == R.id.storageContainerFragment) {
                    val bundle = Bundle().apply {
                        putInt("orderId", item.orderId)
                    }
                    navController.navigate(
                        R.id.action_storageContainerFragment_to_orderPackagingFragment,
                        bundle
                    )
                }
            },
            onEditClick = { item ->
                if (_binding == null) return@OrdersAdapter

                val navController = findNavController()
                // Вызываем глобальный экшен для перехода в фрагмент редактирования
                val bundle = Bundle().apply {
                    putInt("orderId", item.orderId)
                }
                navController.navigate(R.id.action_global_editOrderFragment, bundle)
            },
            onAddPackagingClick = { item ->
                if (_binding == null) return@OrdersAdapter

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
            },
            onCloseOrderClick = { item ->
                if (_binding == null) return@OrdersAdapter

                // Диалог подтверждения перед закрытием заказа
                AlertDialog.Builder(requireContext())
                    .setTitle("Закрытие заказа")
                    .setMessage("Вы уверены, что хотите закрыть заказ по договору №${item.contractNumber}?")
                    .setPositiveButton("Закрыть") { dialog, _ ->
                        dialog.dismiss()

                        viewModel.closeOrderFromUi(item.orderId)
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        )

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter

        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getOrders()
        }

        // Обработка данных
        viewModel.orders.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            // Маппим данные с бэкенда в UI модели
            val allOrders = list.map { order ->
                val totalRequired = order.items.sumOf { it.quantity }
                val totalPacked = order.packaging.sumOf { pkg -> pkg.products.size }
                val isShipped = order.shipmentDate != null

                val packedProductsByType = order.packaging
                    .flatMap { it.products }
                    .groupingBy { product -> product.process.name }
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
                    isShipped = isShipped,
                    requiredModulesCount = totalRequired,
                    packedModulesCount = totalPacked,
                    packagingCount = order.packaging.size,
                    moduleTypes = moduleTypes
                )
            }

            // Разделяем списки
            val activeOrders = allOrders.filter { !it.isShipped }.sortedByDescending { it.orderId }
            val completedOrders =
                allOrders.filter { it.isShipped }.sortedByDescending { it.orderId }

            // Формируем итоговый список с заголовками
            val finalList = mutableListOf<OrderListItem>()

            if (activeOrders.isNotEmpty()) {
                // Используем R.string для заголовка
                finalList.add(OrderHeader(R.string.order_header_active))
                finalList.addAll(activeOrders)
            }

            if (completedOrders.isNotEmpty()) {
                // Используем R.string для заголовка
                finalList.add(OrderHeader(R.string.order_header_completed))
                finalList.addAll(completedOrders)
            }

            adapter.submitList(finalList)
        }

        // Состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val b = _binding ?: return@observe
            val isLoading = state.isLoading
            b.swipeRefresh.isRefreshing = isLoading
            b.swipeRefresh.isEnabled = !isLoading
        }


        binding.fabAddOrder.setOnClickListener {
            findNavController().navigate(R.id.action_storageContainerFragment_to_newOrderFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getOrders()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activeDialog?.dismiss()
        activeDialog = null

        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }

        binding.rvOrders.adapter = null
        _binding = null
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}