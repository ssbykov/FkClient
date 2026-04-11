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

    private var _binding: FragmentQrGenerationBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeesAdapter: EmployeesAdapter
    private var employees: List<EmployeeUi> = emptyList()
    private var selectedIndex: Int? = null
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
                showMessage("Сотрудник не выбран")
                return@setOnClickListener
            }

            // можно очистить старый qr перед новой генерацией
            binding.ivQrCode.setImageDrawable(null)

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadAndGenerateQr(employeeId)
            }
        }
    }

    private fun observeEmployees() {
        viewModel.employees.observe(viewLifecycleOwner) { list ->
            val b = _binding ?: return@observe

            employees = list
                ?.map { EmployeeUi(it.id, it.name) }
                .orEmpty()

            employeesAdapter.setItems(employees)

            if (employees.isNotEmpty()) {
                val first = employees.first()
                selectedIndex = 0
                selectedEmployeeId = first.id
                b.actvEmployee.setText(first.name, false)
            } else {
                selectedIndex = null
                selectedEmployeeId = null
                b.actvEmployee.setText("", false)
            }
        }
    }

    private fun observeQrBitmap() {
        viewModel.qrBitmap.observe(viewLifecycleOwner) { bitmap: Bitmap? ->
            val b = _binding ?: return@observe
            if (bitmap != null) {
                b.ivQrCode.setImageBitmap(bitmap)
            } else {
                b.ivQrCode.setImageDrawable(null)
            }
        }
    }

    private fun observeUiState() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val b = _binding ?: return@observe
            val inProgress = state.isActionInProgress || state.isLoading

            b.btnGenerateQr.isEnabled = !inProgress
            b.btnGenerateQr.alpha = if (inProgress) 0.5f else 1f
            b.progressQrContainer.visibility =
                if (inProgress) View.VISIBLE else View.GONE
        }
    }

    private fun observeErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded) return@collect

                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                            activeDialog = null
                        }
                        .also { builder ->
                            builder.setOnDismissListener { activeDialog = null }
                        }
                        .show()
                }
            }
        }
    }

    private fun showMessage(text: String) {
        if (!isAdded) return

        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(text)
            .setPositiveButton("ОК") { dialog, _ ->
                dialog.dismiss()
                activeDialog = null
            }
            .also { builder ->
                builder.setOnDismissListener { activeDialog = null }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}