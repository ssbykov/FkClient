package ru.faserkraft.client.presentation.packaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.databinding.FragmentPackagingListBinding
import ru.faserkraft.client.presentation.common.adapter.PackagingListAdapter
import ru.faserkraft.client.presentation.common.adapter.PackagingListUiItem
import ru.faserkraft.client.presentation.order.ModuleTypeUi
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatPackagingDate
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

/**
 * Фрагмент со списком упаковок по конкретному процессу на складе.
 * Отображает упаковки из уже загруженного списка склада и мгновенно переходит в карточку упаковки без лишних сетевых запросов.
 */
class PackagingListFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentPackagingListBinding? = null
    private val binding get() = _binding!!

    private val args: PackagingListFragmentArgs by navArgs()
    private lateinit var adapter: PackagingListAdapter

    // Полный список упаковок для текущего процесса (до фильтрации поиском)
    private var allUiItems: List<PackagingListUiItem> = emptyList()

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPackagingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val process = args.process
        binding.tvProcess.text = process

        setupRecyclerView()
        setupSearch()
        observeState(process)
        observeEvents()

        // ОПТИМИЗАЦИЯ 1: загружаем из сети только если данных на складе ещё нет
        if (viewModel.uiState.value.packagingInStorage.isEmpty()) {
            viewModel.loadPackagingInStorage()
        }
    }

    override fun onDestroyView() {
        binding.rvProducts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup & Observe ----------

    private fun setupRecyclerView() {
        adapter = PackagingListAdapter(
            onItemClick = { item ->
                if (_binding == null) return@PackagingListAdapter

                // ОПТИМИЗАЦИЯ 2: находим упаковку в памяти склада и переходим мгновенно за 0 мс!
                val boxInMemory = viewModel.uiState.value.packagingInStorage.find {
                    it.id == item.id || it.serialNumber == item.serialNumber
                }

                if (boxInMemory != null) {
                    viewModel.selectPackaging(boxInMemory)
                    val action =
                        PackagingListFragmentDirections.actionPackagingListFragmentToPackagingFragment(
                            null
                        )
                    findNavController().navigateSafely(action)
                } else {
                    // Резервный сетевой запрос, если вдруг не нашли в памяти
                    viewModel.loadPackaging(item.serialNumber)
                }
            }
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
    }

    private fun setupSearch() {
        // Мгновенная фильтрация списка при вводе номера упаковки
        binding.etSearch.doAfterTextChanged { text ->
            filterList(text?.toString().orEmpty())
        }
    }

    private fun filterList(query: String) {
        val b = _binding ?: return
        val trimmedQuery = query.trim()

        val filteredItems = if (trimmedQuery.isEmpty()) {
            allUiItems
        } else {
            allUiItems.filter { item ->
                item.serialNumber.contains(trimmedQuery, ignoreCase = true)
            }
        }

        adapter.submitList(filteredItems)

        val isListEmpty = filteredItems.isEmpty() && !b.progressBar.isVisible
        b.tvEmptyPackaging.isVisible = isListEmpty
        b.rvProducts.isVisible = !isListEmpty
    }

    private fun observeState(process: String) {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.progressBar.isVisible = state.isLoading

            // Формируем полный список упаковок по процессу
            allUiItems = state.packagingInStorage
                .filter { box ->
                    box.products.any { it.process.name == process }
                }
                .map { box ->
                    val groups = box.products.groupBy { it.process.name }
                    PackagingListUiItem(
                        id = box.id,
                        serialNumber = box.serialNumber,
                        totalCount = box.products.size,
                        types = groups.map { (name, list) ->
                            ModuleTypeUi(name = name, count = list.size)
                        },
                        performedBy = box.performedBy?.name,
                        performedAt = box.performedAt?.let { formatPackagingDate(it) }
                    )
                }

            // Применяем текущий поисковый запрос
            val currentQuery = b.etSearch.text?.toString().orEmpty()
            filterList(currentQuery)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is PackagingEvent.ShowError -> showErrorSnackbar(event.message)
                PackagingEvent.NavigateToPackaging -> {
                    val action =
                        PackagingListFragmentDirections.actionPackagingListFragmentToPackagingFragment(
                            null
                        )
                    findNavController().navigateSafely(action)
                }

                PackagingEvent.NavigateToNewPackaging,
                PackagingEvent.NavigateToEdit,
                PackagingEvent.PackagingDeleted -> Unit
            }
        }
    }
}
