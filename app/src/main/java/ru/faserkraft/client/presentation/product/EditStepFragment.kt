package ru.faserkraft.client.presentation.product

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.adapter.EmployeeUi
import ru.faserkraft.client.adapter.EmployeesAdapter
import ru.faserkraft.client.databinding.FragmentEditStepBinding
import ru.faserkraft.client.presentation.ui.collectFlow

class EditStepFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentEditStepBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeesAdapter: EmployeesAdapter
    private var employees: List<EmployeeUi> = emptyList()
    private var selectedIndex: Int? = null
    private var prefillDone = false

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        employeesAdapter = EmployeesAdapter(requireContext())
        binding.actvEmployee.setAdapter(employeesAdapter)

        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

        observeState()
        observeEvents()
        setupSaveButton()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // Прогресс
            b.btnEdit.isEnabled = !state.isActionInProgress
            b.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE

            // Заголовки
            b.tvSerial.text = state.product?.serialNumber ?: "Не определен"
            b.tvStep.text = state.selectedStep?.definition?.name.orEmpty()

            // Список сотрудников — обновляем только если изменился
            val newEmployees = state.employees.map { EmployeeUi(it.id, it.name) }
            if (newEmployees != employees) {
                employees = newEmployees
                employeesAdapter.setItems(employees)
                prefillEmployee()
            }
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> showDialog(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Prefill ----------

    private fun prefillEmployee() {
        if (prefillDone) return
        val performedById = viewModel.uiState.value.selectedStep?.performedBy?.id ?: return
        val index = employees.indexOfFirst { it.id == performedById }
        if (index >= 0) {
            prefillDone = true
            selectedIndex = index
            _binding?.actvEmployee?.setText(employees[index].name, false)
        }
    }

    // ---------- Save ----------

    private fun setupSaveButton() {
        binding.btnEdit.setOnClickListener {
            val step = viewModel.uiState.value.selectedStep ?: return@setOnClickListener
            val index = selectedIndex

            // Ничего не выбрано — просто уходим
            if (index == null) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            val newEmployee = employees.getOrNull(index) ?: run {
                showDialog("Сотрудник не выбран")
                return@setOnClickListener
            }

            // Исполнитель не изменился — просто уходим
            if (newEmployee.id == step.performedBy?.id) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            viewModel.changeStepPerformer(
                stepId = step.id,
                newEmployeeId = newEmployee.id,
            )
            navigateUpOnComplete()
        }
    }

    // Ждём завершения action и уходим
    private fun navigateUpOnComplete() {
        collectFlow(viewModel.uiState) { state ->
            if (!state.isActionInProgress && _binding != null) {
                findNavController().navigateUp()
            }
        }
    }

    // ---------- Dialog ----------

    private fun showDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }
}