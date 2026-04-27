package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.EmployeePlanUiItem
import ru.faserkraft.client.adapter.PlansAdapter
import ru.faserkraft.client.databinding.FragmentDayPlanBinding
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.ui.base.BaseFragment
import ru.faserkraft.client.ui.common.SharedUiViewModel
import ru.faserkraft.client.ui.dailyplan.DailyPlanViewModel
import ru.faserkraft.client.utils.collectIn
import ru.faserkraft.client.utils.collectEventsIn
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.utils.formatPlanDate
import java.time.LocalDate


/**
 * МИГРИРОВАННЫЙ DayPlanFragment с новой архитектурой
 *
 * НОВОЕ: Использует DailyPlanViewModel вместо ScannerViewModel
 * НОВОЕ: Использует StateFlow вместо LiveData
 * НОВОЕ: Работает с Domain Models вместо DTOs
 * НОВОЕ: Наследуется от BaseFragment для общей логики
 */
@AndroidEntryPoint
class DayPlanFragment : BaseFragment<DailyPlanViewModel>() {

    override val viewModel: DailyPlanViewModel by activityViewModels()
    private val sharedUiViewModel: SharedUiViewModel by activityViewModels()

    private var _binding: FragmentDayPlanBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlansAdapter
    private var currentUserRole: UserRole? = null
    private var canEdit: Boolean = false
    private var isPastDate: Boolean = false
    private var datePicker: MaterialDatePicker<Long>? = null

    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        observeSharedViewModel()
        setupClickListeners()

