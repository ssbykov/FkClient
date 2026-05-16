package ru.faserkraft.client.presentation.product

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductsReworkScrapListBinding
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class ProductsReworkScrapListFragment : Fragment() {

    private val productsViewModel: ProductsViewModel
            by hiltNavGraphViewModels(R.id.productContainerFragment)

    private val productViewModel: ProductViewModel
            by activityViewModels()

    private var _binding: FragmentProductsReworkScrapListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsReworkScrapListAdapter

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsReworkScrapListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        observeState()
        observeEvents()
        renderHeader()
        loadData()

        binding.swipeRefreshModules.setOnRefreshListener { loadData() }
    }

    override fun onDestroyView() {
        binding.rvModules.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsReworkScrapListAdapter { serialNumber ->
            productViewModel.loadProduct(serialNumber)
            // Навигация произойдёт через ProductEvent.NavigateToProduct
        }
        binding.rvModules.layoutManager = LinearLayoutManager(requireContext())
        binding.rvModules.adapter = adapter
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(productsViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.swipeRefreshModules.isRefreshing = state.isLoading
            b.swipeRefreshModules.isEnabled = !state.isLoading

            val selection = state.selectedScrapReworkItem ?: return@collectFlow

            val items = state.reworkScrapProducts
                .filter { product ->
                    product.status == selection.status &&
                            product.process.name == selection.processName
                }
                .map { product ->
                    Log.i(
                        "ProductsReworkScrapListFragment",
                        "Product: ${product.serialNumber}, Status: ${product.status}, " +
                                "Process: ${product.process.name}, ${product.createdAt}"
                    )
                    ProductModuleUiItem(
                        id = product.id,
                        serialNumber = product.serialNumber,
                        createdAt = product.createdAt,
                    )
                }

            adapter.submitList(items)

            state.errorMessage?.let {
                showErrorSnackbar(it)
                productsViewModel.clearError()
            }
        }
    }

    private fun observeEvents() {
        collectFlow(productViewModel.events) { event ->
            when (event) {
                is ProductEvent.NavigateToProduct -> {
                    findNavController().navigateSafely(
                        ProductsReworkScrapListFragmentDirections
                            .actionProductsReworkScrapListFragmentToProductFullFragment()
                    )
                }

                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Header ----------

    private fun renderHeader() {
        val selection = productsViewModel.uiState.value.selectedScrapReworkItem ?: return
        binding.tvProcessName.text = selection.processName
        val uiStatus = selection.status.toUiProductStatus()
        binding.tvStatusName.text = uiStatus.getTitle(requireContext())
    }

    // ---------- Load ----------

    private fun loadData() {
        adapter.submitList(emptyList())
        productsViewModel.loadReworkScrapProducts()
    }
}