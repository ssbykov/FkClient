package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.faserkraft.client.databinding.FragmentAddOrderItemBinding
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.viewmodel.ScannerViewModel

class AddOrderItemBottomSheet : BottomSheetDialogFragment() {

    // Получаем ту же ViewModel, что и в основном фрагменте
    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentAddOrderItemBinding? = null
    private val binding get() = _binding!!

    // Сохраняем список процессов локально, чтобы потом найти выбранный по имени
    private var processesList: List<ProcessDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddOrderItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Подписываемся на список процессов
        viewModel.processes.observe(viewLifecycleOwner) { processes ->
            if (processes != null) {
                processesList = processes

                // Достаем только имена для отображения в выпадающем списке
                val processNames = processes.map { it.name }

                // Создаем адаптер для AutoCompleteTextView
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    processNames
                )
                binding.actvModuleType.setAdapter(adapter)
            }
        }

        // 2. Обработка клика на кнопку "Добавить"
        binding.btnAdd.setOnClickListener {
            val selectedName = binding.actvModuleType.text.toString()
            val quantityStr = binding.etQuantity.text.toString()

            if (selectedName.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toIntOrNull() ?: 0
            if (quantity <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Количество должно быть больше 0",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Находим выбранный ProcessDto по имени, чтобы достать его ID
            val selectedProcess = processesList.find { it.name == selectedName }
            if (selectedProcess == null) {
                Toast.makeText(
                    requireContext(),
                    "Выберите тип модуля из списка",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // 3. Передаем данные обратно во фрагмент (имя, ID процесса и количество)
            val result = Bundle().apply {
                putInt("processId", selectedProcess.id)
                putString("processName", selectedProcess.name)
                putInt("quantity", quantity)
            }
            setFragmentResult("add_item_request", result)

            // Закрываем шторку
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}