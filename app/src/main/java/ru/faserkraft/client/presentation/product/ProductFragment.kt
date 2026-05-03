package ru.faserkraft.client.presentation.product

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.formatIsoToUi
import ru.faserkraft.client.utils.showErrorSnackbar

class ProductFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
        observeEvents()
        setupClickListeners()
    }

    // ---------- Observe ----------

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
            b.btnDone.isEnabled = !state.isActionInProgress

            val product = state.product ?: return@collectFlow
            b.tvProcess.text = product.process.name
            b.tvProductNumber.text = product.serialNumber
            b.tvCreated.text = formatIsoToUi(product.createdAt)

            val uiStatus = product.status.toUiProductStatus()
            val ctx = b.root.context
            val bgColor = ContextCompat.getColor(ctx, uiStatus.bgColorRes)
            val textColor = ContextCompat.getColor(ctx, uiStatus.textColorRes)
            b.chipProductStatus.text = getString(uiStatus.titleRes)
            b.chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            b.chipProductStatus.setTextColor(textColor)
            b.cardProductInfo.setCardBackgroundColor(bgColor)

            val step = state.selectedStep ?: return@collectFlow
            val uiStepStatus = step.toUiStatus()
            b.tvStepName.text = ctx.getString(R.string.step_last_title, step.definition.name)
            b.tvStatus.text = ctx.getString(uiStepStatus.statusTitleRes)
            b.tvCompletedAt.text = ctx.getString(uiStepStatus.statusDescRes)
            b.cardRoot.setBackgroundColor(
                ContextCompat.getColor(ctx, uiStepStatus.bgColorRes)
            )
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.NavigateToNewProduct ->
                    findNavController().navigate(
                        R.id.action_scannerFragment_to_newProductFragment
                    )
                is ProductEvent.NavigateToProduct -> Unit
                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
            }
        }
    }

    // ---------- Clicks ----------

    private fun setupClickListeners() {
        binding.btnAllStages.setOnClickListener {
            findNavController().navigate(R.id.action_productFragment_to_productFullFragment)
        }

        binding.chipProductStatus.setOnClickListener {
            if (!viewModel.uiState.value.userRole.canEditProduct()) return@setOnClickListener
            showConfirmDialog("Изменение статуса", "Вы уверены?") {
                findNavController().navigate(
                    R.id.action_productFragment_to_editProductStatusFragment
                )
            }
        }

        binding.btnEdit.setOnClickListener {
            if (!viewModel.uiState.value.userRole.canEditProduct()) return@setOnClickListener
            showConfirmDialog("Изменение процесса", "Вы уверены?") {
                viewModel.loadProcesses()
                findNavController().navigate(
                    R.id.action_productFragment_to_editProductFragment
                )
            }
        }

        binding.btnDone.setOnClickListener {
            val state = viewModel.uiState.value
            val product = state.product ?: return@setOnClickListener
            val step = state.selectedStep ?: return@setOnClickListener
            if (step.id == 0) return@setOnClickListener

            when {
                product.status == ProductStatus.REWORK ||
                        product.status == ProductStatus.SCRAP ->
                    showErrorSnackbar("Нельзя закрыть этап: продукт в статусе РЕМОНТ или БРАК")

                step.status == StepStatus.DONE ->
                    showErrorSnackbar("Этап уже выполнен")

                else -> showConfirmDialog("Закрыть этап", "Вы уверены?") {
                    viewModel.closeStep(step)
                }
            }
        }
    }

    // ---------- Dialogs ----------

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { d, _ -> onConfirm(); d.dismiss(); activeDialog = null }
            .setNegativeButton("Отмена") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }
}

// Extension в том же файле или в ProductUiMappers.kt
private fun UserRole?.canEditProduct() =
    this == UserRole.ADMIN || this == UserRole.MASTER