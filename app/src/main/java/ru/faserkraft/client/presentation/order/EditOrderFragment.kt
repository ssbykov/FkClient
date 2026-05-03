package ru.faserkraft.client.presentation.order

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.LocalOrderItem
import ru.faserkraft.client.adapter.OrderItemsAdapter
import ru.faserkraft.client.databinding.FragmentNewOrderBinding
import ru.faserkraft.client.domain.model.OrderItem
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.utils.apiFormat
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.utils.formatPlanDate
import ru.faserkraft.client.utils.showErrorSnackbar
import java.util.Calendar

class EditOrderFragment : Fragment() {

    private val viewModel: OrderViewModel by activityViewModels()

    private var _binding: FragmentNewOrderBinding? = null
    private val binding get() = _binding!!

    private var selectedContractDate: String? = null
    private var selectedPlannedDate: String? = null

    private val orderItemsList = mutableListOf<LocalOrderItem>()
    private lateinit var itemsAdapter: OrderItemsAdapter

    private var isDataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupDatePickers()
        setupBottomSheetListener()
        observeState()
        observeEvents()

        if (viewModel.uiState.value.processes.isEmpty()) {
            viewModel.loadProcesses()
        }

        binding.btnAddItem.setOnClickListener {
            AddOrderItemBottomSheet().show(parentFragmentManager, "AddOrderItemBottomSheet")
        }
        binding.btnSave.setOnClickListener { saveChanges() }
    }

    private fun setupUI() {
        binding.toolbar.title = "Редактирование заказа"
        binding.btnSave.text = "Сохранить изменения"
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    val isLoading = state.isLoading || state.isActionInProgress
                    b.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    b.btnSave.isEnabled = !isLoading
                    b.btnAddItem.isEnabled = !isLoading

                    val order = state.currentOrder ?: return@collect
                    if (!isDataLoaded) {
                        b.etContractNumber.setText(order.contractNumber)

                        selectedContractDate = order.contractDate.take(10)
                        selectedPlannedDate = order.plannedShipmentDate.take(10)

                        b.etContractDate.setText(convertDate(selectedContractDate!!))
                        b.etPlannedDate.setText(convertDate(selectedPlannedDate!!))

                        orderItemsList.clear()
                        orderItemsList.addAll(order.items.map { item ->
                            LocalOrderItem(
                                processId = item.workProcess.id,
                                type = item.workProcess.name,
                                quantity = item.quantity
                            )
                        })
                        itemsAdapter.notifyDataSetChanged()
                        isDataLoaded = true
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is OrderEvent.ShowError -> showErrorSnackbar(event.message)
                        OrderEvent.OrderUpdated -> findNavController().popBackStack()
                        OrderEvent.OrderClosed,
                        OrderEvent.OrderDeleted,
                        OrderEvent.OrderCreated,
                        OrderEvent.PackagingAdded -> Unit
                    }
                }
            }
        }
    }

    private fun saveChanges() {
        val contractNumber = binding.etContractNumber.text?.toString()?.trim()
        val orderId = viewModel.uiState.value.currentOrder?.id

        if (orderId == null) {
            showErrorSnackbar("Заказ не загружен")
            return
        }
        if (contractNumber.isNullOrEmpty() || selectedContractDate == null || selectedPlannedDate == null) {
            showErrorSnackbar("Заполните основные параметры")
            return
        }
        if (orderItemsList.isEmpty()) {
            showErrorSnackbar("Добавьте хотя бы одну позицию")
            return
        }

        val domainItems = orderItemsList.map { item ->
            OrderItem(
                id = 0,
                quantity = item.quantity,
                workProcess = Process(
                    id = item.processId,
                    name = item.type,
                    description = "",
                    steps = emptyList()
                )
            )
        }

        viewModel.updateOrder(
            orderId = orderId,
            contractNumber = contractNumber,
            contractDate = selectedContractDate!!,
            plannedShipmentDate = selectedPlannedDate!!,
            items = domainItems
        )
    }

    private fun setupRecyclerView() {
        itemsAdapter = OrderItemsAdapter(orderItemsList) { position ->
            orderItemsList.removeAt(position)
            itemsAdapter.notifyItemRemoved(position)
            itemsAdapter.notifyItemRangeChanged(position, orderItemsList.size)
        }
        binding.rvOrderItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrderItems.adapter = itemsAdapter
    }

    private fun setupBottomSheetListener() {
        setFragmentResultListener("add_item_request") { _, bundle ->
            val processId = bundle.getInt("processId", -1)
            val processName = bundle.getString("processName") ?: return@setFragmentResultListener
            val quantity = bundle.getInt("quantity", 0)

            val existingIndex = orderItemsList.indexOfFirst { it.processId == processId }
            if (existingIndex != -1) {
                orderItemsList[existingIndex] = LocalOrderItem(processId, processName, quantity)
                itemsAdapter.notifyItemChanged(existingIndex)
            } else {
                orderItemsList.add(LocalOrderItem(processId, processName, quantity))
                itemsAdapter.notifyItemInserted(orderItemsList.size - 1)
                binding.rvOrderItems.scrollToPosition(orderItemsList.size - 1)
            }
        }
    }

    private fun setupDatePickers() {
        binding.etContractDate.setOnClickListener {
            showDatePicker(selectedContractDate) { display, server ->
                binding.etContractDate.setText(display)
                selectedContractDate = server
            }
        }
        binding.etPlannedDate.setOnClickListener {
            showDatePicker(selectedPlannedDate) { display, server ->
                binding.etPlannedDate.setText(display)
                selectedPlannedDate = server
            }
        }
    }

    private fun showDatePicker(currentApiDate: String?, onDateSelected: (String, String) -> Unit) {
        val calendar = Calendar.getInstance()
        if (!currentApiDate.isNullOrEmpty()) {
            try {
                calendar.time = apiFormat.parse(currentApiDate)!!
            } catch (e: Exception) {
                Log.w("EditOrderFragment", "Не удалось распарсить дату: $currentApiDate", e)
            }
        }
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val sel = Calendar.getInstance().apply { set(year, month, day) }
                val (serverDate, displayDate) = formatPlanDate(sel.timeInMillis)
                onDateSelected(displayDate, serverDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDataLoaded = false
        binding.rvOrderItems.adapter = null
        _binding = null
    }
}