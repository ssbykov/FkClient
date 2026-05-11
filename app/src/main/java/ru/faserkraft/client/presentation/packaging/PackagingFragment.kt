package ru.faserkraft.client.presentation.packaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentPackagingBinding
import ru.faserkraft.client.presentation.product.ProductEvent
import ru.faserkraft.client.presentation.product.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatIsoToUi
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class PackagingFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels()

    private val args: PackagingFragmentArgs by navArgs()

    private var _binding: FragmentPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingContentAdapter

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args.packagingSerialNumber?.let { serialNumber ->
            viewModel.loadPackaging(serialNumber)
        }

        setupRecyclerView()
        observeState()
        observeEvents()

        binding.btnEdit.setOnClickListener {
            viewModel.onEditClicked()
        }
    }

    override fun onDestroyView() {
        binding.rvPackagingProducts.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup & Observe ----------

    private fun setupRecyclerView() {
        adapter = PackagingContentAdapter { serialNumber ->
            productViewModel.loadProduct(serialNumber)
        }
        binding.rvPackagingProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingProducts.adapter = adapter
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

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

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is PackagingEvent.ShowError -> {
                    showErrorSnackbar(event.message)
                }

                PackagingEvent.NavigateToEdit -> {
                    val action =
                        PackagingFragmentDirections.actionPackagingFragmentToNewPackagingFragment()
                    findNavController().navigateSafely(action.actionId)
                }

                PackagingEvent.NavigateToPackaging,
                PackagingEvent.NavigateToNewPackaging,
                PackagingEvent.PackagingDeleted -> Unit
            }
        }

        collectFlow(productViewModel.events) { event ->
            when (event) {
                is ProductEvent.NavigateToProduct -> {
                    findNavController().navigateSafely(
                        R.id.action_packagingFragment_to_productFullFragment
                    )
                }

                is ProductEvent.ShowError -> {
                    showErrorSnackbar(event.message)
                }

                else -> Unit
            }
        }
    }
}