package ru.faserkraft.client.presentation.packaging

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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ModuleTypeUi
import ru.faserkraft.client.adapter.PackagingListAdapter
import ru.faserkraft.client.adapter.PackagingListUiItem
import ru.faserkraft.client.databinding.FragmentPackagingListBinding
import ru.faserkraft.client.utils.showErrorSnackbar

class PackagingListFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentPackagingListBinding? = null
    private val binding get() = _binding!!

    private val args: PackagingListFragmentArgs by navArgs()
    private lateinit var adapter: PackagingListAdapter

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

        setupRecyclerView(process)
        observeState(process)
        observeEvents()

        viewModel.loadPackagingInStorage()
    }

    private fun setupRecyclerView(process: String) {
        adapter = PackagingListAdapter(
            onItemClick = { item ->
                viewModel.loadPackaging(item.serialNumber)
            }
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
    }

    private fun observeState(process: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

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
                    b.tvEmptyPackaging.visibility = if (uiItems.isEmpty()) View.VISIBLE else View.GONE
                    b.rvProducts.visibility = if (uiItems.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PackagingEvent.ShowError -> showErrorSnackbar(event.message)
                        PackagingEvent.NavigateToPackaging ->
                            findNavController().navigate(
                                R.id.action_packagingListFragment_to_packagingFragment
                            )
                        PackagingEvent.NavigateToNewPackaging,
                        PackagingEvent.NavigateToEdit,
                        PackagingEvent.PackagingDeleted -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvProducts.adapter = null
        _binding = null
    }
}