package ru.faserkraft.client.presentation.plan

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.adapter.AddStepsAdapter
import ru.faserkraft.client.adapter.EmployeeUi
import ru.faserkraft.client.adapter.EmployeesAdapter
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.adapter.StepUi
import ru.faserkraft.client.databinding.FragmentAddDayPlanBinding
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.convertDate

class AddDayPlanFragment : Fragment() {

    private val viewModel: PlanViewModel by activityViewModels()

    private var _binding: FragmentAddDayPlanBinding? = null
    private val binding get() = _binding!!

    // Редактирование: берём из state вместо navArgs
    private var editingPlan: DailyPlan? = null
    private var editingStep: DailyPlanStep? = null

    private lateinit var employeesAdapter: EmployeesAdapter
    private lateinit var processAdapter: ProcessAdapter
    private lateinit var stepsAdapter: AddStepsAdapter

    private var employees: List<EmployeeUi> = emptyList()
    private var processes: List<ProcessUi> = emptyList()
    private var steps: List<StepUi> = emptyList()

    private var selectedEmployeeIndex: Int? = null
    private var selectedProcessIndex: Int? = null
    private var selectedStepIndex: Int? = null

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Читаем контекст редактирования из state один раз при входе
        val state = viewModel.uiState.value
        editingPlan = state.selectedPlan
        editingStep = state.selectedStep

        setupAdapters()
        setupMode()
        setupUiListeners()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        // Чистим выбранный план при уходе с экрана
        viewModel.clearSelectedPlanStep()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapters() {
        employeesAdapter = EmployeesAdapter(requireContext())
        processAdapter = ProcessAdapter(requireContext())
        stepsAdapter = AddStepsAdapter(requireContext())

        binding.actvEmployee.setAdapter(employeesAdapter)
        binding.actvProcess.setAdapter(processAdapter)
        binding.actvStep.setAdapter(stepsAdapter)
    }

    private fun setupMode() {
        val plan = editingPlan
        val step = editingStep
        if (plan != null && step != null) {
            // Режим редактирования
            binding.etDate.setText(convertDate(plan.date))
            binding.etQty.setText(step.plannedQuantity.toString())
        } else {
            // Режим создания — дата из текущего state
            binding.etDate.setText(convertDate(viewModel.uiState.value.date))
        }
    }

    // ---------- Observers ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // Прогресс кнопки
            b.btnCreatePlan.isEnabled = !state.isActionInProgress
            b.progressCreatePlan.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE

            // Сотрудники
            val newEmployees = state.employees.toEmployeeUi()
            if (newEmployees != employees) {
                employees = newEmployees
                employeesAdapter.setItems(employees)
                prefillEmployee(state.employees)
            }

            // Процессы
            val newProcesses = state.processes.toProcessUi()
            if (newProcesses != processes) {
                processes = newProcesses
                processAdapter.setItems(processes)
                prefillProcess(state.processes)
            }
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is PlanEvent.ShowError -> showDialog(event.message)
            }
        }
    }

    // ---------- Prefill (режим редактирования) ----------

    private fun prefillEmployee(employeeList: List<Employee>) {
        val plan = editingPlan ?: return
        if (selectedEmployeeIndex != null) return

        val index = employeeList.indexOfFirst { it.id == plan.employee.id }
        if (index >= 0) {
            selectedEmployeeIndex = index
            binding.actvEmployee.setText(employees[index].name, false)
        }
    }

    private fun prefillProcess(processList: List<Process>) {
        val step = editingStep ?: return
        if (selectedProcessIndex != null) return

        val index = processList.indexOfFirst { it.name == step.workProcess }
        if (index >= 0) {
            selectedProcessIndex = index
            binding.actvProcess.setText(processes[index].name, false)
            loadStepsForProcess(processList[index])

            // Предзаполняем шаг после загрузки steps
            val stepIndex = steps.indexOfFirst { it.id == step.stepDefinitionId }
            if (stepIndex >= 0) {
                selectedStepIndex = stepIndex
                binding.actvStep.setText(steps[stepIndex].name, false)
            }
        }
    }

    // ---------- UI listeners ----------

    private fun setupUiListeners() {
        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedEmployeeIndex = position
        }

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
            selectedProcessIndex = position
            val process = viewModel.uiState.value.processes.getOrNull(position)
                ?: return@setOnItemClickListener
            loadStepsForProcess(process)
            selectedStepIndex = null
            binding.actvStep.setText("", false)
        }

        binding.actvStep.setOnItemClickListener { _, _, position, _ ->
            selectedStepIndex = position
        }

        binding.btnCreatePlan.setOnClickListener {
            if (editingPlan == null) onCreateClicked() else onUpdateClicked()
        }
    }

    // ---------- Actions ----------

    private fun onCreateClicked() {
        val (empIndex, stepIndex, qty) = validateSelection() ?: return
        val planDate = convertDate(binding.etDate.text.toString())
        viewModel.addStepToPlan(
            planDate = planDate,
            employeeId = employees[empIndex].id,
            stepId = steps[stepIndex].id,
            plannedQuantity = qty,
        )
        observeSuccessAndNavigateUp()
    }

    private fun onUpdateClicked() {
        val plan = editingPlan ?: return
        val step = editingStep ?: return
        val (empIndex, stepIndex, qty) = validateSelection() ?: return
        val planDate = convertDate(binding.etDate.text.toString())
        viewModel.updateStepInPlan(
            stepId = step.id,
            planDate = planDate,
            stepDefinitionId = steps[stepIndex].id,
            employeeId = employees[empIndex].id,
            plannedQuantity = qty,
        )
        observeSuccessAndNavigateUp()
    }

    // Ждём завершения action и уходим вверх по стеку
    private fun observeSuccessAndNavigateUp() {
        collectFlow(viewModel.uiState) { state ->
            if (!state.isActionInProgress && _binding != null) {
                // Навигируемся только если нет ошибки (ошибка уйдёт через events)
                findNavController().navigateUp()
            }
        }
    }

    // ---------- Validation ----------

    private fun validateSelection(): Triple<Int, Int, Int>? {
        val empIndex = selectedEmployeeIndex
        val stepIndex = selectedStepIndex
        val qtyText = binding.etQty.text?.toString()?.trim()

        if (empIndex == null || empIndex !in employees.indices) {
            showDialog("Сотрудник не выбран"); return null
        }
        if (selectedProcessIndex == null) {
            showDialog("Процесс не выбран"); return null
        }
        if (stepIndex == null || stepIndex !in steps.indices) {
            showDialog("Этап не выбран"); return null
        }
        val qty = qtyText?.toIntOrNull()
        if (qty == null || qty <= 0) {
            showDialog("Укажите корректное количество"); return null
        }
        return Triple(empIndex, stepIndex, qty)
    }

    // ---------- Helpers ----------

    private fun loadStepsForProcess(process: Process) {
        steps = process.steps.map { StepUi(id = it.id, name = it.name) }
        stepsAdapter.setItems(steps)
    }

    private fun showDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    // ---------- Маппинг domain → UI ----------

    private fun List<Employee>.toEmployeeUi() = map { EmployeeUi(it.id, it.name) }
    private fun List<Process>.toProcessUi() = map { ProcessUi(it.id, it.name) }
}