package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.AddStepsAdapter
import ru.faserkraft.client.adapter.EmployeeUi
import ru.faserkraft.client.adapter.EmployeesAdapter
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.adapter.StepUi
import ru.faserkraft.client.databinding.FragmentAddDayPlanBinding
import ru.faserkraft.client.dto.EmployeePlanDto
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.viewmodel.ScannerViewModel

class AddDayPlanFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentAddDayPlanBinding? = null
    private val binding get() = _binding!!

    private val args: AddDayPlanFragmentArgs by navArgs()
    private val editingPlan: EmployeePlanDto? get() = args.plan

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
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupAdapters()
        setupMode()
        observeEmployees()
        observeProcesses()
        setupUiListeners()
        observeUiState()
        collectErrors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }

    // --- Setup ---

    private fun setupAdapters() {
        employeesAdapter = EmployeesAdapter(requireContext())
        processAdapter = ProcessAdapter(requireContext())
        stepsAdapter = AddStepsAdapter(requireContext())

        binding.actvEmployee.setAdapter(employeesAdapter)
        binding.actvProcess.setAdapter(processAdapter)
        binding.actvStep.setAdapter(stepsAdapter)
    }

    private fun setupMode() {
        if (editingPlan != null) {
            setupEditMode(editingPlan!!)
        } else {
            binding.etDate.setText(args.planDate)
        }
    }

    private fun setupEditMode(plan: EmployeePlanDto) {
        binding.etDate.setText(convertDate(plan.date))
        binding.etQty.setText(plan.plannedQuantity.toString())
    }

    // --- Observers ---

    private fun observeEmployees() {
        viewModel.employees.observe(viewLifecycleOwner) { list ->
            employees = list?.map { EmployeeUi(it.id, it.name) }.orEmpty()
            employeesAdapter.setItems(employees)

            editingPlan?.let { plan ->
                if (selectedEmployeeIndex == null) {
                    val index = employees.indexOfFirst { it.id == plan.employee.id }
                    if (index >= 0) {
                        selectedEmployeeIndex = index
                        binding.actvEmployee.setText(employees[index].name, false)
                    }
                }
            }
        }
    }

    private fun observeProcesses() {
        viewModel.processes.observe(viewLifecycleOwner) { list ->
            processes = list?.map { ProcessUi(it.id, it.name) }.orEmpty()
            processAdapter.setItems(processes)

            editingPlan?.let { plan ->
                if (selectedProcessIndex == null) {
                    val processIndex = processes.indexOfFirst { it.name == plan.workProcess }
                    if (processIndex >= 0) {
                        selectedProcessIndex = processIndex
                        binding.actvProcess.setText(processes[processIndex].name, false)

                        val processDto = viewModel.processes.value?.getOrNull(processIndex)
                        if (processDto != null) {
                            steps = processDto.steps.map { stepDto ->
                                StepUi(id = stepDto.id, name = stepDto.template.name)
                            }
                            stepsAdapter.setItems(steps)

                            val stepIndex = steps.indexOfFirst { it.id == plan.stepDefinition.id }
                            if (stepIndex >= 0) {
                                selectedStepIndex = stepIndex
                                binding.actvStep.setText(steps[stepIndex].name, false)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- UI listeners ---

    private fun setupUiListeners() {
        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedEmployeeIndex = position
        }

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
            selectedProcessIndex = position

            val processDto = viewModel.processes.value
                ?.getOrNull(position)
                ?: return@setOnItemClickListener

            steps = processDto.steps.map { stepDto ->
                StepUi(id = stepDto.id, name = stepDto.template.name)
            }
            stepsAdapter.setItems(steps)

            selectedStepIndex = null
            binding.actvStep.setText("", false)
        }

        binding.actvStep.setOnItemClickListener { _, _, position, _ ->
            selectedStepIndex = position
        }

        binding.btnCreatePlan.setOnClickListener {
            if (editingPlan == null) {
                onCreatePlanClicked()
            } else {
                onUpdatePlanClicked(editingPlan!!)
            }
        }
    }

    // --- State / errors ---

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // _binding может быть null в редких race condition — безопасный доступ
            val b = _binding ?: return@observe
            b.btnCreatePlan.isEnabled = !state.isActionInProgress
            b.progressCreatePlan.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    private fun collectErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                            activeDialog = null
                        }
                        .also { it.setOnDismissListener { activeDialog = null } }
                        .show()
                }
            }
        }
    }

    // --- Actions ---

    private fun validateSelection(): Triple<Int, Int, Int>? {
        val empIndex = selectedEmployeeIndex
        val procIndex = selectedProcessIndex
        val stepIndex = selectedStepIndex
        val qtyText = binding.etQty.text?.toString()?.trim()

        if (empIndex == null || empIndex !in employees.indices) {
            showMessage("Сотрудник не выбран"); return null
        }
        if (procIndex == null || procIndex !in processes.indices) {
            showMessage("Процесс не выбран"); return null
        }
        if (stepIndex == null || stepIndex !in steps.indices) {
            showMessage("Этап не выбран"); return null
        }

        val plannedQuantity = qtyText?.toIntOrNull()
        if (plannedQuantity == null || plannedQuantity <= 0) {
            showMessage("Укажите корректное количество"); return null
        }

        return Triple(empIndex, stepIndex, plannedQuantity)
    }

    private fun onCreatePlanClicked() {
        val validated = validateSelection() ?: return
        val (empIndex, stepIndex, plannedQuantity) = validated

        val planDate = convertDate(binding.etDate.text.toString())
        val employeeId = employees[empIndex].id
        val stepId = steps[stepIndex].id

        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.addStepToDailyPlan(
                planDate = planDate,
                employeeId = employeeId,
                stepId = stepId,
                plannedQuantity = plannedQuantity
            )

            if (_binding == null) return@launch

            result.onSuccess {
                findNavController().navigateUp()
            }
        }
    }

    private fun onUpdatePlanClicked(plan: EmployeePlanDto) {
        val validated = validateSelection() ?: return
        val (empIndex, stepIndex, plannedQuantity) = validated

        val planDate = convertDate(binding.etDate.text.toString())
        val employeeId = employees[empIndex].id
        val stepDefinitionId = steps[stepIndex].id

        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.updateStepInDailyPlan(
                planDate = planDate,
                employeeId = employeeId,
                stepId = plan.id,
                stepDefinitionId = stepDefinitionId,
                plannedQuantity = plannedQuantity
            )

            if (_binding == null) return@launch

            result.onSuccess {
                findNavController().navigateUp()
            }
        }
    }

    // --- Helpers ---

    private fun showMessage(text: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(text)
            .setPositiveButton("ОК") { dialog, _ ->
                dialog.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }
}