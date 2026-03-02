package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.EmployeeUi
import ru.faserkraft.client.adapter.EmployeesAdapter
import ru.faserkraft.client.databinding.FragmentQrGenerationBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class QrGenerationFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentQrGenerationBinding

    private lateinit var employeesAdapter: EmployeesAdapter
    private var employees: List<EmployeeUi> = emptyList()
    private var selectedIndex: Int? = null
    private var selectedEmployeeId: Int? = null
    private var isEmployeesReady = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentQrGenerationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // адаптер как в EditStepFragment
        employeesAdapter = EmployeesAdapter(requireContext())
        binding.actvEmployee.setAdapter(employeesAdapter)

        observeEmployees()
        observeQrBitmap()
        observeErrors()
        observeUiState()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setEmployees()
        }

        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
            val employee = employees.getOrNull(position)
            selectedEmployeeId = employee?.id
        }

        binding.btnGenerateQr.setOnClickListener {
            val index = selectedIndex
            val employeeId = selectedEmployeeId

            if (index == null || employeeId == null) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Сотрудник не выбран")
                    .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadAndGenerateQr(employeeId)
            }
        }

        // обработка ошибок
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
    }

    private fun observeEmployees() {
        viewModel.employees.observe(viewLifecycleOwner) { list ->
            employees = list
                ?.map { EmployeeUi(it.id, it.name) }
                .orEmpty()

            employeesAdapter.setItems(employees)
            isEmployeesReady = true

            // если есть сотрудники — по умолчанию выбираем первого
            if (employees.isNotEmpty()) {
                val first = employees.first()
                selectedIndex = 0
                selectedEmployeeId = first.id
                binding.actvEmployee.setText(first.name, false)
            }
        }
    }

    private fun observeQrBitmap() {
        viewModel.qrBitmap.observe(viewLifecycleOwner) { bitmap: Bitmap? ->
            if (bitmap != null) {
                binding.ivQrCode.setImageBitmap(bitmap)
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val inProgress = state.isActionInProgress

            binding.btnGenerateQr.isEnabled = !inProgress
            binding.btnGenerateQr.alpha = if (inProgress) 0.5f else 1f

            binding.progressQr.visibility =
                if (inProgress) View.VISIBLE else View.GONE
        }
    }

    private fun observeErrors() {
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
    }


    private fun selectEmployeeIfPossible(employeeId: Int?) {
        if (!isEmployeesReady || employeeId == null) return

        val selectedEmployee = employees.find { it.id == employeeId }
        selectedEmployee?.let { emp ->
            val index = employees.indexOf(emp)
            selectedIndex = index
            selectedEmployeeId = emp.id
            binding.actvEmployee.setText(emp.name, false)
        }
    }
}
