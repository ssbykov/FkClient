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
import ru.faserkraft.client.databinding.FragmentInventoryConflictDetailBinding
import ru.faserkraft.client.presentation.ui.collectFlow

@AndroidEntryPoint
class InventoryConflictDetailFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    // Используем Safe Args (InventoryConflictDetailFragmentArgs сгенерируется из nav_graph)
    private val args: InventoryConflictDetailFragmentArgs by navArgs()

    private var _binding: FragmentInventoryConflictDetailBinding? = null
    private val binding get() = _binding!!

    private val adapter = InventoryConflictAdapter(::onActionClick)

    private var currentStepId: Int = -1
    private lateinit var currentConflictType: ConflictType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем аргументы: ID этапа и тип конфликта (например, "MISSING" или "STEP_MISMATCH")
        currentStepId = args.stepDefinitionId
        currentConflictType = ConflictType.valueOf(args.conflictType)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryConflictDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        observeState()
    }

    override fun onDestroyView() {
        binding.rvConflicts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvConflicts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConflicts.adapter = adapter
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // Находим результат по ID этапа в данных инвентаризации
            val result = state.compareResults.find { it.stepDefinition.id == currentStepId }

            if (result == null) {
                return@collectFlow
            }

            // Настраиваем тулбар (заголовок процесса + подзаголовок этапа)
            b.toolbar.title =
                getString(R.string.product_tech_process, result.stepDefinition.process.name)

            // В зависимости от типа конфликта показываем соответствующий список и подзаголовок
            val conflictItems = result.toConflictItems(state.currentInventoryItems)
            val filteredList = conflictItems.filter { it.conflictType == currentConflictType }

            b.toolbar.subtitle = if (currentConflictType == ConflictType.MISSING) {
                getString(R.string.step_not_found, result.stepDefinition.name)
            } else {
                "${result.stepDefinition.name} (Расхождения)"
            }

            // Отправляем отфильтрованный список в адаптер
            adapter.submitList(filteredList)

            // Управление оверлеем загрузки
            b.loadingOverlay.visibility = if (state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    private fun onActionClick(conflictItem: ConflictItem) {
        // Вызов метода во ViewModel для обработки конкретного ConflictItem
        when (conflictItem.conflictType) {
            ConflictType.MISSING -> {
                // Например: viewModel.markItemAsMissing(conflictItem.item.serialNumber)
            }
            ConflictType.STEP_MISMATCH -> {
                // Например: viewModel.syncItemStep(conflictItem)
            }
        }
    }
}