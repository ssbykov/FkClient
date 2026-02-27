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
import ru.faserkraft.client.adapter.EmployeeUi
import ru.faserkraft.client.adapter.EmployeesAdapter
import ru.faserkraft.client.databinding.FragmentEditStepBinding
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.viewmodel.ScannerViewModel


class EditStepFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private val args: EditStepFragmentArgs by navArgs()

    private lateinit var binding: FragmentEditStepBinding

    private lateinit var employeesAdapter: EmployeesAdapter
    private var employees: List<EmployeeUi> = emptyList()
    private var product: ProductDto? = null
    private var isEmployeesReady = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditStepBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        employeesAdapter = EmployeesAdapter(requireContext())
        binding.actvEmployee.setAdapter(employeesAdapter)

        viewModel.employees.observe(viewLifecycleOwner) { list ->
            employees = list
                ?.map { EmployeeUi(it.id, it.name) }
                .orEmpty()
            employeesAdapter.setItems(employees)
            isEmployeesReady = true
            selectEmployeeIfPossible()
        }

        viewModel.productState.observe(viewLifecycleOwner) { state ->
            binding.tvSerial.text = state?.serialNumber ?: "Не определен"
            binding.tvStep.text = args.step.stepDefinition.template.name
            product = state
            selectEmployeeIfPossible()
        }

        var selectedIndex: Int? = null

        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position          // индекс в адаптере
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        binding.btnEdit.setOnClickListener {

            val index = selectedIndex
            if (index == null || index < 0 || index >= employees.size) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Сотрудник не выбран")
                    .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }


            viewLifecycleOwner.lifecycleScope.launch {
                val step = args.step
                val newEmployee = employees.getOrNull(index) ?: return@launch

                val result = viewModel.changeStepPerformer(
                    step = step,
                    newEmployeeId = newEmployee.id
                )

                result.onSuccess {
                    findNavController().navigateUp()
                }.onFailure {
                    // ничего не делаем, ошибку покажет подписка на errorState
                }
            }

        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnEdit.isEnabled = !state.isActionInProgress

            binding.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
        }

    }

    private fun selectEmployeeIfPossible() {
        val id = args.step.performedBy?.id
        if (!isEmployeesReady || id == null) return

        val selectedEmployee = employees.find { it.id == id }
        selectedEmployee?.let {
            binding.actvEmployee.setText(it.name, false)
        }
    }

}