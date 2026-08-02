package ru.faserkraft.client.presentation.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentInventoryResultsBinding
import ru.faserkraft.client.domain.model.InventoryCompareResult
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely

@AndroidEntryPoint
class InventoryResultsFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private var _binding: FragmentInventoryResultsBinding? = null
    private val binding get() = _binding!!

    private val adapter = InventoryResultsAdapter(
        onUnexpectedClick = ::onUnexpectedClick,
        onMissingClick = ::onMissingClick
    )

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        observeState()
    }

    override fun onDestroyView() {
        binding.rvResults.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
    }

    // ---------- Observers ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            val results = state.compareResults

            adapter.submitList(buildListItems(results))

            b.toolbarTitle.text = getString(R.string.inventory_results_title, state.currentInventory?.id)

            b.tvDbTotal.text = results.sumOf { it.dbCount }.toString()
            b.tvScannedTotal.text = results.sumOf { it.scannedCount }.toString()

            val totalDiff = results.sumOf { it.missing.size + it.unexpected.size }
            b.tvDiffTotal.text = totalDiff.toString()
        }
    }

    // ---------- List building ----------

    private fun buildListItems(
        results: List<InventoryCompareResult>,
    ): List<InventoryResultListItem> = buildList {
        // Группируем строго по ID процесса (теперь он точно Int)
        val grouped = results.groupBy { it.stepDefinition.process.id }
        val sortedKeys = grouped.keys.sorted() // Обычная сортировка чисел

        for (processId in sortedKeys) {
            val items = grouped.getValue(processId)

            // Имя берем из первого элемента
            val processName = items.first().stepDefinition.process.name

            add(InventoryResultListItem.Header(processId, processName))

            // Сортируем по порядку этапа
            val sortedItems = items.sortedBy { it.stepDefinition.order }
            sortedItems.forEach { add(InventoryResultListItem.Result(it)) }
        }
    }

    // ---------- Navigation ----------

    private fun onUnexpectedClick(result: InventoryCompareResult) {
        if (result.unexpected.isEmpty()) return

        val stepId = result.stepDefinition.id
        findNavController().navigateSafely(
            InventoryResultsFragmentDirections
                .actionInventoryResultsFragmentToInventoryDiffDetailFragment(stepId)
        )
    }

    private fun onMissingClick(result: InventoryCompareResult) {
        if (result.missing.isEmpty()) return

        val stepId = result.stepDefinition.id
        findNavController().navigateSafely(
            InventoryResultsFragmentDirections
                .actionInventoryResultsFragmentToInventoryMissingDetailFragment(stepId)
        )
    }

}