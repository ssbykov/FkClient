package ru.faserkraft.client.presentation.plan

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentEmployeePlanProductsBinding
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.presentation.inventory.overview.ProductsOverviewByProcessAdapter
import ru.faserkraft.client.presentation.inventory.overview.ProductsOverviewByProcessUiItem
import ru.faserkraft.client.presentation.product.detail.ProductEvent
import ru.faserkraft.client.presentation.product.detail.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.convertDate
import ru.faserkraft.client.utils.ext.navigateSafely

class EmployeePlanProductsFragment : Fragment() {

    private val planViewModel: PlanViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentEmployeePlanProductsBinding? = null
    private val binding get() = _binding!!

    private var plan: DailyPlan? = null
    private var step: DailyPlanStep? = null

    private val adapter = ProductsOverviewByProcessAdapter { serialNumber ->
        if (_binding == null) return@ProductsOverviewByProcessAdapter
        if (productViewModel.uiState.value.isLoading || planViewModel.uiState.value.isLoading) return@ProductsOverviewByProcessAdapter
        productViewModel.loadProduct(serialNumber)
    }

    private var activeDialog: AlertDialog? = null

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEmployeePlanProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val state = planViewModel.uiState.value
        plan = state.selectedPlan
        step = state.selectedStep

        setupRecycler()
        renderHeader()
        observeState()
        observeProductState()
        observeEvents()
        observeProductEvents()
        setupRefresh()
        loadData()
    }

    override fun onDestroyView() {
        binding.rvProductsDetail.adapter = null
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupRecycler() {
        binding.rvProductsDetail.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@EmployeePlanProductsFragment.adapter
        }
    }

    private fun renderHeader() {
        val currentPlan = plan
        val currentStep = step

        if (currentPlan == null || currentStep == null) return

        val planDateUi = convertDate(currentPlan.date)
        binding.tvDetailTitle.text = getString(R.string.employee_plan_products, planDateUi)
        binding.tvProcessName.text = currentStep.workProcess
        binding.tvEmployeeName.text = currentPlan.employee.name
        binding.tvStageName.text = currentStep.stepDefinition.name
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(planViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            updateLoadingState()

            val currentStep = step ?: return@collectFlow
            adapter.submitList(state.filteredProducts.toUiItems(currentStep.stepDefinitionId))
        }
    }

    private fun observeProductState() {
        collectFlow(productViewModel.uiState) {
            updateLoadingState()
        }
    }

    private fun updateLoadingState() {
        val b = _binding ?: return
        val isPlanLoading = planViewModel.uiState.value.isLoading
        val isProductLoading = productViewModel.uiState.value.isLoading

        val isSwipeRefreshing = b.swipeRefreshDetail.isRefreshing
        if (isSwipeRefreshing && !isPlanLoading) {
            b.swipeRefreshDetail.isRefreshing = false
        }

        val showCenterProgress = (isPlanLoading && !isSwipeRefreshing) || isProductLoading
        b.progressBar.isVisible = showCenterProgress
        b.rvProductsDetail.isEnabled = !showCenterProgress
    }

    private fun observeEvents() {
        collectFlow(planViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is PlanEvent.ShowError -> showDialog(event.message)
            }
        }
    }

    private fun observeProductEvents() {
        collectFlow(productViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow

            when (event) {
                is ProductEvent.NavigateToProduct -> {
                    findNavController().navigateSafely(
                        R.id.action_employeePlanProductsFragment_to_productFullFragment
                    )
                }

                is ProductEvent.ShowError -> {
                    showDialog(event.message)
                }

                else -> Unit
            }
        }
    }

    // ---------- Load ----------

    private fun loadData() {
        val currentPlan = plan ?: return
        val currentStep = step ?: return

        planViewModel.loadProductsByStepEmployeeDay(
            stepDefinitionId = currentStep.stepDefinitionId,
            day = currentPlan.date,
            employeeId = currentPlan.employee.id,
        )
    }

    // ---------- SwipeRefresh ----------

    private fun setupRefresh() {
        binding.swipeRefreshDetail.setOnRefreshListener { loadData() }
    }

    // ---------- Маппинг ----------

    private fun List<Product>.toUiItems(stepDefinitionId: Int) = map { product ->
        val performedAt = product.steps
            .find { it.definition.id == stepDefinitionId }
            ?.performedAt
            .orEmpty()
        ProductsOverviewByProcessUiItem(
            id = product.id,
            serialNumber = product.serialNumber,
            createdAt = performedAt,
        )
    }

    // ---------- Dialog ----------

    private fun showDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }
}