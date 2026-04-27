package ru.faserkraft.client.activity

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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.ui.base.BaseFragment
import ru.faserkraft.client.ui.common.SharedUiViewModel
import ru.faserkraft.client.ui.product.ProductViewModel
import ru.faserkraft.client.utils.collectEventsIn
import ru.faserkraft.client.utils.collectIn
import ru.faserkraft.client.utils.formatIsoToUi

/**
 * МИГРИРОВАННЫЙ ProductFragment с новой архитектурой
 *
 * НОВОЕ: Использует ProductViewModel вместо ScannerViewModel
 * НОВОЕ: Использует StateFlow вместо LiveData
 * НОВОЕ: Работает с Domain Models вместо DTOs
 * НОВОЕ: Наследуется от BaseFragment для общей логики
 */
@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class ProductFragment : BaseFragment<ProductViewModel>() {

    override val viewModel: ProductViewModel by activityViewModels()
    private val sharedUiViewModel: SharedUiViewModel by activityViewModels()

    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    private var userRole: UserRole? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
     * Наблюдать за состоянием товара
     */
    private fun observeViewModel() {
        // Состояние товара
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

        // Состояние последнего шага
        viewModel.lastStepState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    updateLastStepUI(state.data)
                }
                is UiState.Error -> {
                    // Обработка ошибки последнего шага
                }
                else -> {
                    // Idle или Loading - ничего не делаем
                }
            }
        }

        // Состояние действий (завершение шага, изменение статуса)
        viewModel.actionState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is ProductViewModel.ActionState.Idle -> {
                    binding.btnDone.isEnabled = true
                    binding.progressEdit.visibility = View.GONE
                }
                is ProductViewModel.ActionState.InProgress -> {
                    binding.btnDone.isEnabled = false
                    binding.progressEdit.visibility = View.VISIBLE
                }
                is ProductViewModel.ActionState.Success -> {
                    binding.btnDone.isEnabled = true
                    binding.progressEdit.visibility = View.GONE
                    showDialog(state.message)
                }
                is ProductViewModel.ActionState.Error -> {
                    binding.btnDone.isEnabled = true
                    binding.progressEdit.visibility = View.GONE
                    showErrorDialog(state.exception)
                }
            }
        }
    }

    /**
     * Наблюдать за общими событиями (ошибки, навигация)
     */
    private fun observeSharedViewModel() {
        sharedUiViewModel.errorMessages.collectEventsIn(viewLifecycleOwner) { message ->
            showDialog(message)
        }

        // TODO: Добавить наблюдение за userData когда будет реализовано
        // sharedUiViewModel.userData.collectIn(viewLifecycleOwner) { user ->
        //     userRole = user?.role
        // }
    }

    /**
     * Обновить UI на основе Product Domain Model
     */
    private fun updateProductUI(product: Product) {
        with(binding) {
            // Основная информация
            tvProcess.text = "Process #${product.processId}" // TODO: Получить имя процесса
            tvProductNumber.text = product.serialNumber
            tvCreated.text = product.createdDate ?: "N/A"

            // Статус товара
            val statusText = when (product.status) {
                "ACTIVE" -> "АКТИВЕН"
                "COMPLETED" -> "ЗАВЕРШЕН"
                "REWORK" -> "РЕМОНТ"
                "SCRAP" -> "БРАК"
                else -> product.status
            }
            chipProductStatus.text = statusText

            // Цвета статуса
            val (bgColorRes, textColorRes) = when (product.status) {
                "ACTIVE" -> R.color.status_success_bg to R.color.status_success_text
                "COMPLETED" -> R.color.status_success_bg to R.color.status_success_text
                "REWORK" -> R.color.status_rework_bg to R.color.status_rework_text
                "SCRAP" -> R.color.status_scrap_bg to R.color.status_scrap_text
                else -> R.color.bg_card to R.color.black
            }

            val bgColor = ContextCompat.getColor(requireContext(), bgColorRes)
            val textColor = ContextCompat.getColor(requireContext(), textColorRes)

            chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            chipProductStatus.setTextColor(textColor)
            cardProductInfo.setCardBackgroundColor(bgColor)
        }
    }

    /**
     * Обновить UI последнего шага
     */
    private fun updateLastStepUI(step: ru.faserkraft.client.domain.model.Step) {
        with(binding) {
            val ctx = cardRoot.context

            tvStepName.text = ctx.getString(
                R.string.step_last_title,
                step.stepDefinition.name
            )

            // Статус шага
            val (statusTitle, statusDesc, bgColorRes) = when (step.status) {
                "done" -> Triple("Выполнен", "Завершен ${step.completedAt ?: "N/A"}", R.color.step_done_bg)
                "in_progress" -> Triple("В работе", "Начато ${step.startedAt ?: "N/A"}", R.color.status_success_bg)
                "pending" -> Triple("Ожидает", "Ожидает выполнения", R.color.step_pending_bg)
                else -> Triple(step.status, "", R.color.bg_card)
            }

            tvStatus.text = statusTitle
            tvCompletedAt.text = statusDesc
            cardRoot.setBackgroundColor(ContextCompat.getColor(ctx, bgColorRes))
        }
    }

    /**
     * Настроить UI элементы
     */
    private fun setupUI() {
        with(binding) {
            tvProductNumber.text = "Загрузка..."
            tvProcess.text = "Загрузка..."
            tvStepName.text = "Загрузка..."
            tvStatus.text = "Загрузка..."
            tvCompletedAt.text = "Загрузка..."
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
                val product = (viewModel.productState.value as? UiState.Success)?.data
                val lastStep = (viewModel.lastStepState.value as? UiState.Success)?.data

                if (product == null || lastStep == null) {
                    showDialog("Данные не загружены")
                    return@setOnClickListener
                }

                // Проверка статуса товара
                if (product.status == "REWORK" || product.status == "SCRAP") {
                    showDialog("Нельзя закрыть этап: продукт в статусе РЕМОНТ или БРАК")
                    return@setOnClickListener
                }

                // Проверка статуса шага
                if (lastStep.status == "done") {
                    showDialog("Этап уже выполнен")
                    return@setOnClickListener
                }

                showConfirmDialog("Закрыть этап", "Вы уверены, что хотите закрыть этап?", onConfirm = {
                    lifecycleScope.launch {
                        viewModel.completeCurrentStep()
                    }
                })
            }

            // Кнопка редактировать процесс
            btnEdit.setOnClickListener {
                if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                    showConfirmDialog("Изменение процесса", "Вы уверены, что хотите изменить процесс?", {
                        lifecycleScope.launch {
                            // TODO: Реализовать setProcesses в новой архитектуре
                            // viewModel.setProcesses()
                            if (_binding != null) {
                                findNavController().navigate(R.id.action_productFragment_to_editProductFragment)
                            }
                        }
                    })
                } else {
                    showDialog("Недостаточно прав для изменения процесса")
                }
            }

            // Чип статуса товара
            chipProductStatus.setOnClickListener {
                if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                    showConfirmDialog("Изменение статуса", "Вы уверены, что хотите изменить статус?", {
                        findNavController().navigate(R.id.action_productFragment_to_editProductStatusFragment)
                    })
                } else {
                    showDialog("Недостаточно прав для изменения статуса")
                }
            }
        }
    }

    private fun showLoadingIndicator() {
        binding.progressEdit.visibility = View.VISIBLE
        binding.btnDone.isEnabled = false
    }

    private fun hideLoadingIndicator() {
        binding.progressEdit.visibility = View.GONE
        binding.btnDone.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        // TODO: Реализовать reset в новой архитектуре если нужно
        // viewModel.resetIsHandled()
        super.onDestroy()
    }
}