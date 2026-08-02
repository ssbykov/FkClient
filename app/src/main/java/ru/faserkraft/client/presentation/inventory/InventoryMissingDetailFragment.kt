package ru.faserkraft.client.presentation.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentInventoryMissingDetailBinding
import ru.faserkraft.client.domain.model.ProductInventoryItem
import ru.faserkraft.client.presentation.ui.collectFlow

@AndroidEntryPoint
class InventoryMissingDetailFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private val args: InventoryMissingDetailFragmentArgs by navArgs()

    private var _binding: FragmentInventoryMissingDetailBinding? = null
    private val binding get() = _binding!!

    private val adapter = InventoryMissingDetailAdapter(::onActionClick)

    private var currentStepId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentStepId = args.stepDefinitionId
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryMissingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        observeState()
    }

    override fun onDestroyView() {
        binding.rvProducts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // Находим результат по ID этапа
            val result = state.compareResults.find { it.stepDefinition.id == currentStepId }

            if (result == null) {
                // Если данные почему-то отсутствуют (например, после полной синхронизации)
                return@collectFlow
            }

            // Настраиваем тулбар
            b.toolbar.title =
                getString(R.string.product_tech_process, result.stepDefinition.process.name)
            b.toolbar.subtitle = getString(R.string.step_not_found, result.stepDefinition.name)

            // Отправляем список missing (недостающих)
            adapter.submitList(result.missing)

            // Управление оверлеем загрузки
            b.loadingOverlay.visibility = if (state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    private fun onActionClick(item: ProductInventoryItem) {
        // Вызов метода во ViewModel для обработки недостающего модуля
        // Например: viewModel.markItemAsScrap(item.product.serialNumber)
    }
}