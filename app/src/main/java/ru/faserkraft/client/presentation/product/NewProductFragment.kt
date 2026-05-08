package ru.faserkraft.client.presentation.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentNewProductBinding
import ru.faserkraft.client.presentation.common.adapter.ProcessAdapter
import ru.faserkraft.client.presentation.common.adapter.ProcessUi
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.showErrorSnackbar

class NewProductFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentNewProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var processAdapter: ProcessAdapter
    private var processes: List<ProcessUi> = emptyList()
    private var selectedIndex: Int? = null

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewProductBinding.inflate(inflater, container, false)
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.btnSave.isEnabled = !state.isActionInProgress

            state.pendingSerialNumber?.let { b.tvSerial.text = it }

            val newProcesses = state.processes.map { ProcessUi(it.id, it.name) }
            if (newProcesses != processes) {
                processes = newProcesses
                processAdapter.setItems(processes)
            }
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                is ProductEvent.NavigateToProduct -> navigateToProduct()
                else -> Unit
            }
        }
    }

    // ---------- Save ----------

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val index = selectedIndex
            if (index == null || index !in processes.indices) {
                showErrorSnackbar("Процесс не выбран")
                return@setOnClickListener
            }

            val serialNumber = binding.tvSerial.text.toString()
            val processId = processes[index].id

            viewModel.createProduct(serialNumber, processId)
        }
    }

    // ---------- Навигация ----------

    private fun navigateToProduct() {
        if (_binding == null) return

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.scannerFragment, inclusive = false)
            .build()

        findNavController().navigate(
            R.id.action_newProductFragment_to_productFragment,
            null,
            navOptions,
        )
    }
}