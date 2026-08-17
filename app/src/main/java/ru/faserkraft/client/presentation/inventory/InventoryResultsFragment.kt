package ru.faserkraft.client.presentation.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentInventoryResultsBinding
import ru.faserkraft.client.domain.model.StepDefinitionWithProcess
import ru.faserkraft.client.presentation.product.detail.ProductEvent
import ru.faserkraft.client.presentation.product.detail.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely

@AndroidEntryPoint
class InventoryResultsFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentInventoryResultsBinding? = null
    private val binding get() = _binding!!

    private val adapter = InventoryProductResultsAdapter(
        onItemClick = ::onProductClick,
        onAcceptAccountingStep = { item, step ->
            // viewModel.resolveProductDiscrepancy(item.domainItem, step)
        },
        onAcceptInventoryStep = { item, step ->
            // viewModel.resolveProductDiscrepancy(item.domainItem, step)
        },
        onSelectCustomStep = ::onSelectCustomStep
    )

    // Список UI-элементов, сформированный из state
    private var allUiProducts: List<ProductInventoryCompareUiItem> = emptyList()

    // Текущие фильтры (null означает выбор "Все")
    private var currentSearchQuery = ""
    private var currentFilterType: CompareStatus? = null

    // Флаг для предотвращения зацикливания при программном переключении кнопок
    private var isUpdatingFilters = false

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFilters()
        observeState()
        observeEvents()
        observeProductLoading()
    }

    override fun onDestroyView() {
        binding.rvResults.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
    }

    private fun setupSearch() {
        if (currentSearchQuery.isNotEmpty()) {
            binding.searchView.setQuery(currentSearchQuery, false)
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText?.trim() ?: ""
                applyFilters()
                return true
            }
        })
    }

    // ---------- Filters Setup & Logic ----------

    private fun setupFilters() {
        val filterButtons = listOf(
            binding.btnFilterAll,
            binding.btnFilterMatched,
            binding.btnFilterMissing,
            binding.btnFilterMismatch,
            binding.btnFilterUnexpected
        )

        filterButtons.forEach { btn ->
            btn.setOnClickListener { checkFilter(btn.id) }
        }

        val initialButtonId = when (currentFilterType) {
            CompareStatus.MATCHED -> R.id.btnFilterMatched
            CompareStatus.MISSING -> R.id.btnFilterMissing
            CompareStatus.STEP_MISMATCH -> R.id.btnFilterMismatch
            CompareStatus.UNEXPECTED -> R.id.btnFilterUnexpected
            null -> R.id.btnFilterAll
        }
        checkFilter(initialButtonId)
    }

    private fun checkFilter(checkedId: Int) {
        if (isUpdatingFilters) return
        isUpdatingFilters = true

        val allGroups = listOf(
            binding.toggleGroupAll,
            binding.toggleGroupStatus,
            binding.toggleGroupUnexpected
        )

        val targetGroup = when (checkedId) {
            R.id.btnFilterAll -> binding.toggleGroupAll
            R.id.btnFilterUnexpected -> binding.toggleGroupUnexpected
            else -> binding.toggleGroupStatus
        }

        allGroups.forEach { group ->
            if (group == targetGroup) {
                if (group.checkedButtonId != checkedId) {
                    group.check(checkedId)
                }
            } else {
                group.clearChecked()
            }
        }

        currentFilterType = when (checkedId) {
            R.id.btnFilterMatched -> CompareStatus.MATCHED
            R.id.btnFilterMissing -> CompareStatus.MISSING
            R.id.btnFilterMismatch -> CompareStatus.STEP_MISMATCH
            R.id.btnFilterUnexpected -> CompareStatus.UNEXPECTED
            else -> null
        }

        isUpdatingFilters = false
        applyFilters()
    }

    private fun updateUnexpectedVisibility() {
        val hasUnexpected = allUiProducts.any { it.compareStatus == CompareStatus.UNEXPECTED }

        binding.toggleGroupUnexpected.isVisible = hasUnexpected

        if (!hasUnexpected && currentFilterType == CompareStatus.UNEXPECTED) {
            checkFilter(R.id.btnFilterAll)
        }
    }

    // ---------- Observers ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // Маппим доменные элементы в UI-модели (с сохранением локально разрешенного этапа, если есть)
            allUiProducts = state.compareResults.map { it.toUiItem() }

            b.toolbarTitle.text =
                getString(R.string.inventory_results_title, state.currentInventory?.id)

            // Подсчет сводки по UI-списку
            val dbTotal = allUiProducts.count { it.accountingStep != null }
            val scannedTotal = allUiProducts.count { it.inventoryStep != null }
            val diffTotal = allUiProducts.count { it.isMismatch }

            b.tvDbTotal.text = dbTotal.toString()
            b.tvScannedTotal.text = scannedTotal.toString()
            b.tvDiffTotal.text = diffTotal.toString()

            updateUnexpectedVisibility()
            applyFilters()
        }
    }

    private fun observeEvents() {
        collectFlow(productViewModel.events) { event ->
            when (event) {
                is ProductEvent.NavigateToProduct -> {
                    findNavController().navigateSafely(
                        InventoryResultsFragmentDirections
                            .actionInventoryResultsFragmentToProductFullFragment()
                    )
                }

                is ProductEvent.NavigateToNewProduct -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.product_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Unit
            }
        }

        collectFlow(viewModel.events) { event ->
            when (event) {
                is InventoryEvent.ShowError -> {
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                }

                else -> Unit
            }
        }
    }

    private fun observeProductLoading() {
        collectFlow(productViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            b.progressBar.isVisible = state.isLoading
            b.rvResults.isEnabled = !state.isLoading
        }
    }

    // ---------- Filtering ----------

    private fun applyFilters() {
        var filteredList = allUiProducts

        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.serialNumber.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        if (currentFilterType != null) {
            filteredList = filteredList.filter { it.compareStatus == currentFilterType }
        }

        adapter.submitList(filteredList)
    }

    // ---------- Actions & Navigation ----------

    private fun onProductClick(item: ProductInventoryCompareUiItem) {
        if (productViewModel.uiState.value.isLoading) return
        productViewModel.loadProduct(item.serialNumber)
    }

    private fun onStepResolved(item: ProductInventoryCompareUiItem, selectedStep: StepDefinitionWithProcess) {
        // Передаем утвержденный этап во ViewModel для фиксации расхождения
//        viewModel.resolveProductDiscrepancy(item.domainItem, selectedStep)
    }

    private fun onSelectCustomStep(item: ProductInventoryCompareUiItem) {
        // Открытие BottomSheetDialog / диалога со справочником этапов
//        viewModel.openCustomStepSelection(item.domainItem)
    }
}