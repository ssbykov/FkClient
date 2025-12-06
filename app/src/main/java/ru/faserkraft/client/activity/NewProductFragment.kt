package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.databinding.FragmentNewProductBinding
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

        viewModel.processes.observe(viewLifecycleOwner) { list ->
            processes = list
                ?.map { ProcessUi(it.id, it.name) }
                .orEmpty()
            processAdapter.setItems(processes)
        }

        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
            val selected = processes.getOrNull(position)
//            viewModel.onProcessSelected(selected?.id)
        }
    }

}