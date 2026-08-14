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
import ru.faserkraft.client.domain.model.ProductInventoryCompareItem
import ru.faserkraft.client.presentation.product.detail.ProductEvent
import ru.faserkraft.client.presentation.product.detail.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely

// Единый enum для всех статусов расхождений
enum class CompareStatus {
    MATCHED, MISSING, UNEXPECTED, STEP_MISMATCH
}

// Extension-свойство для вычисления статуса элемента
val ProductInventoryCompareItem.compareStatus: CompareStatus
    get() = when {
        inventoryStepDefinition == null && accountingStepDefinition != null -> CompareStatus.MISSING
        inventoryStepDefinition != null && accountingStepDefinition == null -> CompareStatus.UNEXPECTED
        inventoryStepDefinition != null && accountingStepDefinition != null && inventoryStepDefinition.id != accountingStepDefinition.id -> CompareStatus.STEP_MISMATCH
        else -> CompareStatus.MATCHED
    }


@AndroidEntryPoint
class InventoryResultsFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private val productViewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentInventoryResultsBinding? = null
    private val binding get() = _binding!!

    // Адаптер для отображения карточек продуктов
    private val adapter = InventoryProductResultsAdapter(
        onItemClick = ::onProductClick
    )

    // Исходный список от API
    private var allProducts: List<ProductInventoryCompareItem> = emptyList()

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

    // Централизованная функция переключения фильтров между тремя группами
    private fun checkFilter(checkedId: Int) {
        if (isUpdatingFilters) return
        isUpdatingFilters = true

        val allGroups = listOf(
            binding.toggleGroupAll,
            binding.toggleGroupStatus,
            binding.toggleGroupUnexpected
        )

        // Определяем, к какой группе принадлежит нажатая кнопка
        val targetGroup = when (checkedId) {
            R.id.btnFilterAll -> binding.toggleGroupAll
            R.id.btnFilterUnexpected -> binding.toggleGroupUnexpected
            else -> binding.toggleGroupStatus
        }

        // Обновляем визуальное состояние групп
        allGroups.forEach { group ->
            if (group == targetGroup) {
                if (group.checkedButtonId != checkedId) {
                    group.check(checkedId)
                }
            } else {
                group.clearChecked()
            }
        }

        // Определяем, какой статус искать в списке (null = показываем все)
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

    // Динамически показываем/скрываем третью строку с "Лишними"
    private fun updateUnexpectedVisibility() {
        val hasUnexpected = allProducts.any { it.compareStatus == CompareStatus.UNEXPECTED }

        binding.toggleGroupUnexpected.visibility = if (hasUnexpected) View.VISIBLE else View.GONE

        // Если пользователь выбрал фильтр "Лишние", но данные обновились и лишних больше нет —
        // сбрасываем фильтр на "Все", чтобы не показывать пустой экран
        if (!hasUnexpected && currentFilterType == CompareStatus.UNEXPECTED) {
            checkFilter(R.id.btnFilterAll)
        }
    }

    // ---------- Observers ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            allProducts = state.compareResults

            b.toolbarTitle.text =
                getString(R.string.inventory_results_title, state.currentInventory?.id)

            // Подсчет сводки по плоскому списку, опираясь на compareStatus
            val dbTotal = allProducts.count { it.accountingStepDefinition != null }
            val scannedTotal = allProducts.count { it.inventoryStepDefinition != null }
            val diffTotal = allProducts.count { it.compareStatus != CompareStatus.MATCHED }

            b.tvDbTotal.text = dbTotal.toString()
            b.tvScannedTotal.text = scannedTotal.toString()
            b.tvDiffTotal.text = diffTotal.toString()

            // Обновляем UI фильтров и применяем их
            updateUnexpectedVisibility()
            applyFilters()
        }
    }

    private fun observeEvents() {
        // События жизненного цикла продукта
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
                        "Продукт не найден", // или "Продукт не найден"
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Unit
            }
        }

        // События инвентаризации
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

            // Показываем/скрываем ProgressBar и блокируем повторные клики
            b.progressBar.isVisible = state.isLoading
            b.rvResults.isEnabled = !state.isLoading
        }
    }

    // ---------- Filtering ----------

    private fun applyFilters() {
        var filteredList = allProducts

        // 1. Применяем текстовый поиск (по серийному номеру)
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.serialNumber.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // 2. Применяем фильтр по выбранной кнопке (если не выбрана "Все")
        if (currentFilterType != null) {
            filteredList = filteredList.filter { it.compareStatus == currentFilterType }
        }

        adapter.submitList(filteredList)
    }

    // ---------- Navigation ----------

    private fun onProductClick(item: ProductInventoryCompareItem) {
        if (productViewModel.uiState.value.isLoading) return
        productViewModel.loadProduct(item.serialNumber)
    }
}