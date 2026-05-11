package ru.faserkraft.client.presentation.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductFullBinding
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatIsoToUi
import ru.faserkraft.client.utils.ext.navigateSafely // <- Добавлен импорт безопасной навигации

class ProductFullFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductFullBinding? = null
    private val binding get() = _binding!!

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

        val adapter = StepsAdapter { stepUiItem ->
            viewModel.selectStep(stepUiItem.step)
            viewModel.loadEmployees()

            findNavController().navigateSafely(
                ProductFullFragmentDirections
                    .actionProductFullFragmentToEditStepFragment()
            )
        }

        binding.rvSteps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSteps.adapter = adapter

        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            val product = state.product ?: return@collectFlow

            b.tvProductNumber.text = getString(R.string.product_serial_number, product.serialNumber)
            b.tvTechProcess.text = getString(R.string.product_tech_process, product.process.name)
            b.tvStartAt.text =
                getString(R.string.product_start_at, formatIsoToUi(product.createdAt))
            val isMaster = state.userRole == UserRole.MASTER
            val stepItems = product.steps.map { step ->
                StepUiItem(
                    isEditable = isMaster
                            && step.performedAt != null,
                    step = step,
                )
            }
            adapter.submitList(stepItems)
        }
    }

    override fun onDestroyView() {
        binding.rvSteps.adapter = null
        _binding = null
        super.onDestroyView()
    }
}