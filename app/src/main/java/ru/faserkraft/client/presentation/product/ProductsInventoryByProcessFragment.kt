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
import ru.faserkraft.client.databinding.FragmentProductsInventoryByProcessBinding
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.showErrorSnackbar

class ProductsInventoryByProcessFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductsInventoryByProcessBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsInventoryByProcessAdapter
    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsInventoryByProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        observeState()
        observeEvents()
        renderHeader()
        loadData()

        binding.swipeRefreshDetail.setOnRefreshListener { loadData() }
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsInventoryByProcessAdapter { serialNumber ->
            viewModel.loadProduct(serialNumber)
            // Навигация произойдёт через ProductEvent.NavigateToProduct
        }
        binding.rvProductsDetail.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsDetail.adapter = adapter
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.swipeRefreshDetail.isRefreshing = state.isLoading
            b.swipeRefreshDetail.isEnabled = !state.isLoading

            val inventoryItem = state.selectedInventoryItem ?: return@collectFlow
            val items = state.productsInventoryByProcess.map { product ->
                val step = product.steps.find {
                    it.definition.id == inventoryItem.stepDefinitionId
                }
                ProductsInventoryByProcessUiItem(
                    id = product.id,
                    serialNumber = product.serialNumber,
                    createdAt = step?.performedAt.orEmpty(),
                )
            }
            adapter.submitList(items)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.NavigateToProduct ->
                    findNavController().navigate(
                        ProductsInventoryByProcessFragmentDirections
                            .actionProductsInventoryByProcessFragmentToProductFullFragment()
                    )
                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Header ----------

    private fun renderHeader() {
        val item = viewModel.uiState.value.selectedInventoryItem ?: return
        binding.tvProcessName.text = item.processName
        binding.tvStageName.text = item.stepName
    }

    // ---------- Load ----------

    private fun loadData() {
        val item = viewModel.uiState.value.selectedInventoryItem ?: return
        adapter.submitList(emptyList())
        viewModel.loadProductsByLastStep(item.processId, item.stepDefinitionId)
    }


    override fun onDestroyView() {
        binding.rvProductsDetail.adapter = null
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }
}