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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentOrderPackagingBinding
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.presentation.common.adapter.PackagingListAdapter
import ru.faserkraft.client.presentation.common.adapter.PackagingListUiItem
import ru.faserkraft.client.presentation.packaging.PackagingEvent
import ru.faserkraft.client.presentation.packaging.PackagingViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatPackagingDate
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

/**
 * Фрагмент просмотра списка упаковок, привязанных к конкретному заказу.
 * Поддерживает поиск упаковки по номеру, отвязку упаковки свайпом вправо и мгновенный переход в карточку упаковки из памяти.
 */
class OrderPackagingFragment : Fragment() {

    private val orderViewModel: OrderViewModel by activityViewModels()
    private val packagingViewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentOrderPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingListAdapter
    private var itemTouchHelper: ItemTouchHelper? = null

    // Полный список упаковок текущего заказа для мгновенной локальной фильтрации
    private var allPackagingUiItems: List<PackagingListUiItem> = emptyList()

    // ---------- Lifecycle ----------

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
        setupSearch()
        observeState()
        observePackagingState()
        observeOrderEvents()
        observePackagingEvents()
    }

    override fun onDestroyView() {
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        binding.rvPackagingStats.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup & Observe ----------

    private fun setupAdapter() {
        adapter = PackagingListAdapter { item ->
            if (_binding == null) return@PackagingListAdapter

            // 1. Ищем готовую упаковку в памяти заказа текущего OrderViewModel
            val currentOrder = orderViewModel.uiState.value.currentOrder
            val boxInMemory = currentOrder?.packaging?.find { it.id == item.id || it.serialNumber == item.serialNumber }

            if (boxInMemory != null) {
                // МГНОВЕННО: передаем готовый объект в PackagingViewModel и переходим без похода в сеть!
                packagingViewModel.selectPackaging(boxInMemory)
                val action = OrderPackagingFragmentDirections
                    .actionOrderPackagingFragmentToPackagingFragment(null)
                findNavController().navigateSafely(action)
            } else {
                // Резервный сетевой запрос, если вдруг не нашли в памяти
                packagingViewModel.loadPackaging(item.serialNumber)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvPackagingStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingStats.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            filterPackaging(text?.toString().orEmpty())
        }
    }

    private fun filterPackaging(query: String) {
        val b = _binding ?: return
        val trimmed = query.trim()

        val filtered = if (trimmed.isEmpty()) {
            allPackagingUiItems
        } else {
            allPackagingUiItems.filter { item ->
                item.serialNumber.contains(trimmed, ignoreCase = true)
            }
        }

        adapter.submitList(filtered) {
            val isEmpty = filtered.isEmpty() && !b.progressBar.isVisible
            b.tvEmptyStorage.isVisible = isEmpty
            b.rvPackagingStats.isVisible = !isEmpty
        }
    }

    private fun observeState() {
        collectFlow(orderViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            updateLoadingState()

            val order = state.currentOrder
            if (order != null) {
                renderOrder(order)
            } else {
                val isLoading = b.progressBar.isVisible
                b.tvEmptyStorage.isVisible = !isLoading
                b.rvPackagingStats.isVisible = false
            }
        }
    }

    private fun observePackagingState() {
        collectFlow(packagingViewModel.uiState) {
            updateLoadingState()
        }
    }

    private fun updateLoadingState() {
        val b = _binding ?: return
        val isLoading = orderViewModel.uiState.value.isLoading ||
                packagingViewModel.uiState.value.isLoading
        b.progressBar.isVisible = isLoading
    }

    private fun observeOrderEvents() {
        collectFlow(orderViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is OrderEvent.ShowError -> showErrorSnackbar(event.message)

                // Точечный откат конкретного свайпнутого элемента через notifyItemChanged
                is OrderEvent.DetachPackagingFailed -> {
                    val position = adapter.currentList.indexOfFirst { it.id == event.packagingId }
                    if (position != -1) {
                        adapter.notifyItemChanged(position)
                    }
                }

                else -> Unit
            }
        }
    }

    private fun observePackagingEvents() {
        collectFlow(packagingViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is PackagingEvent.NavigateToPackaging -> {
                    val action = OrderPackagingFragmentDirections
                        .actionOrderPackagingFragmentToPackagingFragment(null)
                    findNavController().navigateSafely(action)
                }

                is PackagingEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Helpers ----------

    private fun renderOrder(order: Order) {
        val b = _binding ?: return

        b.tvStatsTitle.text = getString(R.string.packaging_title_format, order.contractNumber)

        allPackagingUiItems = order.packaging.map { box ->
            val groups = box.products.groupBy { it.process.name }
            PackagingListUiItem(
                id = box.id,
                serialNumber = box.serialNumber,
                totalCount = box.products.size,
                types = groups.map { (name, list) -> ModuleTypeUi(name = name, count = list.size) },
                performedAt = box.performedAt?.let { formatPackagingDate(it) },
                performedBy = box.performedBy?.name
            )
        }

        val currentQuery = b.etSearch.text?.toString().orEmpty()
        filterPackaging(currentQuery)

        updateSwipeHelper(order)
    }

    private fun updateSwipeHelper(order: Order) {
        val isClosed = order.shipmentDate != null
        if (!isClosed && itemTouchHelper == null) {
            itemTouchHelper = ItemTouchHelper(buildSwipeCallback())
                .also { it.attachToRecyclerView(binding.rvPackagingStats) }
        } else if (isClosed && itemTouchHelper != null) {
            itemTouchHelper?.attachToRecyclerView(null)
            itemTouchHelper = null
        }
    }

    private fun buildSwipeCallback() =
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
                        orderViewModel.detachPackagingFromOrder(listOf(item.id))
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
}
