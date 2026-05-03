package ru.faserkraft.client.presentation.packaging

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
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentPackagingBinding
import ru.faserkraft.client.presentation.product.ProductEvent
import ru.faserkraft.client.presentation.product.ProductViewModel
import ru.faserkraft.client.utils.formatIsoToUi
import ru.faserkraft.client.utils.showErrorSnackbar

class PackagingFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingContentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeState()
        observeEvents()
        binding.btnEdit.setOnClickListener {
            viewModel.onEditClicked()
        }
    }

    private fun setupRecyclerView() {
        adapter = PackagingContentAdapter { serialNumber ->
            productViewModel.loadProduct(serialNumber)
        }
        binding.rvPackagingProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingProducts.adapter = adapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    val packaging = state.currentPackaging

                    b.tvPackagingSerial.text = packaging?.serialNumber
                    b.tvCreatedBy.text = packaging?.performedBy?.name
                    b.tvCreatedAt.text = formatIsoToUi(packaging?.performedAt)

                    val products = packaging?.products.orEmpty()
                    adapter.submitList(
                        products
                            .map { p ->
                                PackagingContentUiItem(
                                    id = p.id,
                                    serialNumber = p.serialNumber,
                                    processName = p.process.name
                                )
                            }
                            .sortedBy { it.serialNumber }
                    )
                    b.tvItemsSummary.text = getString(R.string.items_count, products.size)

                    b.btnEdit.visibility = if (state.canEdit) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PackagingEvent.ShowError -> showErrorSnackbar(event.message)
                        PackagingEvent.NavigateToEdit -> navigateToEdit()
                        PackagingEvent.NavigateToPackaging,
                        PackagingEvent.NavigateToNewPackaging,
                        PackagingEvent.PackagingDeleted -> Unit
                    }
                }
            }
        }
        // отдельно слушаем события продукта
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                productViewModel.events.collect { event ->
                    when (event) {
                        is ProductEvent.NavigateToProduct ->
                            findNavController().navigate(
                                R.id.action_packagingFragment_to_productFullFragment
                            )

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun navigateToEdit() {
        findNavController().navigate(
            PackagingFragmentDirections.actionPackagingFragmentToNewPackagingFragment()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPackagingProducts.adapter = null
        _binding = null
    }
}