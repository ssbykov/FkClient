package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.ModuleTypeUi
import ru.faserkraft.client.adapter.PackagingListAdapter
import ru.faserkraft.client.adapter.PackagingListUiItem
import ru.faserkraft.client.databinding.FragmentPackagingListBinding
import ru.faserkraft.client.dto.FinishedProductDto
import ru.faserkraft.client.viewmodel.ScannerViewModel


class PackagingListFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentPackagingListBinding

    private val args: PackagingListFragmentArgs by navArgs()

    private lateinit var adapter: PackagingListAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPackagingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val process = args.process
        binding.tvProcess.text = process

        adapter = PackagingListAdapter()

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        viewModel.packagingBoxes.observe(viewLifecycleOwner) { packaging ->

            val uiItems: List<PackagingListUiItem> = packaging
                .orEmpty()
                .filter { box ->
                    box.products.any { it.process.name == process }
                }
                .map { box ->
                    // группируем продукты по имени процесса
                    val groups: Map<String, List<FinishedProductDto>> =
                        box.products.groupBy { it.process.name }

                    val types = groups.map { (name, list) ->
                        ModuleTypeUi(
                            name = name,
                            count = list.size
                        )
                    }

                    PackagingListUiItem(
                        id = box.id,
                        serialNumber = box.serialNumber,
                        totalCount = box.products.size,
                        types = types
                    )
                }

            adapter.submitList(uiItems)

            // Показ/скрытие сообщения об отсутствии упаковок
            if (uiItems.isEmpty()) {
                binding.tvEmptyPackaging.visibility = View.VISIBLE
                binding.rvProducts.visibility = View.GONE
            } else {
                binding.tvEmptyPackaging.visibility = View.GONE
                binding.rvProducts.visibility = View.VISIBLE
            }
        }


        // начальная загрузка
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPackagingInStorage()
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

        // FAB – действие при нажатии (например, открыть экран упаковки для отправки)
        binding.fabAdd.setOnClickListener {
            // Вариант 1: перейти к PackagingShipmentFragment с тем же process
            val action =
                PackagingListFragmentDirections.actionPackagingListFragmentToPackagingShipmentFragment(
                    process
                )
            findNavController().navigate(action)
        }
    }
}
