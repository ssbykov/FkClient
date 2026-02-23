package ru.faserkraft.client.activity

import android.app.AlertDialog
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

    private lateinit var binding: FragmentProductBinding

    private lateinit var userRole: UserRole

    private lateinit var product: ProductDto


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
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
                    ContextCompat.getColor(cardRoot.context, uiStatus.bgColorRes)
                )
            }
        }

        viewModel.productState.observe(viewLifecycleOwner) {
            product = it ?: return@observe
            with(binding) {
                tvProcess.text = product.process.name
                tvProductNumber.text = product.serialNumber
                tvCreated.text = formatIsoToUi(product.createdAt)

                val uiStatus = product.status.toUiProductStatus()
                val ctx = root.context

                chipProductStatus.text = getString(uiStatus.titleRes)
                chipProductStatus.chipBackgroundColor =
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, uiStatus.bgColorRes)
                    )

                // меняем цвет карточки с общей инфой о продукте
                cardProductInfo.setCardBackgroundColor(
                    ContextCompat.getColor(ctx, uiStatus.bgColorRes)
                )
                // или только обводку:
                // cardProductInfo.strokeColor = ContextCompat.getColor(ctx, uiStatus.bgColorRes)
            }
        }


        viewModel.userData.observe(viewLifecycleOwner) { user ->
            userRole = user?.role ?: return@observe
        }

        binding.btnAllStages.setOnClickListener {
            findNavController().navigate(R.id.action_productFragment_to_productFullFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        binding.chipProductStatus.setOnClickListener {
            if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Изменение статуса")
                    .setMessage("Вы уверены, что хотите изменить статус?")
                    .setPositiveButton("Изменить") { dialog, _ ->
                        findNavController().navigate(R.id.action_productFragment_to_editProductStatusFragment)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        binding.tvProcess.setOnClickListener {
            if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Изменение процесса")
                    .setMessage("Вы уверены, что хотите изменить процесс?")
                    .setPositiveButton("Изменить") { dialog, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.setProcesses()
                            findNavController().navigate(R.id.action_productFragment_to_editProductFragment)
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }


        binding.btnDone.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val lastStep = viewModel.lastStep.value ?: emptyStep

                if (lastStep.id == 0) return@launch

                if ( product.status == ProductStatus.REPAIR ||
                    product.status == ProductStatus.SCRAP
                ) {
                    AlertDialog.Builder(requireContext())
                        .setMessage("Нельзя закрыть этап: продукт в статусе РЕМОНТ или БРАК")
                        .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                        .show()
                    return@launch
                }

                if (lastStep.status == StepStatusBackend.DONE.raw) {
                    AlertDialog.Builder(requireContext())
                        .setMessage("Этап уже выполнен")
                        .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Закрыть этап")
                        .setMessage("Вы уверены, что хотите закрыть этап?")
                        .setPositiveButton("Закрыть") { dialog, _ ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                viewModel.closeStep(lastStep)
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton("Отмена") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnDone.isEnabled = !state.isActionInProgress

            binding.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
        }

    }

    override fun onDestroy() {
        viewModel.resetIsHandled()
        super.onDestroy()
    }
}