package ru.faserkraft.client.presentation.packaging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.presentation.ui.hideKeyboard
import ru.faserkraft.client.databinding.FragmentNewPackagingBinding
import ru.faserkraft.client.utils.showErrorSnackbar

class NewPackagingFragment : Fragment() {

    private val viewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentNewPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingProductsAdapter
    private var emptyObserver: RecyclerView.AdapterDataObserver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        observeState()
        observeEvents()

        viewModel.loadAvailableProducts()

        binding.tvPackagingSerial.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard()
                v.clearFocus()
                true
            } else false
        }

        binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
        binding.btnSave.setOnClickListener { onSaveClick() }
    }

    private fun setupAdapter() {
        adapter = PackagingProductsAdapter { item, isChecked ->
            val current = adapter.currentList.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index != -1) {
                current[index] = current[index].copy(isSelected = isChecked)
                adapter.submitList(current)
            }
            syncSelectAllCheckbox(current)
        }
    }

    private fun setupRecyclerView() {
        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver!!)
        emptyObserver?.onChanged()

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val b = _binding ?: return@collect

                    b.tvPackagingSerial.text = state.currentPackaging?.serialNumber

                    val editingPackaging = state.currentPackaging
                    val selectedIdsFromPackaging =
                        editingPackaging?.products?.map { it.id }?.toSet().orEmpty()

                    val base = state.availableProducts.map { p ->
                        PackagingProductUiItem(
                            id = p.id,
                            serialNumber = p.serialNumber,
                            processName = p.process.name,
                            sizeType = p.process.sizeTypeId ?: 0,
                            packagingCount = p.process.packagingCount ?: 1,
                            isSelected = selectedIdsFromPackaging.contains(p.id)
                        )
                    }

                    val extra = editingPackaging?.products
                        ?.filter { ep -> base.none { it.id == ep.id } }
                        ?.map { ep ->
                            PackagingProductUiItem(
                                id = ep.id,
                                serialNumber = ep.serialNumber,
                                processName = ep.process.name,
                                sizeType = ep.process.sizeTypeId ?: 0,
                                packagingCount = ep.process.packagingCount ?: 1,
                                isSelected = true
                            )
                        }.orEmpty()

                    val uiItems = (base + extra).sortedBy { it.serialNumber }
                    adapter.submitList(uiItems)
                    syncSelectAllCheckbox(uiItems)

                    val isLoading = state.isLoading || state.isActionInProgress
                    b.btnSave.isEnabled = !isLoading
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
                        PackagingEvent.NavigateToPackaging -> navigateToPackaging()
                        PackagingEvent.PackagingDeleted -> navigateToScanner()
                        PackagingEvent.NavigateToNewPackaging,
                        PackagingEvent.NavigateToEdit -> Unit
                    }
                }
            }
        }
    }

    private fun onSaveClick() {
        val serial = binding.tvPackagingSerial.text?.toString().orEmpty()
        val selectedItems = adapter.currentList.filter { it.isSelected }
        val isEditing = viewModel.uiState.value.currentPackaging?.products?.isNotEmpty() == true

        if (selectedItems.isEmpty()) {
            if (isEditing) {
                MaterialAlertDialogBuilder(requireContext())
                    .setIcon(android.R.drawable.ic_delete)
                    .setTitle("Внимание")
                    .setMessage("Вы не выбрали ни одного продукта!\nУдалить упаковку?")
                    .setNegativeButton("Удалить") { dialog, _ ->
                        viewModel.deletePackaging(serial)
                        dialog.dismiss()
                    }
                    .setPositiveButton("Отмена", null)
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage("Выберите хотя бы один продукт!")
                    .setPositiveButton("ОК", null)
                    .show()
            }
            return
        }

        val firstItem = selectedItems.first()
        val firstTypeSize = firstItem.sizeType
        val maxAllowed = firstItem.packagingCount ?: 1

        if (selectedItems.any { it.sizeType != firstTypeSize }) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("Все выбранные продукты должны быть одного типоразмера!")
                .setPositiveButton("ОК", null)
                .show()
            return
        }

        if (selectedItems.size > maxAllowed) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("Количество выбранных продуктов не должно превышать $maxAllowed!")
                .setPositiveButton("ОК", null)
                .show()
            return
        }

        viewModel.createPackaging(serial, selectedItems.map { it.id })
    }

    private fun navigateToPackaging() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.newPackagingFragment, inclusive = true)
            .setLaunchSingleTop(true)
            .build()
        findNavController().navigate(
            R.id.action_newPackagingFragment_to_packagingFragment,
            null,
            navOptions
        )
    }

    private fun navigateToScanner() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_main, true)
            .build()
        findNavController().navigate(R.id.scannerFragment, null, navOptions)
    }

    private fun checkEmpty() {
        val isEmpty = adapter.itemCount == 0
        _binding?.tvEmptyProducts?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        _binding?.rvProducts?.visibility = if (isEmpty) View.GONE else View.VISIBLE
        _binding?.cbSelectAll?.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun syncSelectAllCheckbox(items: List<PackagingProductUiItem>) {
        val b = _binding ?: return
        val allSelected = items.isNotEmpty() && items.all { it.isSelected }
        b.cbSelectAll.setOnCheckedChangeListener(null)
        b.cbSelectAll.isChecked = allSelected
        b.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
    }

    private val selectAllListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        val updated = adapter.currentList.map { it.copy(isSelected = isChecked) }
        adapter.submitList(updated)
    }

    override fun onDestroyView() {
        emptyObserver?.let { adapter.unregisterAdapterDataObserver(it) }
        emptyObserver = null
        binding.rvProducts.adapter = null
        _binding = null
        super.onDestroyView()
    }
}