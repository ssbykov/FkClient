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
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.databinding.FragmentEditProductBinding
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.viewmodel.ScannerViewModel


class EditProductFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentEditProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var processAdapter: ProcessAdapter
    private var processes: List<ProcessUi> = emptyList()
    private var product: ProductDto? = null
    private var isProcessesReady = false

    private var selectedIndex: Int? = null

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        processAdapter = ProcessAdapter(requireContext())
        binding.actvProcess.setAdapter(processAdapter)

        viewModel.processes.observe(viewLifecycleOwner) { list ->
            processes = list?.map { ProcessUi(it.id, it.name) }.orEmpty()
            processAdapter.setItems(processes)
            isProcessesReady = true
            selectProductProcessIfPossible()
        }

        viewModel.productState.observe(viewLifecycleOwner) { state ->
            _binding?.tvSerial?.text = state?.serialNumber ?: "Не определен"
            product = state
            selectProductProcessIfPossible()
        }

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

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
                        .also { builder ->
                            builder.setOnDismissListener { activeDialog = null }
                        }
                        .show()
                }
            }
        }

        binding.btnEdit.setOnClickListener {
            val index = selectedIndex
            if (index == null || index < 0 || index >= processes.size) {
                activeDialog?.dismiss()
                activeDialog = AlertDialog.Builder(requireContext())
                    .setMessage("Процесс не выбран")
                    .setPositiveButton("ОК") { dialog, _ ->
                        dialog.dismiss()
                        activeDialog = null
                    }
                    .show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val id = product?.id ?: return@launch
                val processId = processes[index].id

                val result = viewModel.changeProductProcess(
                    productId = id,
                    newProcessId = processId
                )

                if (_binding == null) return@launch

                result.onSuccess {
                    findNavController().navigateUp()
                }
                // onFailure — ошибка уйдёт в errorState
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnEdit.isEnabled = !state.isActionInProgress
            binding.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }

    private fun selectProductProcessIfPossible() {
        val b = _binding ?: return
        val id = product?.process?.id
        if (!isProcessesReady || id == null) return

        val selectedProcess = processes.find { it.id == id }
        selectedProcess?.let {
            b.actvProcess.setText(it.name, false)
        }
    }
}