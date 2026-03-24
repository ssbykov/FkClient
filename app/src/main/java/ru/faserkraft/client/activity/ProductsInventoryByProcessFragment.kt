package ru.faserkraft.client.activity

import android.app.AlertDialog
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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.ProductsInventoryByProcessAdapter
import ru.faserkraft.client.adapter.ProductsInventoryByProcessUiItem
import ru.faserkraft.client.databinding.FragmentProductsInventoryByProcessBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ProductsInventoryByProcessFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private val args: ProductsInventoryByProcessFragmentArgs by navArgs()

    private var _binding: FragmentProductsInventoryByProcessBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsInventoryByProcessAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsInventoryByProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        adapter = ProductsInventoryByProcessAdapter { dto ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.handleProductQr(dto)
                findNavController().navigate(
                    ProductsInventoryByProcessFragmentDirections
                        .actionProductsInventoryByProcessFragmentToProductFullFragment()
                )
            }
        }

        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupObservers()
        renderHeader()
        loadData()
        setupRefresh()
    }

    private fun setupRecycler() {
        binding.rvProductsDetail.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsDetail.adapter = adapter
    }

    private fun setupObservers() {
        // Подписка на список элементов для RecyclerView
        viewModel.productsInventoryByProcess.observe(viewLifecycleOwner) { list ->
            val items = list.orEmpty().map {
                ProductsInventoryByProcessUiItem(
                    id = it.id,
                    serialNumber = it.serialNumber,
                    createdAt = it.createdAt
                )
            }
            adapter.submitList(items)
        }

        // Подписка на состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshDetail.isRefreshing = state.isLoading
            binding.swipeRefreshDetail.isEnabled = !state.isLoading
        }

        // Подписка на ошибки через Flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

    }

    private fun renderHeader() {
        // Заполняем шапку экрана данными из аргументов навигации
        with(args.productsInventoryDto) {
            binding.tvProcessName.text = processName
            binding.tvStageName.text = stepName
        }
    }

    private fun loadData() {
        // Запрашиваем список по processId и stepDefinitionId
        viewLifecycleOwner.lifecycleScope.launch {

            with(args.productsInventoryDto) {
                viewModel.getProductsByLastCompletedStep(processId, stepDefinitionId)
            }
        }
    }

    private fun setupRefresh() {
        // Pull-to-refresh
        binding.swipeRefreshDetail.setOnRefreshListener {
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
