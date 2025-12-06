package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.dto.StepStatusBackend
import ru.faserkraft.client.dto.emptyStep
import ru.faserkraft.client.dto.toUiStatus
import ru.faserkraft.client.utils.formatIsoToUi
import ru.faserkraft.client.viewmodel.ScannerViewModel


class ProductFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentProductBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.lastStep.observe(viewLifecycleOwner) { lastStep ->
            with(binding) {
                tvStepName.text = lastStep.stepDefinition.template.name
                val uiStatus = lastStep.toUiStatus()
                tvStatus.text = uiStatus.title
                tvCompletedAt.text = uiStatus.description
                imgStatus.setImageResource(uiStatus.iconRes)
            }
        }

        viewModel.productState.observe(viewLifecycleOwner) { product ->
            product ?: return@observe
            with(binding) {
                tvProcess.text = product.process.name
                tvProductNumber.text = product.serialNumber
                tvCreated.text = formatIsoToUi(product.createdAt)
            }
        }

        binding.btnAllStages.setOnClickListener {
            findNavController().navigate(R.id.action_productFragment_to_productFullFragment)
        }


        binding.btnDone.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val lastStep = viewModel.lastStep.value ?: emptyStep

                    if (lastStep.id == 0) {
                        return@launch
                    }

                    if (lastStep.status == StepStatusBackend.DONE.raw) {
                        AlertDialog.Builder(requireContext())
                            .setMessage("Этап уже выполнен")
                            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                            .show()
                    } else {
                        viewModel.closeStep(lastStep)
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(e.message ?: "Ошибка")
                        .setPositiveButton("ОК") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }


    }

    override fun onDestroy() {
        viewModel.resetIsHandled()
        super.onDestroy()
    }
}