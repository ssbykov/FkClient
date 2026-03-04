package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.ProductsInventoryAdapter
import ru.faserkraft.client.adapter.ProductsInventoryUiItem
import ru.faserkraft.client.databinding.FragmentProductsInventoryBinding
import ru.faserkraft.client.dto.ProductsInventoryDto
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ProductsInventoryFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentProductsInventoryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductsInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ProductsInventoryAdapter()
        binding.rvProductsStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsStats.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getProductsInventory()
        }

        // observe данных
        viewModel.productsInventory.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            // группировка по процессам
            val grouped = list.groupBy { it.processName }

            val uiItems = mutableListOf<ProductsInventoryUiItem>()
            grouped.forEach { (processName, items) ->
                uiItems += ProductsInventoryUiItem.ProcessHeader(processName)
                items.forEach { dto: ProductsInventoryDto ->
                    uiItems += ProductsInventoryUiItem.StageItem(dto)
                }
            }

            adapter.submitList(uiItems)
        }

        // состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isLoading = state.isLoading
            binding.swipeRefreshStats.isRefreshing = isLoading
            binding.swipeRefreshStats.isEnabled = !isLoading
        }

        // обработка ошибок
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

        // pull-to-refresh
        binding.swipeRefreshStats.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getProductsInventory()
            }
        }
    }
}
