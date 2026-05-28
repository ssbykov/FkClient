package ru.faserkraft.client.presentation.inventory

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentInventoryListBinding
import ru.faserkraft.client.domain.model.Inventory
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

@AndroidEntryPoint
class InventoryListFragment : Fragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private var _binding: FragmentInventoryListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: InventoryListAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupEmptyObserver()
        observeState()
        observeEvents()
        setupListeners()

        viewModel.loadInventories()
    }

    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvInventories.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupAdapter() {
        adapter = InventoryListAdapter(
            onItemClick = { inventory ->
                if (_binding == null) return@InventoryListAdapter
                viewModel.openInventoryDetail(inventory)
            },
            onDeleteClick = { inventory ->
                showDeleteConfirmDialog(inventory)
            }
        )
        binding.rvInventories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInventories.adapter = adapter
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

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadInventories()
        }
        binding.fabNewInventory.setOnClickListener {
            viewModel.createInventory()
        }
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.swipeRefresh.isRefreshing = state.isLoading
            b.swipeRefresh.isEnabled = !state.isLoading
            b.fabNewInventory.isEnabled = !state.isActionInProgress

            adapter.submitList(
                state.inventories
                    .sortedBy { it.id }
                    .map { inventory ->
                        InventoryListItem(
                            inventory = inventory,
                            itemCount = inventory.itemCount,
                        )
                    }
            )

            updateEmptyView()
        }
    }

    private fun getNavController() =
        (requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .navController

    private fun observeEvents() {
        collectFlow(viewModel.events, Lifecycle.State.CREATED) { event ->
            if (_binding == null || !isAdded) return@collectFlow

            when (event) {
                InventoryEvent.NavigateToDetail -> {
                    getNavController().navigateSafely(
                        InventoryContainerFragmentDirections
                            .actionInventoryListFragmentToInventoryDetailFragment()
                    )
                }

                InventoryEvent.NavigateToScan -> {
                    getNavController().navigateSafely(
                        InventoryContainerFragmentDirections
                            .actionInventoryListFragmentToInventoryScanFragment()
                    )
                }

                is InventoryEvent.ShowError -> showErrorSnackbar(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Диалог удаления ----------

    private fun showDeleteConfirmDialog(inventory: Inventory) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_inventory_title)
            .setMessage(
                getString(R.string.delete_inventory_message, inventory.id)
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteInventory(inventory.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // ---------- Вспомогательное ----------

    private fun updateEmptyView() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvInventories.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}