package ru.faserkraft.client.presentation.inventory.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductsOverviewByProcessBinding
import ru.faserkraft.client.presentation.product.detail.ProductEvent
import ru.faserkraft.client.presentation.product.detail.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class ProductsOverviewByProcessFragment : androidx.fragment.app.Fragment() {

    private val productsOverviewViewModel: ProductsOverviewViewModel
            by hiltNavGraphViewModels(R.id.inventoryContainerFragment)

    private val productViewModel: ProductViewModel
            by activityViewModels()

    private var _binding: FragmentProductsOverviewByProcessBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsOverviewByProcessAdapter

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsOverviewByProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        observeState()
        observeProductState()
        observeEvents()
        renderHeader()
        loadData()

        binding.swipeRefreshDetail.setOnRefreshListener { loadData() }
    }

    override fun onDestroyView() {
        binding.rvProductsDetail.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsOverviewByProcessAdapter { serialNumber ->
            if (_binding == null) return@ProductsOverviewByProcessAdapter
            productViewModel.loadProduct(serialNumber)
        }
        binding.rvProductsDetail.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsDetail.adapter = adapter
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(productsOverviewViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            updateLoadingState()

            val inventoryItem = state.selectedOverviewItem ?: return@collectFlow
            val items = state.productsOverviewByProcess.map { product ->
                val step = product.steps.find {
                    it.definition.id == inventoryItem.stepDefinitionId
                }
                ProductsOverviewByProcessUiItem(
                    id = product.id,
                    serialNumber = product.serialNumber,
                    createdAt = step?.performedAt.orEmpty(),
                )
            }
            adapter.submitList(items)

            state.errorMessage?.let {
                showErrorSnackbar(it)
                productsOverviewViewModel.clearError()
            }
        }
    }

    private fun observeProductState() {
        collectFlow(productViewModel.uiState) {
            updateLoadingState()
        }
    }

    private fun updateLoadingState() {
        val b = _binding ?: return
        val isOverviewLoading = productsOverviewViewModel.uiState.value.isLoading
        val isProductLoading = productViewModel.uiState.value.isLoading

        val isSwipeRefreshing = b.swipeRefreshDetail.isRefreshing
        if (isSwipeRefreshing && !isOverviewLoading) {
            b.swipeRefreshDetail.isRefreshing = false
        }

        val showCenterProgress = (isOverviewLoading && !isSwipeRefreshing) || isProductLoading
        b.progressBar.isVisible = showCenterProgress
    }

    private fun observeEvents() {
        collectFlow(productViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is ProductEvent.NavigateToProduct -> {
                    findNavController().navigateSafely(
                        ProductsOverviewByProcessFragmentDirections
                            .actionProductsOverviewByProcessFragmentToProductFullFragment()
                    )
                }

                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Header ----------

    private fun renderHeader() {
        val item = productsOverviewViewModel.uiState.value.selectedOverviewItem ?: return
        binding.tvProcessName.text = item.processName
        binding.tvStageName.text = item.stepName
    }

    // ---------- Load ----------

    private fun loadData() {
        val item = productsOverviewViewModel.uiState.value.selectedOverviewItem ?: return
        productsOverviewViewModel.loadProductsByLastStep(item.processId, item.stepDefinitionId)
    }
}