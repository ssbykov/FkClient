package ru.faserkraft.client.presentation.product

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.adapter.ProductsInventoryAdapter
import ru.faserkraft.client.adapter.ProductsInventoryUiItem
import ru.faserkraft.client.databinding.FragmentProductsInventoryBinding
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.showErrorSnackbar

class ProductsInventoryFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductsInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsInventoryAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupEmptyObserver()
        observeState()
        observeEvents()

        binding.swipeRefreshStats.setOnRefreshListener {
            viewModel.loadProductsInventory()
        }

        viewModel.loadProductsInventory()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsInventoryAdapter { item ->
            if (_binding == null) return@ProductsInventoryAdapter
            viewModel.selectInventoryItem(item)   // ← сохраняем в state
            findNavController().navigate(
                ProductsInventoryFragmentDirections
                    .actionProductsInventoryFragmentToProductsInventoryByProcessFragment()
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

            val uiItems = buildUiItems(state.productsInventory)
            adapter.submitList(uiItems)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Группировка ----------

    private fun buildUiItems(list: List<ProductsInventory>): List<ProductsInventoryUiItem> {
        if (list.isEmpty()) return emptyList()
        val grouped = list
            .sortedWith(compareBy({ it.processName }, { it.stepDefinitionId }))
            .groupBy { it.processName }

        return buildList {
            grouped.forEach { (processName, items) ->
                add(ProductsInventoryUiItem.ProcessHeader(processName))
                items.forEach { add(ProductsInventoryUiItem.StageItem(it)) }
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


    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvProductsStats.adapter = null
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }
}