package ru.faserkraft.client.presentation.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.presentation.order.AddPackagingAdapter
import ru.faserkraft.client.presentation.order.ModuleTypeUi
import ru.faserkraft.client.presentation.order.PackagingShipmentUiItem
import ru.faserkraft.client.databinding.FragmentOrderAddPackagingBinding
import ru.faserkraft.client.domain.model.Order
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.presentation.packaging.PackagingEvent
import ru.faserkraft.client.presentation.packaging.PackagingViewModel
import ru.faserkraft.client.utils.showErrorSnackbar

class OrderAddPackagingFragment : Fragment() {

    private val orderViewModel: OrderViewModel by activityViewModels()
    private val packagingViewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentOrderAddPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AddPackagingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderAddPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        observeState()
        observeOrderEvents()
        observePackagingEvents()

        packagingViewModel.loadPackagingInStorage()

        binding.btnSave.setOnClickListener { onSaveClick() }
    }

    private fun setupAdapter() {
        adapter = AddPackagingAdapter { item, isChecked ->
            val current = adapter.currentList.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index != -1) {
                current[index] = current[index].copy(isSelected = isChecked)
                adapter.submitList(current)
            }
            syncSelectAllCheckbox(current)
        }
    }

    private fun setupRecyclerView() {
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
        binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                orderViewModel.uiState.collect { orderState ->
                    val b = _binding ?: return@collect
                    val order = orderState.currentOrder ?: return@collect

                    b.tvOrderDetails.text = getString(
                        R.string.order_details_format,
                        order.contractNumber
                    )

                    updateList(order, packagingViewModel.uiState.value.packagingInStorage)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                packagingViewModel.uiState.collect { packagingState ->
                    val order = orderViewModel.uiState.value.currentOrder ?: return@collect
                    updateList(order, packagingState.packagingInStorage)
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
                        OrderEvent.PackagingAdded -> showPackagingAddedDialog()
                        OrderEvent.OrderClosed,
                        OrderEvent.OrderDeleted,
                        OrderEvent.OrderUpdated,
                        OrderEvent.OrderCreated -> Unit
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
                        is PackagingEvent.ShowError -> showErrorSnackbar(event.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun updateList(order: Order, storage: List<Packaging>) {
        val requiredProcesses = order.items
            .map { it.workProcess.name }
            .toSet()

        val uiItems = storage
            .filter { box ->
                box.products.any { product ->
                    requiredProcesses.contains(product.process.name)
                }
            }
            .map { box ->
                val groups = box.products.groupBy { it.process.name }
                PackagingShipmentUiItem(
                    id = box.id,
                    serialNumber = box.serialNumber,
                    totalCount = box.products.size,
                    types = groups.map { (name, list) -> ModuleTypeUi(name = name, count = list.size) },
                    isSelected = false
                )
            }

        adapter.submitList(uiItems)
        syncSelectAllCheckbox(uiItems)
    }

    private fun onSaveClick() {
        val selectedItems = adapter.currentList.filter { it.isSelected }
        val orderId = orderViewModel.uiState.value.currentOrder?.id ?: return

        if (selectedItems.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Внимание")
                .setMessage("Вы не выбрали ни одной упаковки для добавления")
                .setPositiveButton("ОК", null)
                .show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Подтверждение")
            .setMessage("Добавить ${selectedItems.size} упаковок в заказ?")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("OK") { _, _ ->
                orderViewModel.addPackagingToOrder(orderId, selectedItems.map { it.id })
            }
            .show()
    }

    private fun showPackagingAddedDialog() {
        if (_binding == null || !isAdded) return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Успех")
            .setMessage("Упаковки успешно добавлены в заказ.\n\nПродолжить добавление?")
            .setPositiveButton("Продолжить", null)
            .setNegativeButton("Завершить") { _, _ -> findNavController().navigateUp() }
            .setCancelable(false)
            .show()
    }

    private fun syncSelectAllCheckbox(items: List<PackagingShipmentUiItem>) {
        val b = _binding ?: return
        val allSelected = items.isNotEmpty() && items.all { it.isSelected }
        b.cbSelectAll.setOnCheckedChangeListener(null)
        b.cbSelectAll.isChecked = allSelected
        b.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
    }

    private val selectAllListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        val updated = adapter.currentList.map { it.copy(isSelected = isChecked) }
        adapter.submitList(updated)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.cbSelectAll?.setOnCheckedChangeListener(null)
        binding.rvProducts.adapter = null
        _binding = null
    }
}