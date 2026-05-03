package ru.faserkraft.client.presentation.packaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.databinding.FragmentProductsStorageBinding
import ru.faserkraft.client.domain.model.Packaging

class StorageFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentProductsStorageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsStorageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        setupListeners()
        observeState()

        viewModel.loadPackagingInStorage()
    }

    private fun setupAdapter() {
        adapter = ProductsStorageAdapter { item ->
            val action = StorageContainerFragmentDirections
                .actionStorageContainerFragmentToPackagingListFragment(item.process)
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        binding.rvProductsStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsStats.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefreshStats.setOnRefreshListener {
            viewModel.loadPackagingInStorage()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    b.swipeRefreshStats.isRefreshing = state.isLoading
                    b.swipeRefreshStats.isEnabled = !state.isLoading

                    val uiList = mapToUiItems(state.packagingInStorage)
                    adapter.submitList(uiList) { checkEmpty() }
                }
            }
        }
    }

    private fun mapToUiItems(list: List<Packaging>): List<ProductsStorageUiItem> {
        if (list.isEmpty()) return emptyList()

        return list
            .flatMap { it.products }
            .groupBy { it.process.id to it.process.name }
            .map { (key, products) ->
                val (processId, processName) = key
                val packagingCount = list.count { packaging ->
                    packaging.products.any { it.process.id == processId }
                }
                ProductsStorageUiItem(
                    id = processId,
                    process = processName,
                    productCount = products.size,
                    packagingCount = packagingCount
                )
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvProductsStats.adapter = null
        _binding = null
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyStorage.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvProductsStats.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}