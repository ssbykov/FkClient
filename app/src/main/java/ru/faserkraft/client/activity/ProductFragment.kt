package ru.faserkraft.client.activity

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.dto.ProductDto
import ru.faserkraft.client.dto.ProductStatus
import ru.faserkraft.client.dto.StepStatusBackend
import ru.faserkraft.client.dto.emptyStep
import ru.faserkraft.client.dto.toUiProductStatus
import ru.faserkraft.client.dto.toUiStatus
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.utils.formatIsoToUi
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ProductFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    private var userRole: UserRole? = null
    private var product: ProductDto? = null

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.lastStep.observe(viewLifecycleOwner) { lastStep ->
            with(binding) {
                val ctx = cardRoot.context

                tvStepName.text = ctx.getString(
                    R.string.step_last_title,
                    lastStep.stepDefinition.template.name
                )

                val uiStatus = lastStep.toUiStatus()
                tvStatus.text = ctx.getString(uiStatus.statusTitleRes)
                tvCompletedAt.text = ctx.getString(uiStatus.statusDescRes)
                cardRoot.setBackgroundColor(
                    ContextCompat.getColor(ctx, uiStatus.bgColorRes)
                )
            }
        }

        viewModel.productState.observe(viewLifecycleOwner) { productState ->
            product = productState ?: return@observe

            with(binding) {
                // Теперь product точно не null, так как мы сделали элвис-возврат выше
                val currentProduct = product!!

                tvProcess.text = currentProduct.process.name
                tvProductNumber.text = currentProduct.serialNumber
                tvCreated.text = formatIsoToUi(currentProduct.createdAt)

                val uiStatus = currentProduct.status.toUiProductStatus()
                val ctx = root.context

                val bgColor = ContextCompat.getColor(ctx, uiStatus.bgColorRes)
                val textColor = ContextCompat.getColor(ctx, uiStatus.textColorRes)

                chipProductStatus.text = getString(uiStatus.titleRes)
                chipProductStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
                chipProductStatus.setTextColor(textColor)

                cardProductInfo.setCardBackgroundColor(bgColor)
            }
        }

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            userRole = user?.role
        }

        binding.btnAllStages.setOnClickListener {
            findNavController().navigate(R.id.action_productFragment_to_productFullFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded) return@collect
                    showDialog(msg) {
                        viewModel.resetIsHandled()
                    }
                }
            }
        }

        binding.chipProductStatus.setOnClickListener {
            if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                showConfirmDialog("Изменение статуса", "Вы уверены, что хотите изменить статус?") {
                    findNavController().navigate(R.id.action_productFragment_to_editProductStatusFragment)
                }
            }
        }

        binding.btnEdit.setOnClickListener {
            if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                showConfirmDialog("Изменение процесса", "Вы уверены, что хотите изменить процесс?") {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.setProcesses()
                        if (_binding != null) {
                            findNavController().navigate(R.id.action_productFragment_to_editProductFragment)
                        }
                    }
                }
            }
        }

        binding.btnDone.setOnClickListener {
            val lastStep = viewModel.lastStep.value ?: emptyStep
            val currentProduct = product ?: return@setOnClickListener

            if (lastStep.id == 0) return@setOnClickListener

            if (currentProduct.status == ProductStatus.REWORK ||
                currentProduct.status == ProductStatus.SCRAP
            ) {
                showDialog("Нельзя закрыть этап: продукт в статусе РЕМОНТ или БРАК")
                return@setOnClickListener
            }

            if (lastStep.status == StepStatusBackend.DONE.raw) {
                showDialog("Этап уже выполнен")
            } else {
                showConfirmDialog("Закрыть этап", "Вы уверены, что хотите закрыть этап?") {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.closeStep(lastStep)
                    }
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnDone.isEnabled = !state.isActionInProgress
            binding.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    // Вспомогательный метод для обычных алертов
    private fun showDialog(message: String, onPositive: () -> Unit = {}) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ ->
                onPositive()
                dialog.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    // Вспомогательный метод для диалогов с подтверждением
    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
                activeDialog = null
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }

    override fun onDestroy() {
        viewModel.resetIsHandled()
        super.onDestroy()
    }
}