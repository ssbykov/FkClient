package ru.faserkraft.client.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.LocalOrderItem
import ru.faserkraft.client.adapter.OrderItemsAdapter
import ru.faserkraft.client.databinding.FragmentNewOrderBinding
import ru.faserkraft.client.dto.OrderItemCreateDto
import ru.faserkraft.client.dto.OrderUpdateDto
import ru.faserkraft.client.utils.apiFormat
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.utils.formatPlanDate
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.util.Calendar

class EditOrderFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private val args: EditOrderFragmentArgs by navArgs()

    private var _binding: FragmentNewOrderBinding? = null
    private val binding get() = _binding!!

    // Сохраняем даты в формате yyyy-MM-dd
    private var selectedContractDate: String? = null
    private var selectedPlannedDate: String? = null

    private val orderItemsList = mutableListOf<LocalOrderItem>()
    private lateinit var itemsAdapter: OrderItemsAdapter

    // Флаг, чтобы заполнять данные заказа с сервера только один раз
    private var isDataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupDatePickers()
        setupBottomSheetListener()

        if (viewModel.processes.value.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch { viewModel.setProcesses() }
        }

        // Если данные еще не загружались, запрашиваем их у бэкенда
        if (!isDataLoaded) {
            viewModel.getOrder(args.orderId)
        }

        // Подписка на данные
        viewModel.currentOrder.observe(viewLifecycleOwner) { order ->
            // Проверяем, что это нужный заказ и мы еще не предзаполняли форму
            if (order != null && order.id == args.orderId && !isDataLoaded) {

                binding.etContractNumber.setText(order.contractNumber)

                selectedContractDate = order.contractDate.take(10)
                selectedPlannedDate = order.plannedShipmentDate.take(10)

                // Используем утилиту convertDate!
                binding.etContractDate.setText(convertDate(selectedContractDate!!))
                binding.etPlannedDate.setText(convertDate(selectedPlannedDate!!))

                // Заполняем позиции только при первой загрузке
                orderItemsList.clear()
                val mappedLocalItems = order.items.map { item ->
                    LocalOrderItem(
                        processId = item.workProcess.id,
                        type = item.workProcess.name,
                        quantity = item.quantity
                    )
                }
                orderItemsList.addAll(mappedLocalItems)
                itemsAdapter.notifyDataSetChanged()

                isDataLoaded = true // Защита от повторного перезаписывания при повороте экрана
            }
        }

        binding.btnAddItem.setOnClickListener {
            val bottomSheet = AddOrderItemBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddOrderItemBottomSheet")
        }

        binding.btnSave.setOnClickListener { saveChanges() }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isLoading = state.isLoading || state.isActionInProgress
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
            binding.btnAddItem.isEnabled = !isLoading
        }
    }

    private fun setupUI() {
        binding.toolbar.title = "Редактирование заказа"
        binding.btnSave.text = "Сохранить изменения"
    }

    private fun saveChanges() {
        val contractNumber = binding.etContractNumber.text?.toString()?.trim()

        if (contractNumber.isNullOrEmpty() || selectedContractDate == null || selectedPlannedDate == null) {
            Toast.makeText(requireContext(), "Заполните основные параметры", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (orderItemsList.isEmpty()) {
            Toast.makeText(requireContext(), "Добавьте хотя бы одну позицию", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val orderUpdate = OrderUpdateDto(
            id = args.orderId,
            contractNumber = contractNumber,
            contractDate = selectedContractDate!!,
            plannedShipmentDate = selectedPlannedDate!!
        )

        val mappedItems = orderItemsList.map { item ->
            OrderItemCreateDto(processId = item.processId, quantity = item.quantity)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val updateHeaderResult = viewModel.updateOrder(orderUpdate)
            if (updateHeaderResult.isSuccess) {
                val updateItemsResult = viewModel.updateOrderItems(args.orderId, mappedItems)
                if (updateItemsResult.isSuccess) {
                    Toast.makeText(requireContext(), "Заказ обновлен", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка при обновлении позиций",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
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

    // --- ОБНОВЛЕННАЯ РАБОТА С ДАТАМИ ЧЕРЕЗ УТИЛИТЫ ---

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
        viewModel.clearCurrentOrder()
        binding.rvOrderItems.adapter = null
        _binding = null
    }
}