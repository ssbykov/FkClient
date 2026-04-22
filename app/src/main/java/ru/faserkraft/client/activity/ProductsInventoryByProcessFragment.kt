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

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsInventoryByProcessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductsInventoryByProcessAdapter { dto ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.handleProductQr(dto)

                if (_binding == null) return@launch

                findNavController().navigate(
                    ProductsInventoryByProcessFragmentDirections
                        .actionProductsInventoryByProcessFragmentToProductFullFragment()
                )
            }
        }

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
        viewModel.productsInventoryByProcess.observe(viewLifecycleOwner) { list ->
            val items = list.orEmpty().map {
                val stepId = args.productsInventoryDto.stepDefinitionId
                val step = it.steps.find { step -> step.stepDefinition.id == stepId }

                ProductsInventoryByProcessUiItem(
                    id = it.id,
                    serialNumber = it.serialNumber,
                    createdAt = step?.performedAt.toString()
                )
            }
            adapter.submitList(items)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // Защита: observer может сработать в момент уничтожения
            val b = _binding ?: return@observe
            b.swipeRefreshDetail.isRefreshing = state.isLoading
            b.swipeRefreshDetail.isEnabled = !state.isLoading
        }

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
    }

    private fun renderHeader() {
        with(args.productsInventoryDto) {
            binding.tvProcessName.text = processName
            binding.tvStageName.text = stepName
        }
    }

    private fun loadData() {
        adapter.submitList(emptyList())
        viewLifecycleOwner.lifecycleScope.launch {
            with(args.productsInventoryDto) {
                viewModel.getProductsByLastCompletedStep(processId, stepDefinitionId)
            }
        }
    }

    private fun setupRefresh() {
        binding.swipeRefreshDetail.setOnRefreshListener {
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null

        binding.rvProductsDetail.adapter = null
        _binding = null
    }
}