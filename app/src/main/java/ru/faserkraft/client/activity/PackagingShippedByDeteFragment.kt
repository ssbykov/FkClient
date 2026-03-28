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
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ModuleTypeUi
import ru.faserkraft.client.adapter.PackagingListAdapter
import ru.faserkraft.client.adapter.PackagingListUiItem
import ru.faserkraft.client.databinding.FragmentPackagingListBinding
import ru.faserkraft.client.databinding.FragmentShippedByDateBinding
import ru.faserkraft.client.dto.FinishedProductDto
import ru.faserkraft.client.viewmodel.ScannerViewModel


class ShippedByDateFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentShippedByDateBinding

    private val args: ShippedByDateFragmentArgs by navArgs()

    private lateinit var adapter: PackagingListAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentShippedByDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shipmentDate = args.shipmentDate
        binding.tvStatsTitle.text = shipmentDate

        adapter = PackagingListAdapter(
            onItemClick = { item ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.handlePackagingSerialQr(item.serialNumber)
                    // Используем глобальное действие
                    findNavController().navigate(R.id.action_global_packagingFragment)
                }
            }
        )

        binding.rvPackagingStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingStats.adapter = adapter

        viewModel.shippedPackaging.observe(viewLifecycleOwner) { packagingList ->
            val uiItems: List<PackagingListUiItem> = packagingList
                .orEmpty()
                .filter { box ->
                    val boxDate = (box.shipmentAt ?: "").take(10)
                    boxDate == shipmentDate
                }
                .map { box ->
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

    }
}
