package ru.faserkraft.client.presentation.product

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatIsoToUi
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class ProductFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
        observeEvents()
        setupClickListeners()
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            val product = state.product

            val canEditByRole = state.userRole.canEditProduct()
            val isNotPackaged = product?.packagingSerialNumber == null
            val canEdit = canEditByRole && isNotPackaged

            b.progressEdit.visibility = if (state.isActionInProgress) View.VISIBLE else View.GONE
            b.btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
            b.chipProductStatus.isClickable = canEdit

            if (product == null) return@collectFlow

            b.tvProcess.text = product.process.name
            b.tvProductNumber.text = product.serialNumber
            b.tvCreated.text = formatIsoToUi(product.createdAt)

            // --- СТАТУС ---
            val uiStatus = product.status.toUiProductStatus()
            val ctx = b.root.context
            val bgColor = ContextCompat.getColor(ctx, uiStatus.bgColorRes)
            val textColor = ContextCompat.getColor(ctx, uiStatus.textColorRes)

            b.chipProductStatus.text = getString(uiStatus.titleRes)
            b.chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
            b.chipProductStatus.setTextColor(textColor)
            b.cardProductInfo.setCardBackgroundColor(bgColor)

            if (product.packagingSerialNumber != null) {
                b.chipPackaging.visibility = View.VISIBLE
                b.chipPackaging.text = product.packagingSerialNumber

                b.chipPackaging.chipBackgroundColor = ColorStateList.valueOf(bgColor)
                b.chipPackaging.setTextColor(textColor)
                b.chipPackaging.chipIconTint = ColorStateList.valueOf(textColor)

                b.chipPackaging.chipStrokeWidth = 2f
                b.chipPackaging.chipStrokeColor = ColorStateList.valueOf(textColor)
            } else {
                b.chipPackaging.visibility = View.GONE
            }

            val step = state.selectedStep ?: return@collectFlow
            val uiStepStatus = step.toUiStatus()

            b.tvStepName.text = ctx.getString(R.string.step_last_title, step.definition.name)
            b.tvStatus.text = ctx.getString(uiStepStatus.statusTitleRes)
            b.tvCompletedAt.text = ctx.getString(uiStepStatus.statusDescRes)
            b.cardRoot.setBackgroundColor(ContextCompat.getColor(ctx, uiStepStatus.bgColorRes))
        }
    }
    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.NavigateToEditProcess -> {
                    findNavController().navigateSafely(
                        R.id.action_productFragment_to_editProductFragment
                    )
                }

                is ProductEvent.NavigateToEditStatus -> {
                    findNavController().navigateSafely(
                        R.id.action_productFragment_to_editProductStatusFragment
                    )
                }

                is ProductEvent.NavigateToPackaging -> {
                    val action =
                        ProductFragmentDirections.actionProductFragmentToPackagingFragment(event.packagingSerialNumber)
                    findNavController().navigateSafely(action)
                }

                is ProductEvent.ShowError -> {
                    showErrorSnackbar(event.message)
                }

                is ProductEvent.ShowConfirmationDialog -> {
                    showConfirmDialog(title = event.title, message = event.message) {
                        viewModel.onDialogConfirmed(event.actionType, event.step)
                    }
                }

                ProductEvent.NavigateToNewProduct,
                ProductEvent.NavigateToProduct -> Unit
            }
        }
    }

    // ---------- Clicks ----------

    private fun setupClickListeners() {
        binding.btnAllStages.setOnClickListener {
            findNavController().navigate(R.id.action_productFragment_to_productFullFragment)
        }
        binding.chipProductStatus.setOnClickListener {
            viewModel.onChangeStatusClicked()
        }
        binding.btnEdit.setOnClickListener {
            viewModel.onChangeProcessClicked()
        }
        binding.btnDone.setOnClickListener {
            viewModel.onCloseStepClicked()
        }
        binding.chipPackaging.setOnClickListener {
            viewModel.onPackagingClicked()
        }
    }

    // ---------- Dialogs ----------

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { d, _ ->
                onConfirm()
                d.dismiss()
                activeDialog = null
            }
            .setNegativeButton("Отмена") { d, _ ->
                d.dismiss()
                activeDialog = null
            }
            .also { builder ->
                builder.setOnDismissListener { activeDialog = null }
            }
            .show()
    }
}