package ru.faserkraft.client.presentation.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ru.faserkraft.client.databinding.BottomSheetInventoryConfirmBinding

class InventoryConfirmBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private var _binding: BottomSheetInventoryConfirmBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetInventoryConfirmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val state = viewModel.uiState.value
        val product = state.pendingProduct ?: run {
            dismiss()
            return
        }
        val steps = state.availableSteps

        // Свайп вниз / кнопка «назад» — сбрасываем pending
        dialog?.setOnCancelListener {
            viewModel.cancelPendingItem()
        }

        // --- Данные продукта ---
        binding.tvSerialNumber.text = product.serialNumber

        // --- Spinner с этапами ---
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            steps.map { it.nameGenitive },
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerStep.adapter = adapter

        // Предвыбираем последний выполненный шаг
        val preselectedIndex = steps.indexOfFirst { it.id == state.preselectedStepId }
        if (preselectedIndex >= 0) {
            binding.spinnerStep.setSelection(preselectedIndex)
        }

        // --- Кнопки ---
        binding.btnConfirm.setOnClickListener {
            val selectedStep = steps.getOrNull(binding.spinnerStep.selectedItemPosition)
                ?: return@setOnClickListener
            viewModel.confirmItem(
                serialNumber = product.serialNumber,
                stepDefinitionId = selectedStep.id,
            )
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            viewModel.cancelPendingItem()
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "InventoryConfirmBottomSheet"
    }
}