package ru.faserkraft.client.presentation.product

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.databinding.FragmentEditProductBinding
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.showErrorSnackbar

class EditProductFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentEditProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var processAdapter: ProcessAdapter
    private var processes: List<ProcessUi> = emptyList()
    private var selectedIndex: Int? = null
    private var prefillDone = false

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        processAdapter = ProcessAdapter(requireContext())
        binding.actvProcess.setAdapter(processAdapter)

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
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

            // Серийный номер
            b.tvSerial.text = state.product?.serialNumber ?: "Не определен"

            // Список процессов — обновляем только если изменился
            val newProcesses = state.processes.map { ProcessUi(it.id, it.name) }
            if (newProcesses != processes) {
                processes = newProcesses
                processAdapter.setItems(processes)
                prefillProcess(state.product?.process?.id)
            }
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Prefill ----------

    private fun prefillProcess(currentProcessId: Int?) {
        if (prefillDone || currentProcessId == null) return
        val index = processes.indexOfFirst { it.id == currentProcessId }
        if (index >= 0) {
            prefillDone = true
            selectedIndex = index
            _binding?.actvProcess?.setText(processes[index].name, false)
        }
    }

    // ---------- Save ----------

    private fun setupSaveButton() {
        binding.btnEdit.setOnClickListener {
            val productId = viewModel.uiState.value.product?.id ?: return@setOnClickListener
            val index = selectedIndex

            if (index == null || index !in processes.indices) {
                showErrorSnackbar("Процесс не выбран")
                return@setOnClickListener
            }

            val newProcessId = processes[index].id
            val currentProcessId = viewModel.uiState.value.product?.process?.id

            // Процесс не изменился — просто уходим
            if (newProcessId == currentProcessId) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            viewModel.changeProcess(productId, newProcessId)
            navigateUpOnComplete()
        }
    }

    private fun navigateUpOnComplete() {
        collectFlow(viewModel.uiState) { state ->
            if (!state.isActionInProgress && _binding != null) {
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }
}