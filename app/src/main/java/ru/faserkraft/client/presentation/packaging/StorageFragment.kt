package ru.faserkraft.client.presentation.packaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.databinding.FragmentProductsStorageBinding
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely

class StorageFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentProductsStorageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsStorageAdapter

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        setupListeners()
        observeState()

        viewModel.loadPackagingInStorage()
    }

    override fun onDestroyView() {
        binding.rvProductsStats.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup & Observe ----------

    private fun setupAdapter() {
        adapter = ProductsStorageAdapter { item ->
            if (_binding == null) return@ProductsStorageAdapter
            val action = StorageContainerFragmentDirections
                .actionStorageContainerFragmentToPackagingListFragment(item.process)

            findNavController().navigateSafely(action)
        }
    }

    private fun setupRecyclerView() {
        binding.rvProductsStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsStats.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefreshStats.setOnRefreshListener {
            viewModel.loadPackagingInStorage()
        }
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            val isSwipeRefreshing = b.swipeRefreshStats.isRefreshing
            if (isSwipeRefreshing && !state.isLoading) {
                b.swipeRefreshStats.isRefreshing = false
            }

            b.progressBar.isVisible = state.isLoading && !isSwipeRefreshing

            val uiList = mapToUiItems(state.packagingInStorage)
            adapter.submitList(uiList) { checkEmpty() }
        }
    }

    // ---------- Helpers ----------

    private fun mapToUiItems(list: List<Packaging>): List<ProductsStorageUiItem> {
        if (list.isEmpty()) return emptyList()

        return list
            .flatMap { it.products }
            .groupBy { it.process.id to it.process.name }
            .map { (key, products) ->
                val (processId, processName) = key
                val packagingCount = list.count { packaging ->
                    packaging.products.any { it.process.id == processId }
                }
                ProductsStorageUiItem(
                    id = processId,
                    process = processName,
                    productCount = products.size,
                    packagingCount = packagingCount
                )
            }
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0 && !b.progressBar.isVisible
        b.tvEmptyStorage.isVisible = isEmpty
        b.rvProductsStats.isVisible = !isEmpty
    }
}