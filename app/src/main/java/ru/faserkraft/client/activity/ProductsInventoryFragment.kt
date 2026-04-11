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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.ProductsInventoryAdapter
import ru.faserkraft.client.adapter.ProductsInventoryUiItem
import ru.faserkraft.client.databinding.FragmentProductsInventoryBinding
import ru.faserkraft.client.dto.ProductsInventoryDto
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ProductsInventoryFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentProductsInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsInventoryAdapter

    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductsInventoryAdapter { dto ->

            if (_binding == null) return@ProductsInventoryAdapter

            val action = ProductsInventoryFragmentDirections
                .actionProductsInventoryFragmentToProductsInventoryByProcessFragment(dto)
            findNavController().navigate(action)
        }

        binding.rvProductsStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsStats.adapter = adapter

        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        // первоначальная загрузка
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getProductsInventory()
        }

        // observe данных
        viewModel.productsInventory.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            val grouped = list
                .sortedWith(
                    compareBy<ProductsInventoryDto> { it.processName }
                        .thenBy { it.stepDefinitionId }
                )
                .groupBy { it.processName }

            val uiItems = mutableListOf<ProductsInventoryUiItem>()
            grouped.forEach { (processName, items) ->
                uiItems += ProductsInventoryUiItem.ProcessHeader(processName)
                items.forEach { dto ->
                    uiItems += ProductsInventoryUiItem.StageItem(dto)
                }
            }

            adapter.submitList(uiItems)
        }

        // состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val b = _binding ?: return@observe
            b.swipeRefreshStats.isRefreshing = state.isLoading
            b.swipeRefreshStats.isEnabled = !state.isLoading
        }

        // обработка ошибок
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded) return@collect

                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                            activeDialog = null
                        }
                        .also { it.setOnDismissListener { activeDialog = null } }
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

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null

        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }

        binding.rvProductsStats.adapter = null
        _binding = null
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyInventory.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvProductsStats.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}