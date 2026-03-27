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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.PackagingProductUiItem
import ru.faserkraft.client.adapter.PackagingProductsAdapter
import ru.faserkraft.client.databinding.FragmentNewPackagingBinding
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.viewmodel.ScannerViewModel

class NewPackagingFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentNewPackagingBinding

    private lateinit var adapter: PackagingProductsAdapter

    private val args: NewPackagingFragmentArgs by navArgs()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNewPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // номер упаковки
        viewModel.packagingState.observe(viewLifecycleOwner) { packaging ->
            binding.tvPackagingSerial.text = packaging?.serialNumber
        }

        adapter = PackagingProductsAdapter { item, isChecked ->
            val current = adapter.currentList.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index != -1) {
                current[index] = current[index].copy(isSelected = isChecked)
                adapter.submitList(current)
            }

            val allSelected = adapter.currentList.all { it.isSelected }
            if (binding.cbSelectAll.isChecked != allSelected) {
                binding.cbSelectAll.setOnCheckedChangeListener(null)
                binding.cbSelectAll.isChecked = allSelected
                binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
            }
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        // empty view observer
        val emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()    // первичная проверка

        // обработчик "выбрать все"
        binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)

        // загрузка списка доступных продуктов для упаковки
        viewModel.availableProductsForPackaging.observe(viewLifecycleOwner) { products ->
            val editingPackaging = args.packaging
            val selectedIdsFromPackaging =
                editingPackaging?.products?.map { it.id }?.toSet().orEmpty()

            val base = products.map { p ->
                PackagingProductUiItem(
                    id = p.id,
                    serialNumber = p.serialNumber,
                    processName = p.process.name,
                    sizeType = p.process.type?.id ?: 0,
                    packagingCount = p.process.type?.packagingCount ?: 1,
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
                        sizeType = ep.process.type?.id ?: 0,
                        packagingCount = ep.process.type?.packagingCount ?: 1,
                        isSelected = true
                    )
                }.orEmpty()

            val uiItems = (base + extra).sortedBy { it.serialNumber }
            adapter.submitList(uiItems)

            binding.cbSelectAll.setOnCheckedChangeListener(null)
            val allSelected = uiItems.isNotEmpty() && uiItems.all { it.isSelected }
            binding.cbSelectAll.isChecked = allSelected
            binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
        }

        // начальная загрузка
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadAvailableProductsForPackaging()
        }

        // ошибки
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

        // создание упаковки (без изменений)
        binding.btnSave.setOnClickListener {
            val serial = binding.tvPackagingSerial.text?.toString().orEmpty()

            val selectedItems = adapter.currentList.filter { it.isSelected }
            if (selectedItems.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setIcon(android.R.drawable.ic_delete)
                    .setTitle("Внимание")
                    .setMessage("Вы не выбрали ни одного продукта!\nУдалить упаковку?")
                    .setNegativeButton("ОК") { dialog, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val result = viewModel.deletePackaging(serial)
                            if (result.isSuccess) {
                                view.let { root ->
                                    Snackbar.make(
                                        root,
                                        "Упаковка удалена",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_main, true)
                                    .build()

                                findNavController().navigate(
                                    R.id.scannerFragment,
                                    null,
                                    navOptions
                                )
                            }
                        }
                        dialog.dismiss()
                    }
                    .setPositiveButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                return@setOnClickListener
            }

            val firstTypeSize = selectedItems.first().sizeType

            val hasDifferentTypeSize = selectedItems.any { it.sizeType != firstTypeSize }
            if (hasDifferentTypeSize) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Все выбранные продукты должны быть одного типоразмера!")
                    .setPositiveButton("ОК", null)
                    .show()
                return@setOnClickListener
            }

            val maxAllowed = selectedItems.first().packagingCount
            val selectedCount = selectedItems.size

            if (selectedCount > (maxAllowed ?: 1)) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Количество выбранных продуктов не должно превышать $maxAllowed!")
                    .setPositiveButton("ОК", null)
                    .show()
                return@setOnClickListener
            }

            val newPackaging = PackagingCreateDto(
                serialNumber = serial,
                products = selectedItems.map { it.id }
            )

            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.createPackaging(newPackaging)
                result.onSuccess {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.newPackagingFragment, inclusive = true)
                        .setLaunchSingleTop(true)
                        .build()

                    findNavController().navigate(
                        R.id.action_newPackagingFragment_to_packagingFragment,
                        null,
                        navOptions
                    )
                }.onFailure {
                    // обработка через errorState
                }
            }
        }
    }

    private fun checkEmpty() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmptyProducts.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvProducts.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.cbSelectAll.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private val selectAllListener =
        { _: android.widget.CompoundButton, isChecked: Boolean ->
            val updated = adapter.currentList.map { it.copy(isSelected = isChecked) }
            adapter.submitList(updated)
        }
}
