package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.AddPackagingAdapter
import ru.faserkraft.client.adapter.ModuleTypeUi
import ru.faserkraft.client.adapter.PackagingShipmentUiItem
import ru.faserkraft.client.databinding.FragmentOrderAddPackagingBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class OrderAddPackagingFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentOrderAddPackagingBinding? = null
    private val binding get() = _binding!!

    private val args: OrderAddPackagingFragmentArgs by navArgs()

    private lateinit var adapter: AddPackagingAdapter

    private var activeDialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOrderAddPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = args.orderId
        binding.tvOrderDetails.text = "Заказ ID: $orderId"

        adapter = AddPackagingAdapter { item, isChecked ->
            val current = adapter.currentList.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index != -1) {
                current[index] = current[index].copy(isSelected = isChecked)
                adapter.submitList(current)
            }

            val allSelected = current.isNotEmpty() && current.all { it.isSelected }
            if (binding.cbSelectAll.isChecked != allSelected) {
                binding.cbSelectAll.setOnCheckedChangeListener(null)
                binding.cbSelectAll.isChecked = allSelected
                binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
            }
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
        binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)

        // Единая функция для фильтрации и обновления списка
        fun updateList() {
            // 1. Получаем список требуемых процессов для данного заказа
            val requiredProcesses = viewModel.orders.value
                ?.find { it.id == orderId }
                ?.items
                ?.map { it.workProcess.name }
                ?.toSet()
                ?: emptySet()

            // 2. Берем список всех упаковок на складе
            val packaging = viewModel.packagingBoxes.value.orEmpty()

            // 3. Фильтруем и маппим
            val uiItems: List<PackagingShipmentUiItem> = packaging
                .filter { box ->
                    // Оставляем упаковку, если в ней есть хотя бы один продукт с нужным типом процесса.
                    // (Если упаковка должна состоять строго ТОЛЬКО из нужных деталей, замените "any" на "all")
                    box.products.any { product ->
                        requiredProcesses.contains(product.process.name)
                    }
                }
                .map { box ->
                    val groups = box.products.groupBy { it.process.name }
                    val types = groups.map { (name, list) ->
                        ModuleTypeUi(name = name, count = list.size)
                    }
                    PackagingShipmentUiItem(
                        id = box.id,
                        serialNumber = box.serialNumber,
                        totalCount = box.products.size,
                        types = types,
                        isSelected = false
                    )
                }

            adapter.submitList(uiItems)

            // Обновляем состояние чекбокса "Выбрать все"
            binding.cbSelectAll.setOnCheckedChangeListener(null)
            val allSelected = uiItems.isNotEmpty() && uiItems.all { it.isSelected }
            binding.cbSelectAll.isChecked = allSelected
            binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
        }

        // Наблюдаем за заказами (чтобы знать требуемые процессы)
        viewModel.orders.observe(viewLifecycleOwner) {
            updateList()
        }

        // Наблюдаем за складом (чтобы знать, какие упаковки доступны)
        viewModel.packagingBoxes.observe(viewLifecycleOwner) {
            updateList()
        }

        // Запрашиваем актуальные данные со склада (заказы обычно уже загружены на предыдущем экране)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPackagingInStorage()
            // Если заказы могли не загрузиться, раскомментируйте строку ниже:
            // viewModel.getOrders()
        }

        // Правильная и безопасная обработка ошибок (без дублей)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded || msg.isNullOrBlank()) return@collect

                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .also { builder ->
                            builder.setOnDismissListener {
                                viewModel.resetIsHandled()
                                activeDialog = null
                            }
                        }
                        .show()
                }
            }
        }

        // Кнопка сохранения
        binding.btnSave.setOnClickListener {
            val selectedUiItems = adapter.currentList.filter { it.isSelected }
            if (selectedUiItems.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Внимание")
                    .setMessage("Вы не выбрали ни одной упаковки для добавления")
                    .setPositiveButton("ОК", null)
                    .show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Подтверждение")
                .setMessage("Добавить ${selectedUiItems.size} упаковок в заказ ID $orderId?")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("OK") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        performAddToOrder(orderId, selectedUiItems)
                    }
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activeDialog?.dismiss()
        activeDialog = null

        _binding?.cbSelectAll?.setOnCheckedChangeListener(null)
        binding.rvProducts.adapter = null
        _binding = null
    }

    private suspend fun performAddToOrder(
        orderId: Int,
        selectedUiItems: List<PackagingShipmentUiItem>
    ) {
        val packagingIds = selectedUiItems.map { it.id }

        val result = viewModel.addPackagingToOrder(orderId, packagingIds)

        if (!isAdded || _binding == null) return

        if (result.isSuccess) {
            AlertDialog.Builder(requireContext())
                .setTitle("Успех")
                .setMessage("Упаковки (${packagingIds.size} шт.) успешно добавлены в заказ.\n\nПродолжить добавление упаковок?")
                .setPositiveButton("Продолжить") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Завершить") { _, _ ->
                    findNavController().navigateUp()
                }
                .setCancelable(false)
                .show()
        }
    }

    private val selectAllListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        val updated = adapter.currentList.map { it.copy(isSelected = isChecked) }
        adapter.submitList(updated)
    }
}