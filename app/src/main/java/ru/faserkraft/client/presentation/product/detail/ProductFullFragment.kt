package ru.faserkraft.client.presentation.product.detail

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductFullBinding
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatIsoToUi
import ru.faserkraft.client.utils.ext.navigateSafely

class ProductFullFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductFullBinding? = null
    private val binding get() = _binding!!

    private lateinit var stepsAdapter: StepsAdapter

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductFullBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupStepsList()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        binding.rvSteps.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // 1. Управление видимостью прогресс-бара
            b.progressBar.isVisible = state.isLoading || state.isActionInProgress

            val product = state.product ?: return@collectFlow

            // 2. Отображение основных данных продукта
            b.tvProductNumber.text =
                getString(R.string.product_serial_number, product.serialNumber)
            b.tvTechProcess.text =
                getString(R.string.product_tech_process, product.process.name)
            b.tvStartAt.text =
                getString(R.string.product_start_at, formatIsoToUi(product.createdAt))

            val uiStatus = product.status.toUiProductStatus()
            val ctx = b.root.context
            val bgColor = ContextCompat.getColor(ctx, uiStatus.bgColorRes)
            val textColor = ContextCompat.getColor(ctx, uiStatus.textColorRes)

            b.chipProductStatus.text = getString(uiStatus.titleRes)
            b.chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            b.chipProductStatus.setTextColor(textColor)
            b.cardProduct.setCardBackgroundColor(bgColor)

            // 3. Условия доступности редактирования и сброса шагов
            val isPrivileged = state.userRole == UserRole.MASTER || state.userRole == UserRole.ADMIN
            val isProductNotPacked = product.packagingSerialNumber == null
            val isProductNormal = product.status == ProductStatus.NORMAL

            val stepItems = product.steps.map { step ->
                StepUiItem(
                    isEditable = isPrivileged && isProductNotPacked && isProductNormal && step.status == StepStatus.DONE,
                    step = step,
                )
            }
            stepsAdapter.submitList(stepItems)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            val b = _binding ?: return@collectFlow
            when (event) {
                is ProductEvent.ShowError -> {
                    Snackbar.make(b.root, event.message, Snackbar.LENGTH_LONG).show()
                }

                is ProductEvent.ShowConfirmationDialog -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(event.title)
                        .setMessage(event.message)
                        .setPositiveButton(R.string.save) { _, _ ->
                            viewModel.onDialogConfirmed(event.actionType, event.step)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }

                else -> Unit
            }
        }
    }

    // ---------- Setup ----------

    private fun setupStepsList() {
        stepsAdapter = StepsAdapter { stepUiItem, action ->
            when (action) {
                StepAction.CHANGE_PERFORMER -> {
                    viewModel.selectStep(stepUiItem.step)
                    viewModel.loadEmployees()
                    findNavController().navigateSafely(
                        ProductFullFragmentDirections.actionProductFullFragmentToEditStepFragment()
                    )
                }

                StepAction.RESET_STEP -> {
                    showResetStepConfirmDialog(stepUiItem.step)
                }
            }
        }

        binding.rvSteps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSteps.adapter = stepsAdapter
    }

    private fun showResetStepConfirmDialog(step: Step) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_step_title)
            .setMessage(getString(R.string.reset_step_confirm_message, step.definition.name))
            .setPositiveButton(R.string.reset) { _, _ ->
                viewModel.resetStep(step.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}