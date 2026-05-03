package ru.faserkraft.client.presentation.registration

import QrGenerationEvent
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.databinding.FragmentQrGenerationBinding
import ru.faserkraft.client.presentation.common.adapter.EmployeeUi
import ru.faserkraft.client.presentation.common.adapter.EmployeesAdapter
import ru.faserkraft.client.presentation.ui.collectFlow

@AndroidEntryPoint
class QrGenerationFragment : Fragment() {

    private val viewModel: QrGenerationViewModel by viewModels()

    private var _binding: FragmentQrGenerationBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeesAdapter: EmployeesAdapter
    private var employees: List<EmployeeUi> = emptyList()
    private var selectedEmployeeId: Int? = null

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQrGenerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupListeners()
        observeState()
        observeEvents()
        viewModel.loadEmployees()
    }

    private fun setupAdapter() {
        employeesAdapter = EmployeesAdapter(requireContext())
        binding.actvEmployee.setAdapter(employeesAdapter)
    }

    private fun setupListeners() {
        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedEmployeeId = employees.getOrNull(position)?.id
        }

        binding.btnGenerateQr.setOnClickListener {
            val employeeId = selectedEmployeeId
            if (employeeId == null) {
                showDialog("Сотрудник не выбран")
                return@setOnClickListener
            }
            binding.ivQrCode.setImageDrawable(null)
            viewModel.generateQr(employeeId)
        }
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            val uiEmployees = state.employees.map { EmployeeUi(it.id, it.name) }
            if (uiEmployees != employees) {
                employees = uiEmployees
                employeesAdapter.setItems(employees)
                val first = employees.firstOrNull()
                selectedEmployeeId = first?.id
                b.actvEmployee.setText(first?.name.orEmpty(), false)
            }

            b.ivQrCode.setImageBitmap(state.qrBitmap)

            val inProgress = state.isLoading || state.isActionInProgress
            b.btnGenerateQr.isEnabled = !inProgress
            b.btnGenerateQr.alpha = if (inProgress) 0.5f else 1f
            b.progressQrContainer.visibility =
                if (inProgress) View.VISIBLE else View.GONE
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is QrGenerationEvent.ShowError -> showDialog(event.message)
            }
        }
    }

    private fun showDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { activeDialog = null }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}