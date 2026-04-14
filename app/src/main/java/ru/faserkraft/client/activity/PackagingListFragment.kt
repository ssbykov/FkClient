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
import ru.faserkraft.client.dto.FinishedProductDto
import ru.faserkraft.client.viewmodel.ScannerViewModel

class PackagingListFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentPackagingListBinding? = null
    private val binding get() = _binding!!

    private val args: PackagingListFragmentArgs by navArgs()
    private lateinit var adapter: PackagingListAdapter

    private var activeDialog: AlertDialog? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPackagingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val process = args.process
        binding.tvProcess.text = process

        adapter = PackagingListAdapter(
            onItemClick = { item ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.handlePackagingSerialQr(item.serialNumber)

                    if (_binding == null) return@launch

                    findNavController().navigate(
                        R.id.action_packagingListFragment_to_packagingFragment
                    )
                }
            }
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        viewModel.packagingBoxes.observe(viewLifecycleOwner) { packaging ->
            val b = _binding ?: return@observe

            val uiItems: List<PackagingListUiItem> = packaging
                .orEmpty()
                .filter { box -> box.products.any { it.process.name == process } }
                .map { box ->
                    val groups: Map<String, List<FinishedProductDto>> =
                        box.products.groupBy { it.process.name }

                    val types = groups.map { (name, list) ->
                        ModuleTypeUi(name = name, count = list.size)
                    }

                    PackagingListUiItem(
                        id = box.id,
                        serialNumber = box.serialNumber,
                        totalCount = box.products.size,
                        types = types
                    )
                }

            adapter.submitList(uiItems)

            b.tvEmptyPackaging.visibility = if (uiItems.isEmpty()) View.VISIBLE else View.GONE
            b.rvProducts.visibility = if (uiItems.isEmpty()) View.GONE else View.VISIBLE
        }

        // начальная загрузка
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPackagingInStorage()
        }

        // обработка ошибок
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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        binding.rvProducts.adapter = null  // 🟡 ИСПРАВЛЕНИЕ 4
        _binding = null
    }
}