package ru.faserkraft.client.activity

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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.LocalOrderItem
import ru.faserkraft.client.adapter.OrderItemsAdapter
import ru.faserkraft.client.databinding.FragmentNewOrderBinding
import ru.faserkraft.client.dto.OrderCreateDto
import ru.faserkraft.client.dto.OrderItemCreateDto
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewOrderFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentNewOrderBinding? = null
    private val binding get() = _binding!!

    // Сохраняем даты в формате yyyy-MM-dd
    private var selectedContractDate: String? = null
    private var selectedPlannedDate: String? = null

    // Локальный список позиций заказа, пока он не отправлен на сервер
    private val orderItemsList = mutableListOf<LocalOrderItem>()
    private lateinit var itemsAdapter: OrderItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupDatePickers()
        setupBottomSheetListener()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setProcesses()
        }

        // Кнопка вызова шторки добавления позиции
        binding.btnAddItem.setOnClickListener {
            val bottomSheet = AddOrderItemBottomSheet()
            bottomSheet.show(parentFragmentManager, "AddOrderItemBottomSheet")
        }

        // Обработка сохранения всего заказа
        binding.btnSave.setOnClickListener {
            val contractNumber = binding.etContractNumber.text?.toString()?.trim()

            if (contractNumber.isNullOrEmpty() || selectedContractDate == null || selectedPlannedDate == null) {
                Toast.makeText(requireContext(), "Заполните основные параметры", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (orderItemsList.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Добавьте хотя бы одну позицию в заказ",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 1. Формируем DTO только для шапки заказа
            val newOrder = OrderCreateDto(
                contractNumber = contractNumber,
                contractDate = selectedContractDate!!,
                plannedShipmentDate = selectedPlannedDate!!
            )

            // 2. Формируем список DTO для позиций
            val mappedItems = orderItemsList.map { item ->
                OrderItemCreateDto(
                    processId = item.processId,
                    quantity = item.quantity
                )
            }

            viewLifecycleOwner.lifecycleScope.launch {
                // Шаг 1: Отправляем шапку заказа на сервер
                val createResult = viewModel.createOrder(newOrder)

                createResult.onSuccess { createdOrder ->
                    // Шаг 2: Если заказ успешно создан, берем его ID и отправляем позиции
                    val updateResult = viewModel.updateOrderItems(createdOrder.id, mappedItems)

                    if (updateResult.isSuccess) {
                        Toast.makeText(
                            requireContext(),
                            "Заказ и позиции успешно добавлены",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().popBackStack()
                    } else {
                        // Ошибка добавления позиций (сама шапка при этом уже создалась)
                        Toast.makeText(
                            requireContext(),
                            "Заказ создан, но ошибка при добавлении позиций",
                            Toast.LENGTH_LONG
                        ).show()
                        // Можно тоже выйти или оставить пользователя на экране
                        findNavController().popBackStack()
                    }

                }.onFailure {
                    // Если ошибка произошла еще на этапе создания шапки,
                    // сообщение об ошибке покажется автоматически через errorState во ViewModel
                }
            }
        }

        // Подписка на состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isLoading = state.isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
            binding.btnAddItem.isEnabled = !isLoading
            binding.etContractNumber.isEnabled = !isLoading
            binding.etContractDate.isEnabled = !isLoading
            binding.etPlannedDate.isEnabled = !isLoading
        }
    }


    private fun setupRecyclerView() {
        itemsAdapter = OrderItemsAdapter(orderItemsList) { position ->
            // Обработка удаления позиции по клику на корзину
            orderItemsList.removeAt(position)
            itemsAdapter.notifyItemRemoved(position)
            itemsAdapter.notifyItemRangeChanged(position, orderItemsList.size)
        }
        binding.rvOrderItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrderItems.adapter = itemsAdapter
    }

    private fun setupDatePickers() {
        binding.etContractDate.setOnClickListener {
            showDatePicker { displayDate, serverDate ->
                binding.etContractDate.setText(displayDate)
                selectedContractDate = serverDate
            }
        }

        binding.etPlannedDate.setOnClickListener {
            showDatePicker { displayDate, serverDate ->
                binding.etPlannedDate.setText(displayDate)
                selectedPlannedDate = serverDate
            }
        }
    }

    private fun setupBottomSheetListener() {
        setFragmentResultListener("add_item_request") { _, bundle ->
            // Достаем данные по правильным ключам из шторки
            val processId = bundle.getInt("processId", -1)
            val processName = bundle.getString("processName") ?: return@setFragmentResultListener
            val quantity = bundle.getInt("quantity", 0)

            // Ищем, есть ли уже такой процесс в списке
            val existingItemIndex = orderItemsList.indexOfFirst { it.processId == processId }

            if (existingItemIndex != -1) {
                // Если процесс уже есть, заменяем его новым (обновляем количество)
                orderItemsList[existingItemIndex] = LocalOrderItem(processId, processName, quantity)
                // Уведомляем адаптер, что конкретный элемент изменился
                itemsAdapter.notifyItemChanged(existingItemIndex)
                Toast.makeText(requireContext(), "Количество обновлено", Toast.LENGTH_SHORT).show()
            } else {
                // Если процесса еще нет, добавляем как новый элемент
                orderItemsList.add(LocalOrderItem(processId, processName, quantity))
                itemsAdapter.notifyItemInserted(orderItemsList.size - 1)
                // Прокручиваем список к новому элементу
                binding.rvOrderItems.scrollToPosition(orderItemsList.size - 1)
            }
        }
    }

    private fun showDatePicker(onDateSelected: (displayDate: String, serverDate: String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }

            val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val serverFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            onDateSelected(
                displayFormat.format(selectedCalendar.time),
                serverFormat.format(selectedCalendar.time)
            )
        }, year, month, day).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvOrderItems.adapter = null
        _binding = null
    }
}