package ru.faserkraft.client.presentation.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.FragmentProductsReworkScrapBinding
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class ProductsReworkScrapFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentProductsReworkScrapBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsReworkScrapAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductsReworkScrapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupEmptyObserver()
        observeState()
        observeEvents()

        binding.swipeRefreshReworkScrap.setOnRefreshListener {
            viewModel.loadReworkScrapProducts()
        }
        viewModel.loadReworkScrapProducts()
    }

    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvReworkScrap.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = ProductsReworkScrapAdapter { statItem ->
            if (_binding == null) return@ProductsReworkScrapAdapter

            // Передаем во ViewModel только суть (процесс и статус)
            viewModel.selectReworkScrapProduct(statItem.processName, statItem.status)

            // ИСПОЛЬЗУЕМ КЛАСС DIRECTIONS ОТ КОНТЕЙНЕРА
            findNavController().navigateSafely(
                ProductContainerFragmentDirections.actionProductContainerFragmentToProductsReworkScrapListFragment()
            )
        }

        binding.rvReworkScrap.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReworkScrap.adapter = adapter
    }

    private fun setupEmptyObserver() {
        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = updateEmptyView()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = updateEmptyView()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = updateEmptyView()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        updateEmptyView()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            b.swipeRefreshReworkScrap.isRefreshing = state.isLoading
            b.swipeRefreshReworkScrap.isEnabled = !state.isLoading
            adapter.submitList(buildUiItems(state.reworkScrapProducts))
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Группировка ----------

    private fun buildUiItems(products: List<Product>): List<ReworkScrapUiItem> {
        if (products.isEmpty()) return emptyList()

        // 1. Группируем продукты по имени процесса
        val groupedByProcess = products
            .sortedBy { it.process.name }
            .groupBy { it.process.name }

        return buildList {
            groupedByProcess.forEach { (processName, processProducts) ->
                // Добавляем заголовок процесса
                add(ReworkScrapUiItem.ProcessHeader(processName))

                // 2. Внутри процесса группируем по статусу (REWORK / SCRAP)
                val groupedByStatus = processProducts.groupBy { it.status }

                // Сортируем статусы, чтобы они шли в одинаковом порядке (например, сначала REWORK, потом SCRAP)
                groupedByStatus.toSortedMap().forEach { (status, statusProducts) ->
                    // Добавляем карточку с подсчитанным количеством
                    add(
                        ReworkScrapUiItem.StatusStatItem(
                            processName = processName,
                            status = status,
                            count = statusProducts.size
                        )
                    )
                }
            }
        }
    }

    // ---------- Вспомогательное ----------

    private fun updateEmptyView() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyReworkScrap.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvReworkScrap.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}