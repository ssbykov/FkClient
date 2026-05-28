package ru.faserkraft.client.presentation.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentInventoryDetailBinding
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.formatIsoToUi
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

@AndroidEntryPoint
class InventoryDetailFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private var _binding: FragmentInventoryDetailBinding? = null
    private val binding get() = _binding!!

    private val adapter = InventoryItemsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.clearCurrentInventory()
            findNavController().popBackStack()
        }

        setupRecyclerView()
        setupButtons()
        observeState()
        observeEvents()
    }

    override fun onDestroyView() {
        binding.rvItems.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnAddItems.setOnClickListener {
            viewModel.continueInventory()
        }
        binding.btnCompare.setOnClickListener {
            viewModel.compareInventory()
        }
    }

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            val inventory = state.currentInventory ?: return@collectFlow

            b.tvTitle.text = getString(R.string.inventory_name_format, inventory.id)
            b.tvCreatedAt.text = formatIsoToUi(inventory.createdAt)

            b.tvStatus.text = if (inventory.isOpen) {
                getString(R.string.inventory_status_open)
            } else {
                val closedStr = getString(R.string.inventory_status_closed)
                inventory.completedAt?.let { "$closedStr · ${formatIsoToUi(it)}" } ?: closedStr
            }

            b.tvItemsCount.text = getString(
                R.string.inventory_scanned_count,
                state.currentInventoryItemCount
            )

            val uiItems = mutableListOf<InventoryItemUiItem>()
            // Группируем элементы по объекту ProcessShort
            val groupedItems = state.currentInventoryItems.groupBy { it.stepDefinition.process }

            for ((process, processItems) in groupedItems) {
                // Извлекаем имя из объекта процесса для заголовка
                uiItems.add(InventoryItemUiItem.ProcessHeader(process.name))

                val sortedItems = processItems.sortedByDescending { it.scannedAt }
                uiItems.addAll(sortedItems.map { InventoryItemUiItem.Entry(it) })
            }

            adapter.submitList(uiItems)

            b.btnAddItems.visibility = if (inventory.isOpen) View.VISIBLE else View.GONE
            b.btnCompare.visibility = if (inventory.isOpen) View.VISIBLE else View.GONE
            b.btnCompare.isEnabled =
                state.currentInventoryItems.isNotEmpty() &&
                        !state.isLoading &&
                        !state.isActionInProgress

            b.progressBar.visibility =
                if (state.isLoading || state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    private fun getNavController() =
        (requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .navController

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            android.util.Log.d("INV_DEBUG", "event=$event, fragment=${this::class.java.simpleName}")
            if (_binding == null || !isAdded) return@collectFlow

            when (event) {
                InventoryEvent.NavigateToScan -> {
                    findNavController().navigateSafely(
                        InventoryDetailFragmentDirections
                            .actionInventoryDetailFragmentToInventoryScanFragment()
                    )
                }

                InventoryEvent.NavigateToResults -> {
                    getNavController().navigate(
                        InventoryDetailFragmentDirections
                            .actionInventoryDetailFragmentToInventoryResultsFragment()
                    )
                }

                is InventoryEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }
}