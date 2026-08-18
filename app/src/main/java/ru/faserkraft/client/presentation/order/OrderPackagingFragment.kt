package ru.faserkraft.client.presentation.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class OrderPackagingFragment : Fragment() {

    private val orderViewModel: OrderViewModel by activityViewModels()
    private val packagingViewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentOrderPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingListAdapter
    private var itemTouchHelper: ItemTouchHelper? = null

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
            packagingViewModel.loadPackaging(item.serialNumber)
        }
    }

    private fun setupRecyclerView() {
        binding.rvPackagingStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingStats.adapter = adapter
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

        val isEmpty = uiItems.isEmpty() && !b.progressBar.isVisible
        b.tvEmptyStorage.isVisible = isEmpty
        b.rvPackagingStats.isVisible = !isEmpty

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
}