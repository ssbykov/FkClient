package ru.faserkraft.client.presentation.order


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import ru.faserkraft.client.databinding.FragmentAddOrderItemBinding


class AddOrderItemBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: OrderViewModel by activityViewModels()

    private var _binding: FragmentAddOrderItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddOrderItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val processes = state.processes
                    if (processes.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            processes.map { it.name }
                        )
                        binding.actvModuleType.setAdapter(adapter)
                    }
                }
            }
        }

        binding.btnAdd.setOnClickListener {
            val selectedName = binding.actvModuleType.text.toString()
            val quantityStr = binding.etQuantity.text.toString()

            if (selectedName.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantity = quantityStr.toIntOrNull() ?: 0
            if (quantity <= 0) {
                Toast.makeText(requireContext(), "Количество должно быть больше 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedProcess = viewModel.uiState.value.processes.find { it.name == selectedName }
            if (selectedProcess == null) {
                Toast.makeText(requireContext(), "Выберите тип модуля из списка", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setFragmentResult("add_item_request", Bundle().apply {
                putInt("processId", selectedProcess.id)
                putString("processName", selectedProcess.name)
                putInt("quantity", quantity)
            })
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}