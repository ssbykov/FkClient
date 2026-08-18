package ru.faserkraft.client.presentation.packaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.databinding.FragmentPackagingListBinding
import ru.faserkraft.client.presentation.common.adapter.PackagingListAdapter
import ru.faserkraft.client.presentation.common.adapter.PackagingListUiItem
import ru.faserkraft.client.presentation.order.ModuleTypeUi
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class PackagingListFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentPackagingListBinding? = null
    private val binding get() = _binding!!

    private val args: PackagingListFragmentArgs by navArgs()
    private lateinit var adapter: PackagingListAdapter

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPackagingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val process = args.process
        binding.tvProcess.text = process

        setupRecyclerView()
        observeState(process)
        observeEvents()

        viewModel.loadPackagingInStorage()
    }

    override fun onDestroyView() {
        binding.rvProducts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup & Observe ----------

    private fun setupRecyclerView() {
        adapter = PackagingListAdapter(
            onItemClick = { item ->
                if (_binding == null) return@PackagingListAdapter
                // Загружаем упаковку во ViewModel перед переходом
                viewModel.loadPackaging(item.serialNumber)
            }
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
    }

    private fun observeState(process: String) {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.progressBar.isVisible = state.isLoading

            val uiItems = state.packagingInStorage
                .filter { box ->
                    box.products.any { it.process.name == process }
                }
                .map { box ->
                    val groups = box.products.groupBy { it.process.name }
                    PackagingListUiItem(
                        id = box.id,
                        serialNumber = box.serialNumber,
                        totalCount = box.products.size,
                        types = groups.map { (name, list) ->
                            ModuleTypeUi(name = name, count = list.size)
                        }
                    )
                }

            adapter.submitList(uiItems)

            val isEmpty = uiItems.isEmpty() && !state.isLoading
            b.tvEmptyPackaging.isVisible = isEmpty
            b.rvProducts.isVisible = !isEmpty
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is PackagingEvent.ShowError -> showErrorSnackbar(event.message)
                PackagingEvent.NavigateToPackaging -> {
                    val action =
                        PackagingListFragmentDirections.actionPackagingListFragmentToPackagingFragment(
                            null
                        )
                    findNavController().navigateSafely(action)
                }

                PackagingEvent.NavigateToNewPackaging,
                PackagingEvent.NavigateToEdit,
                PackagingEvent.PackagingDeleted -> Unit
            }
        }
    }
}