package ru.faserkraft.client.presentation.inventory.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductsOverviewBinding
import ru.faserkraft.client.domain.model.ProductsOverview
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class ProductsOverviewFragment : androidx.fragment.app.Fragment() {

    private val viewModel: ProductsOverviewViewModel
            by hiltNavGraphViewModels(R.id.inventoryContainerFragment)

    private var _binding: FragmentProductsOverviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsOverviewAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupEmptyObserver()
        observeState()

        binding.swipeRefreshStats.setOnRefreshListener {
            viewModel.loadProductsOverview()
        }

        viewModel.loadProductsOverview()
    }

    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvProductsStats.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsOverviewAdapter { item ->
            if (_binding == null) return@ProductsOverviewAdapter
            viewModel.selectOverviewItem(item)
            findNavController().navigateSafely(
                _root_ide_package_.ru.faserkraft.client.presentation.inventory.InventoryContainerFragmentDirections.Companion
                    .actionInventoryContainerFragmentToProductsOverviewByProcessFragment()
            )
        }
        binding.rvProductsStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsStats.adapter = adapter
    }

    private fun setupEmptyObserver() {
        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = updateEmptyView()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = updateEmptyView()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = updateEmptyView()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        updateEmptyView()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.swipeRefreshStats.isRefreshing = state.isLoading
            b.swipeRefreshStats.isEnabled = !state.isLoading

            adapter.submitList(buildUiItems(state.productsOverview))

            state.errorMessage?.let {
                showErrorSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    // ---------- Группировка ----------

    private fun buildUiItems(list: List<ProductsOverview>): List<ProductsOverviewUiItem> {
        if (list.isEmpty()) return emptyList()
        val grouped = list
            .sortedWith(compareBy({ it.processName }, { it.stepDefinitionId }))
            .groupBy { it.processName }

        return buildList {
            grouped.forEach { (processName, items) ->
                add(ProductsOverviewUiItem.ProcessHeader(processName))
                items.forEach { add(ProductsOverviewUiItem.StageItem(it)) }
            }
        }
    }

    // ---------- Вспомогательное ----------

    private fun updateEmptyView() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyInventory.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvProductsStats.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}