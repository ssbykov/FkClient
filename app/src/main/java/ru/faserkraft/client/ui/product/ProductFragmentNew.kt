package ru.faserkraft.client.ui.product

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.ui.base.BaseFragment
import ru.faserkraft.client.ui.common.SharedUiViewModel
import ru.faserkraft.client.utils.collectEventsIn
import ru.faserkraft.client.utils.collectIn

/**
 * Рефакторная версия ProductFragment с новой архитектурой
 * НОВОЕ: Использует ProductViewModel вместо ScannerViewModel
 * НОВОЕ: Используе StateFlow вместо LiveData
 * НОВОЕ: Работает с Domain Models вместо DTOs
 */
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class ProductFragmentNew : BaseFragment<ProductViewModel>() {

    override val viewModel: ProductViewModel by viewModels()
    private val sharedUiViewModel: SharedUiViewModel by viewModels()

    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
        observeSharedViewModel()
        setupClickListeners()
    }

    /**
     * Наблюдать за состоянием товара в ViewModel
     */
    private fun observeViewModel() {
        viewModel.productState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> {
                    hideLoadingIndicator()
                }
                is UiState.Loading -> {
                    showLoadingIndicator()
                }
                is UiState.Success -> {
                    hideLoadingIndicator()
                    updateProductUI(state.data)
                }
                is UiState.Error -> {
                    hideLoadingIndicator()
                    showErrorDialog(state.exception)
                }
            }
        }

        // Наблюдать за состоянием действий (завершение шага, обновление статуса)
        viewModel.actionState.collectIn(viewLifecycleOwner) { actionState ->
            when (actionState) {
                is ProductViewModel.ActionState.Idle -> {
                    hideActionProgress()
                }
                is ProductViewModel.ActionState.InProgress -> {
                    showActionProgress()
                }
                is ProductViewModel.ActionState.Success -> {
                    hideActionProgress()
                    showDialog(actionState.message)
                    viewModel.resetActionState()
                }
                is ProductViewModel.ActionState.Error -> {
                    hideActionProgress()
                    showErrorDialog(actionState.exception)
                    viewModel.resetActionState()
                }
            }
        }
    }

    /**
     * Наблюдать за события из SharedUiViewModel (ошибки, навигация)
     */
    private fun observeSharedViewModel() {
        sharedUiViewModel.errorMessages.collectEventsIn(viewLifecycleOwner) { message ->
            showDialog(message)
        }
    }

    /**
     * Обновить UI на основе Product Domain Model
     */
    private fun updateProductUI(product: Product) {
        with(binding) {
            // Основная информация
            tvProductNumber.text = product.serialNumber
            tvProcess.text = "Process #${product.processId}"
            tvCreated.text = product.createdDate ?: "N/A"

            // Статус товара - используем простые цвета
            val statusColor = when (product.status) {
                "ACTIVE" -> android.R.color.holo_green_light
                "COMPLETED" -> android.R.color.holo_blue_light
                "REWORK" -> android.R.color.holo_orange_light
                "SCRAP" -> android.R.color.holo_red_light
                else -> android.R.color.darker_gray
            }
            cardProductInfo.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), statusColor)
            )

            // Шаги производства - TODO: добавить элементы UI
            val firstIncompleteStep = product.steps.firstOrNull { it.status != "done" }
            if (firstIncompleteStep != null) {
                // TODO: Добавить TextView для отображения текущего шага
                // tvCurrentStep.text = firstIncompleteStep.stepDefinition.name
            }
        }
    }

    /**
     * Настроить UI элементы
     */
    private fun setupUI() {
        with(binding) {
            tvProductNumber.text = "Загрузка..."
            tvProcess.text = "Загрузка..."
            // tvCurrentStep.text = "Загрузка..." // TODO: добавить элемент UI
        }
    }

    /**
     * Настроить обработчики клика
     */
    private fun setupClickListeners() {
        with(binding) {
            // Кнопка просмотра всех этапов
            btnAllStages.setOnClickListener {
                findNavController().navigate(R.id.action_productFragment_to_productFullFragment)
            }

            // Кнопка завершить этап
            btnDone.setOnClickListener {
                showConfirmDialog(
                    "Завершить этап",
                    "Вы уверены, что хотите завершить текущий этап?",
                    onConfirm = {
                        viewModel.completeCurrentStep()
                    }
                )
            }

            // Кнопка редактировать процесс
            btnEdit.setOnClickListener {
                findNavController().navigate(R.id.action_productFragment_to_editProductFragment)
            }
        }
    }

    private fun showLoadingIndicator() {
        // binding.progressBar.visibility = View.VISIBLE // TODO: добавить ProgressBar
        binding.btnDone.isEnabled = false
    }

    private fun hideLoadingIndicator() {
        // binding.progressBar.visibility = View.GONE // TODO: добавить ProgressBar
        binding.btnDone.isEnabled = true
    }

    private fun showActionProgress() {
        // binding.progressBarAction.visibility = View.VISIBLE // TODO: добавить ProgressBar
    }

    private fun hideActionProgress() {
        // binding.progressBarAction.visibility = View.GONE // TODO: добавить ProgressBar
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
