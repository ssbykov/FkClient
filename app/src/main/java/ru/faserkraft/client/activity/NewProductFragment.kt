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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.databinding.FragmentNewProductBinding
import ru.faserkraft.client.dto.ProductCreateDto
import ru.faserkraft.client.utils.nowIsoUtc
import ru.faserkraft.client.viewmodel.ScannerViewModel


class NewProductFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentNewProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var processAdapter: ProcessAdapter
    private var processes: List<ProcessUi> = emptyList()

    private var selectedIndex: Int? = null

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        processAdapter = ProcessAdapter(requireContext())
        binding.actvProcess.setAdapter(processAdapter)

        viewModel.newProduct.observe(viewLifecycleOwner) {
            binding.tvSerial.text = it.serialNumber
        }

        viewModel.processes.observe(viewLifecycleOwner) { list ->
            processes = list
                ?.map { ProcessUi(it.id, it.name) }
                .orEmpty()
            processAdapter.setItems(processes)
        }

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
            selectedIndex = position
        }

        // обработка ошибок
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded) return@collect
                    // 🟡 ИСПРАВЛЕНИЕ 3: закрываем предыдущий перед показом нового
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

        binding.btnSave.setOnClickListener {
            val index = selectedIndex
            if (index == null || index !in processes.indices) {
                showMessage("Процесс не выбран")
                return@setOnClickListener
            }

            val process = processes[index]
            val nowIso = nowIsoUtc()

            val newProduct = ProductCreateDto(
                processId = process.id,
                serialNumber = binding.tvSerial.text.toString(),
                createdAt = nowIso
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.createProduct(newProduct)

                if (_binding == null) return@launch

                result.onSuccess {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.scannerFragment, false)
                        .build()

                    findNavController().navigate(
                        R.id.action_newProductFragment_to_productFragment,
                        null,
                        navOptions
                    )
                }
                // onFailure — диалог покажет подписка на errorState
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
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
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }
}