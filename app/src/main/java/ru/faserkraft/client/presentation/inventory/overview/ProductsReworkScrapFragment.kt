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
import ru.faserkraft.client.databinding.FragmentProductsReworkScrapBinding
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class ProductsReworkScrapFragment : androidx.fragment.app.Fragment() {

    private val viewModel: ProductsOverviewViewModel
            by hiltNavGraphViewModels(R.id.inventoryContainerFragment)

    private var _binding: FragmentProductsReworkScrapBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsReworkScrapAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsReworkScrapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupEmptyObserver()
        observeState()

        binding.swipeRefreshReworkScrap.setOnRefreshListener {
            viewModel.loadReworkScrapProducts()
        }
        viewModel.loadReworkScrapProducts()
    }

    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvReworkScrap.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsReworkScrapAdapter { statItem ->
            if (_binding == null) return@ProductsReworkScrapAdapter

            viewModel.selectReworkScrapProduct(statItem.processName, statItem.status)

            findNavController().navigateSafely(
                _root_ide_package_.ru.faserkraft.client.presentation.inventory.InventoryContainerFragmentDirections.Companion
                    .actionInventoryContainerFragmentToProductsReworkScrapListFragment()
            )
        }
        binding.rvReworkScrap.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReworkScrap.adapter = adapter
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

            b.swipeRefreshReworkScrap.isRefreshing = state.isLoading
            b.swipeRefreshReworkScrap.isEnabled = !state.isLoading

            adapter.submitList(buildUiItems(state.reworkScrapProducts))

            state.errorMessage?.let {
                showErrorSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    // ---------- Группировка ----------

    private fun buildUiItems(products: List<Product>): List<ReworkScrapUiItem> {
        if (products.isEmpty()) return emptyList()

        val groupedByProcess = products
            .sortedBy { it.process.name }
            .groupBy { it.process.name }

        return buildList {
            groupedByProcess.forEach { (processName, processProducts) ->
                add(ReworkScrapUiItem.ProcessHeader(processName))

                val groupedByStatus = processProducts.groupBy { it.status }
                groupedByStatus.toSortedMap().forEach { (status, statusProducts) ->
                    add(
                        ReworkScrapUiItem.StatusStatItem(
                            processName = processName,
                            status = status,
                            count = statusProducts.size
                        )
                    )
                }
            }
        }
    }

    // ---------- Вспомогательное ----------

    private fun updateEmptyView() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyReworkScrap.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvReworkScrap.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}