        // Загружаем планы на текущую дату при старте
        val today = LocalDate.now().toString()
        val todayUi = convertDate(today)
        binding.etDate.setText(todayUi)
        viewModel.getDayPlans(today)
    }

    /**
     * Настроить RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = PlansAdapter(
            canEdit,
            onEditPlanClick = { employeePlan ->
                // TODO: Implement navigation to edit plan
                showDialog("Редактирование плана пока не реализовано в новой архитектуре")
            },
            onEmployeeProductsClick = { employeePlan ->
                // TODO: Implement navigation to employee products
                showDialog("Навигация к продуктам сотрудника пока не реализована")
            }
        )

        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = adapter

        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        // TODO: Implement swipe to delete
        // val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
        //     // Implementation
        // }
        // ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvPlans)
    }

    /**
     * Наблюдать за состоянием ViewModel
     */
    private fun observeViewModel() {
        // Состояние планов
        viewModel.dayPlansState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> {
                    hideLoadingIndicator()
                }
                is UiState.Loading -> {
                    showLoadingIndicator()
                }
                is UiState.Success -> {
                    hideLoadingIndicator()
                    updatePlansList(state.data)
                }
                is UiState.Error -> {
                    hideLoadingIndicator()
                    showErrorDialog(state.exception)
                }
            }
        }
    }

    /**
     * Наблюдать за общими событиями
     */
    private fun observeSharedViewModel() {
        // TODO: Implement user data observation when available
        sharedUiViewModel.errorMessages.collectEventsIn(viewLifecycleOwner) { message ->
            showDialog(message)
        }
    }

    /**
     * Обновить список планов
     */
    private fun updatePlansList(plans: List<DailyPlan>) {
        if (plans.isEmpty()) {
            adapter.submitList(emptyList())
            checkEmpty()
            return
        }

        // Маппим данные с бэкенда в UI модели
        val uiItems = mutableListOf<EmployeePlanUiItem>()
        plans.forEach { plan ->
            val steps = plan.steps
            if (steps.isEmpty()) return@forEach

            uiItems += EmployeePlanUiItem.Header(plan.employee.name)
            steps.forEach { step ->
                // TODO: Create proper EmployeePlanDto from domain model
                val employeePlan = ru.faserkraft.client.dto.EmployeePlanDto(
                    id = step.id,
                    date = plan.date,
                    employee = ru.faserkraft.client.dto.EmployeeDto(
                        id = plan.employee.id,
                        name = plan.employee.name,
                        user = ru.faserkraft.client.dto.UserDto(
                            id = 0, // TODO: Add proper user ID
                            email = "" // TODO: Add proper email
                        )
                    ),
                    stepDefinition = ru.faserkraft.client.dto.StepDefinitionDto(
                        id = step.stepDefinition.id,
                        order = step.stepDefinition.order,
                        template = ru.faserkraft.client.dto.TemplateDto(
                            name = step.stepDefinition.name, // Use name as template name
                            nameGenitive = step.stepDefinition.name // TODO: Add proper genitive
                        )
                    ),
                    workProcess = step.workProcess.name, // workProcess is String in DTO
                    plannedQuantity = step.plannedQuantity,
                    actualQuantity = step.actualQuantity ?: 0 // actualQuantity is Int in DTO
                )
                uiItems += EmployeePlanUiItem.Step(employeePlan)
            }
        }
        adapter.submitList(uiItems)
    }

    /**
     * Настроить обработчики клика
     */
    private fun setupClickListeners() {
        binding.etDate.setOnClickListener {
            it.clearFocus()
            showDatePicker()
        }

        binding.swipeRefresh.setOnRefreshListener {
            val planDate = convertDate(binding.etDate.text.toString())
            viewModel.getDayPlans(planDate)
        }

        // TODO: Implement user role checking
        // binding.fabAddPlan.setOnClickListener {
        //     if (currentUserRole != UserRole.MASTER) return@setOnClickListener
        //     if (isPastDate) showCopyPlanDialog() else openAddPlanScreen()
        // }

        binding.btnPrevDate.setOnClickListener { shiftDate(-1) }
        binding.btnNextDate.setOnClickListener { shiftDate(1) }
    }

    private fun showDatePicker() {
        // уже есть и показан – выходим
        datePicker?.let { picker ->
            if (picker.isAdded) return
        }

        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выбор даты")

        val picker = builder.build()
        datePicker = picker

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val (datePlan, etDatePlan) = formatPlanDate(utcMillis)
            binding.etDate.setText(etDatePlan)
            viewModel.getDayPlans(datePlan)
        }

        picker.addOnDismissListener {
            datePicker = null
        }

        picker.show(parentFragmentManager, "day_plan_date_picker")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun shiftDate(days: Long) {
        val currentText = binding.etDate.text?.toString().orEmpty()
        val apiDate = convertDate(currentText)

        if (!apiDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return

        val newDate = LocalDate.parse(apiDate).plusDays(days)
        val newApiDate = newDate.toString()
        val newUiDate = convertDate(newApiDate)

        binding.etDate.setText(newUiDate)
        recomputeCanEdit(newApiDate)
        viewModel.getDayPlans(newApiDate)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun recomputeCanEdit(planDateApi: String? = null) {
        // TODO: Implement proper user role checking
        val isMaster = currentUserRole == UserRole.MASTER

        val effectiveDateApi = planDateApi ?: run {
            val text = binding.etDate.text?.toString().orEmpty()
            val api = convertDate(text)
            api.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        }

        val selectedDate = effectiveDateApi?.let { LocalDate.parse(it) }
        val today = LocalDate.now()

        isPastDate = selectedDate?.isBefore(today) == true

        canEdit = isMaster
        binding.fabAddPlan.visibility = if (canEdit) View.VISIBLE else View.GONE

        if (canEdit) {
            if (isPastDate) {
                binding.fabAddPlan.setImageResource(R.drawable.ic_copy)
                binding.fabAddPlan.contentDescription = "Скопировать план"
            } else {
                binding.fabAddPlan.setImageResource(R.drawable.ic_add)
                binding.fabAddPlan.contentDescription = "Добавить план"
            }
        }

        if (this::adapter.isInitialized) {
            val allowStepEdit = isMaster && !isPastDate
            adapter.setCanEdit(allowStepEdit)
        }
    }

    private fun showLoadingIndicator() {
        binding.swipeRefresh.isRefreshing = true
    }

    private fun hideLoadingIndicator() {
        binding.swipeRefresh.isRefreshing = false
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyPlans.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvPlans.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvPlans.adapter = null
        datePicker?.dismiss()
        datePicker = null
        _binding = null
    }

}