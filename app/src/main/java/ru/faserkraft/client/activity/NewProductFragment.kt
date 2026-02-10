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

    private lateinit var binding: FragmentNewProductBinding

    private lateinit var processAdapter: ProcessAdapter
    private var processes: List<ProcessUi> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        var selectedIndex: Int? = null

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
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

        binding.btnCreate.setOnClickListener {

            val index = selectedIndex
            if (index == null || index < 0 || index >= processes.size) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Процесс не выбран")
                    .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                    .show()
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

                result.onSuccess {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.scannerFragment, false)
                        .build()

                    findNavController().navigate(
                        R.id.action_newProductFragment_to_productFragment,
                        null,
                        navOptions
                    )
                }.onFailure {
                    // тут просто ничего не делаем, диалог покажет подписка на errorState
                }
            }
        }
    }

}